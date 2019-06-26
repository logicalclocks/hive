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
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;

import io.hops.common.Pair;
import io.hops.security.HopsUtil;
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
import javax.security.cert.X509Certificate;

@SuppressWarnings("rawtypes")
public class TSSLBasedProcessor<I extends Iface> extends TUGIBasedProcessor<Iface> {

  private final I iface;
  private final Map<String,  ProcessFunction<Iface, ? extends  TBase>>
    functions;
  static final Logger LOG = LoggerFactory.getLogger(TSSLBasedProcessor.class);

  private Configuration metastoreConf = null;


  public TSSLBasedProcessor(I iface, Configuration metastoreConf) throws SecurityException, NoSuchFieldException,
    IllegalArgumentException, IllegalAccessException, NoSuchMethodException,
    InvocationTargetException {
    super(iface);
    this.iface = iface;
    this.functions = getProcessMapView();
    this.metastoreConf = metastoreConf;
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

  private Pair<String, String> extractDN(TProtocol in) throws TException, SSLException {
    // Get the the certificate chain out of the TProtocol object
    TTransport tTransport = in.getTransport();
    Socket socket = ((TUGIContainingTransport) tTransport).getSocket();
    X509Certificate[] certs = ((SSLSocket) socket).getSession().getPeerCertificateChain();

    // Make sure it's 2 way ssl, i.e. client certificate is available
    if (certs.length == 0) {
      LOG.error("Client certificate not available");
      throw new SSLException("Client certificate not available");
    }

    // Client certificate is always the first
    String DN = certs[0].getSubjectDN().getName();
    String CN = HopsUtil.extractCNFromSubject(DN);
    String applicationId = HopsUtil.extractOFromSubject(DN);

    if (CN.contains("__")) {
      // The certificate is in the format projectName__userName
      return new Pair<>(CN, applicationId);
    } else {
      // The certificate might be a machine certificate if the operation is requested
      // by the superuser
      InetAddress hostnameIp = null;
      try {
        // Hostname resolution and check against the ip of the machine doing the request
        hostnameIp = InetAddress.getByName(CN);
      } catch (java.net.UnknownHostException ex) {
        LOG.error("Cannot resolve machine address: ", ex);
        throw new TException("Cannot authenticate the user");
      }

      if (!hostnameIp.getHostAddress().equals(HiveMetaStore.HMSHandler.getThreadLocalIpAddress())) {
        // The requests doesn't come from the same machine of the certificate.
        // Something shady is happening here. Do not authenticate the user.
        LOG.error("Superuser request coming from a different host");
        throw new TException("Cannot authenticate the user");
      }

      return new Pair<>(MetastoreConf.getVar(metastoreConf, MetastoreConf.ConfVars.HIVE_SUPER_USER), null);
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
      Pair<String, String> certDN = extractDN(iprot);
      if (!user.equals(certDN.getL())) {
        // Mismatch between UGI user and common name in the certificate
        LOG.error("Mismatch between the UGI user: ", user,
            " and common name in the certificate: ", certDN.getL());
        throw new TTransportException("Client not authorized.");
      }

      clientUgi = UserGroupInformation.createRemoteUser(user);
      if (certDN.getR() != null) {
        clientUgi.addApplicationId(certDN.getR());
      }

      ugiTrans.setClientUGI(clientUgi);
      oprot.writeMessageBegin(new TMessage(msg.name, TMessageType.REPLY, msg.seqid));
      result.write(oprot);
      oprot.writeMessageEnd();
      oprot.getTransport().flush();
  }

}
