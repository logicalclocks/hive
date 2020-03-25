/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hadoop.hive.metastore;

import io.hops.security.HopsFileBasedKeyStoresFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class HopsTLSTSocketFactory {

    private static final Logger LOG = LoggerFactory.getLogger(HopsTLSTSocketFactory.class);

    public static class HopsTLSTransportParams {
        protected int clientTimeout;
        protected InetSocketAddress ifAddress;
        protected List<String> excludeCiphers;
        protected String[] enabledProtocols;
        protected boolean clientAuth;
    }

    public static TServerSocket getServerSocket(Configuration conf, HopsTLSTransportParams params) throws TTransportException {
        SSLContext ctx = createSSLContext(conf, params);
        return createServer(ctx.getServerSocketFactory(), params);
    }

    private static SSLContext createSSLContext(Configuration conf, HopsTLSTransportParams params) throws TTransportException {
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");

            // Create reloadable keyManagers/trustManagers
            HopsFileBasedKeyStoresFactory keyStoresFactory = new HopsFileBasedKeyStoresFactory();
            keyStoresFactory.setConf(conf);
            keyStoresFactory.setSystemConf(conf);
            //TODO (Fabio) look into here
            keyStoresFactory.init(SSLFactory.Mode.SERVER);
            ctx.init(keyStoresFactory.getKeyManagers(), keyStoresFactory.getTrustManagers(), null);
            ctx.getDefaultSSLParameters().setProtocols(params.enabledProtocols);
        } catch (Exception var17) {
            throw new TTransportException("Error creating the transport", var17);
        }
        return ctx;
    }

    private static TServerSocket createServer(SSLServerSocketFactory factory, HopsTLSTransportParams params) throws TTransportException {
        try {
            SSLServerSocket serverSocket = (SSLServerSocket)factory.createServerSocket(
                    params.ifAddress.getPort(), 100, params.ifAddress.getAddress());
            serverSocket.setSoTimeout(params.clientTimeout);
            serverSocket.setNeedClientAuth(params.clientAuth);
            disableExcludedCiphers(serverSocket, params);
            return new TServerSocket(new TServerSocket.ServerSocketTransportArgs().serverSocket(serverSocket));
        } catch (Exception var7) {
            throw new TTransportException("Could not bind to port " + params.ifAddress.getPort(), var7);
        }
    }

    private static void disableExcludedCiphers(SSLServerSocket sslServerSocket, HopsTLSTransportParams params) {
        String[] cipherSuites = sslServerSocket.getEnabledCipherSuites();

        ArrayList<String> defaultEnabledCipherSuites = new ArrayList<>(Arrays.asList(cipherSuites));
        Iterator iterator = params.excludeCiphers.iterator();

        while(iterator.hasNext()) {
          String cipherName = (String)iterator.next();
          if(defaultEnabledCipherSuites.contains(cipherName)) {
            defaultEnabledCipherSuites.remove(cipherName);
            LOG.debug("Disabling cipher suite {}.", cipherName);
          }
        }

        cipherSuites = defaultEnabledCipherSuites.toArray(
            new String[defaultEnabledCipherSuites.size()]);
        sslServerSocket.setEnabledCipherSuites(cipherSuites);
    }
}
