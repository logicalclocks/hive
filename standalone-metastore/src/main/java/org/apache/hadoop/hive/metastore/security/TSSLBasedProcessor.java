/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.metastore.security;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import io.hops.common.Pair;
import io.hops.security.HopsUtil;
import io.hops.security.HopsX509AuthenticationException;
import io.hops.security.HopsX509Authenticator;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore.Iface;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore.set_ugi_args;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore.set_ugi_result;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.HiveMetaStore;
import org.apache.hadoop.hive.metastore.TUGIBasedProcessor;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.hadoop.security.SaslRpcServer;
import org.apache.thrift.ProcessFunction;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

@SuppressWarnings("rawtypes")
public class TSSLBasedProcessor<I extends Iface> extends TUGIBasedProcessor<Iface> {

  private final I iface;
  private final Map<String,  ProcessFunction<Iface, ? extends  TBase>>
    functions;
  private final HopsX509Authenticator hopsX509Authenticator;
  static final Logger LOG = LoggerFactory.getLogger(TSSLBasedProcessor.class);

  private Configuration metastoreConf = null;


  public TSSLBasedProcessor(I iface, Configuration metastoreConf) throws SecurityException, NoSuchFieldException,
    IllegalArgumentException, IllegalAccessException, NoSuchMethodException,
    InvocationTargetException {
    super(iface);
    this.iface = iface;
    this.functions = getProcessMapView();
    this.metastoreConf = metastoreConf;
    hopsX509Authenticator = new HopsX509Authenticator(metastoreConf);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean process(final TProtocol in, final TProtocol out) throws TException {
    setIpAddress(in);

    final TMessage msg = in.readMessageBegin();
    final ProcessFunction<Iface, ? extends  TBase> fn = functions.get(msg.name);
    if (fn == null) {
      TProtocolUtil.skip(in, TType.STRUCT);
      in.readMessageEnd();
      TApplicationException x = new TApplicationException(TApplicationException.UNKNOWN_METHOD,
          "Invalid method name: '"+msg.name+"'");
      out.writeMessageBegin(new TMessage(msg.name, TMessageType.EXCEPTION, msg.seqid));
      x.write(out);
      out.writeMessageEnd();
      out.getTransport().flush();
      return true;
    }

    TUGIContainingTransport ugiTrans = (TUGIContainingTransport)in.getTransport();
    // Store ugi in transport if the rpc is set_ugi
    if (msg.name.equalsIgnoreCase("set_ugi")){
      try {
        handleSetUGISSL(ugiTrans, (set_ugi<Iface>)fn, msg, in, out);
      } catch (TException e) {
        throw e;
      } catch (Exception e) {
        throw new TException(e.getCause());
      }
      return true;
    }

    UserGroupInformation clientUgi = ugiTrans.getClientUGI();
    if (null == clientUgi){
      throw new TException("UGI missing from the request");
    } else {
      try {
        // Found ugi, perform doAs().
        PrivilegedExceptionAction<Void> pvea = new PrivilegedExceptionAction<Void>() {
          @Override
          public Void run() {
            try {
              fn.process(msg.seqid, in, out, iface);
              return null;
            } catch (TException te) {
              throw new RuntimeException(te);
            }
          }
        };
        clientUgi.doAs(pvea);
        return true;
      } catch (RuntimeException rte) {
        if (rte.getCause() instanceof TException) {
          throw (TException)rte.getCause();
        }
        throw rte;
      } catch (InterruptedException ie) {
        throw new RuntimeException(ie); // unexpected!
      } catch (IOException ioe) {
        throw new RuntimeException(ioe); // unexpected!
      } finally {
          try {
            FileSystem.closeAllForUGI(clientUgi);
          } catch (IOException e) {
            LOG.error("Could not clean up file-system handles for UGI: " + clientUgi, e);
          }
      }
    }
  }

   protected void handleSetUGISSL(TUGIContainingTransport ugiTrans,
                                  set_ugi<Iface> fn, TMessage msg, TProtocol iprot, TProtocol oprot)
      throws TException, SecurityException, SSLException, IllegalArgumentException {

      UserGroupInformation clientUgi = ugiTrans.getClientUGI();
      if( null != clientUgi){
        throw new TException(new IllegalStateException("UGI is already set. Resetting is not " +
        "allowed. Current ugi is: " + clientUgi.getUserName()));
      }

      set_ugi_args args = fn.getEmptyArgsInstance();
      try {
        args.read(iprot);
      } catch (TProtocolException e) {
        iprot.readMessageEnd();
        TApplicationException x = new TApplicationException(TApplicationException.PROTOCOL_ERROR,
            e.getMessage());
        oprot.writeMessageBegin(new TMessage(msg.name, TMessageType.EXCEPTION, msg.seqid));
        x.write(oprot);
        oprot.writeMessageEnd();
        oprot.getTransport().flush();
        return;
      }
      iprot.readMessageEnd();
      set_ugi_result result = fn.getResult(iface, args);
      List<String> principals = result.getSuccess();
      // Store the ugi in transport and then continue as usual.
      String user = principals.remove(principals.size()-1);

      // Get the the certificate chain out of the TProtocol object
      TTransport tTransport = iprot.getTransport();
      Socket socket = ((TUGIContainingTransport) tTransport).getSocket();
      X509Certificate[] certs = (X509Certificate[]) ((SSLSocket) socket).getSession().getPeerCertificates();

      // Make sure it's 2 way ssl, i.e. client certificate is available
      if (certs.length == 0) {
        LOG.error("Client certificate not available");
        throw new SSLException("Client certificate not available");
      }
      clientUgi = UserGroupInformation.createRemoteUser(user);

      try {
        InetAddress address = InetAddress.getByName(HiveMetaStore.HMSHandler.getThreadLocalIpAddress());
        hopsX509Authenticator.authenticateConnection(clientUgi, certs[0], address, "hive-metastore");
      } catch (UnknownHostException ex) {
        LOG.error("Cannot validate client IP address: " + HiveMetaStore.HMSHandler.getThreadLocalIpAddress());
        throw new TTransportException("Cannot validate client IP address", ex);
      } catch (HopsX509AuthenticationException ex) {
        throw new TTransportException("Client not authorized", ex);
      }

      ugiTrans.setClientUGI(clientUgi);
      oprot.writeMessageBegin(new TMessage(msg.name, TMessageType.REPLY, msg.seqid));
      result.write(oprot);
      oprot.writeMessageEnd();
      oprot.getTransport().flush();
  }

}
