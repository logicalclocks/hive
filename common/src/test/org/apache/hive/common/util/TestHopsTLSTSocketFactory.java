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

package org.apache.hive.common.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.hive.common.auth.HiveAuthUtils;
import org.apache.hadoop.hive.common.auth.TServerSocketFactory;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.net.ssl.SSLSocket;
import javax.security.cert.X509Certificate;
import java.io.File;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;

import static org.apache.hadoop.security.ssl.FileBasedKeyStoresFactory.SSL_KEYSTORE_RELOAD_INTERVAL_TPL_KEY;
import static org.apache.hadoop.security.ssl.FileBasedKeyStoresFactory.SSL_TRUSTSTORE_RELOAD_INTERVAL_TPL_KEY;
import static org.apache.hadoop.security.ssl.FileBasedKeyStoresFactory.resolvePropertyName;

public class TestHopsTLSTSocketFactory {
  private Thread serverThread;
  private TThreadPoolServer server;

  private Path serverKeyStore, serverTrustStore;
  private Path clientKeyStore, clientTrustStore;
  private String outputDir;
  private String password = "11111";
  private KeyPair caKeyPair = null;
  private java.security.cert.X509Certificate caCert;
  private String keyAlg = "RSA";
  private String signAlg = "SHA256withRSA";

  @Rule
  public final ExpectedException rule = ExpectedException.none();

  private static final class TestProcessorFactory extends TProcessorFactory {

    public TestProcessorFactory() {
      super(null);
    }

    @Override
    public TProcessor getProcessor(TTransport trans) {
      return (tProtocol, tProtocol1) -> {
        tProtocol.readMessageBegin();
        return true;
      };
    }
  }

  @Before
  public void startServer() throws Exception {
    outputDir = KeyStoreTestUtil.getClasspathDir(TestHopsTLSTSocketFactory.class);

    // Generate CA
    caKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    caCert = KeyStoreTestUtil.generateCertificate("CN=CARoot", caKeyPair, 42, signAlg);

    generateServerCerts("first");
    generateClientCerts();

    // Configure TLS
    Configuration sslServerConf = KeyStoreTestUtil.createServerSSLConfig(serverKeyStore.toString(),
        password, password, serverTrustStore.toString(), password, "");
    Path sslServerPath = Paths.get(outputDir, "ssl-server.xml");
    File sslServer = new File(sslServerPath.toUri());
    KeyStoreTestUtil.saveConfig(sslServer, sslServerConf);

    HiveConf hiveConf = new HiveConf();
    hiveConf.setBoolean(CommonConfigurationKeysPublic.IPC_SERVER_SSL_ENABLED, true);
    hiveConf.addResource("ssl-server.xml");
    hiveConf.setLong(resolvePropertyName(SSLFactory.Mode.SERVER, SSL_KEYSTORE_RELOAD_INTERVAL_TPL_KEY), 1000);
    hiveConf.setLong(resolvePropertyName(SSLFactory.Mode.SERVER, SSL_TRUSTSTORE_RELOAD_INTERVAL_TPL_KEY), 1000);
    hiveConf.set(SSLFactory.SSL_ENABLED_PROTOCOLS_KEY, "TLSv1.2,TLSv1.1,TLSv1");
    hiveConf.set(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY, "ALLOW_ALL");

    TServerSocket serverSocket = TServerSocketFactory.
        getServerSocket(hiveConf, TServerSocketFactory.TSocketType.TWOWAYTLS,null, 3245);

    serverThread = new Thread(() -> {
      TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverSocket)
          .processorFactory(new TestProcessorFactory())
          .transportFactory(new TTransportFactory())
          .protocolFactory(new TBinaryProtocol.Factory())
          .inputProtocolFactory(new TBinaryProtocol.Factory())
          .minWorkerThreads(1)
          .maxWorkerThreads(2);

      server = new TThreadPoolServer(args);
      server.serve();
    });

    serverThread.start();
    Thread.sleep(1000);
  }

  @After
  public void stopServer() throws Exception {
    server.stop();
    serverThread.join();
  }

  @Test
  public void testFailConnectionWithoutClientAuth() throws Exception {
    TTransport clientTransport = HiveAuthUtils.getTLSClientSocket("localhost", 3245, 0,
        clientTrustStore.toString(), password);
    clientTransport.write("Some random bytes".getBytes());

    rule.expect(TTransportException.class);
    clientTransport.flush();

    clientTransport.close();
  }

  @Test
  public void testCertificateReloading() throws Exception {
    TTransport clientTransport = HiveAuthUtils.get2WayTLSClientSocket("localhost", 3245, 0,
        clientTrustStore.toString(), password, clientKeyStore.toString(), password);
    validateCN(clientTransport, "first");
    clientTransport.close();

    generateServerCerts("second");
    // Wait for the new certificates to be reloaded
    Thread.sleep(5000);

    // Check the new common name
    clientTransport = HiveAuthUtils.get2WayTLSClientSocket("localhost", 3245, 0,
        clientTrustStore.toString(), password, clientKeyStore.toString(), password);
    validateCN(clientTransport, "second");
    clientTransport.close();
  }

  private void validateCN(TTransport transport, String expectedCN) throws Exception {
    Socket socket = ((TSocket) transport).getSocket();
    X509Certificate[] certs = ((SSLSocket) socket).getSession().getPeerCertificateChain();

    // Make sure it's 2 way ssl, i.e. client certificate is available
    if (certs.length == 0) {
      throw new TException("Missing certificates");
    }

    // Client certificate is always the first
    String DN = certs[0].getSubjectDN().getName();
    String[] dnTokens = DN.split(",");
    String[] cnTokens = dnTokens[0].split("=", 2);
    if (cnTokens.length != 2) {
      throw new TException("Cannot authenticate the user: Unrecognized CN format");
    }

    Assert.assertEquals(cnTokens[1], expectedCN);
  }

  private void generateServerCerts(String cn) throws Exception {
    // Generate server certificate signed by CA
    KeyPair serverKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    java.security.cert.X509Certificate serverCrt = KeyStoreTestUtil.generateSignedCertificate("CN=" + cn,
        serverKeyPair, 42, signAlg, caKeyPair.getPrivate(), caCert);

    serverKeyStore = Paths.get(outputDir, "server.keystore.jks");
    serverTrustStore = Paths.get(outputDir, "server.truststore.jks");
    KeyStoreTestUtil.createKeyStore(serverKeyStore.toString(), password, password,
            "server_alias", serverKeyPair.getPrivate(), serverCrt);
    KeyStoreTestUtil.createTrustStore(serverTrustStore.toString(), password, "CARoot", caCert);
  }


  private void generateClientCerts() throws Exception {
    // Generate server certificate signed by CA
    KeyPair clientKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    java.security.cert.X509Certificate clientCrt = KeyStoreTestUtil.generateSignedCertificate("CN=client" ,
        clientKeyPair, 42, signAlg, caKeyPair.getPrivate(), caCert);

    clientKeyStore = Paths.get(outputDir, "client.keystore.jks");
    clientTrustStore= Paths.get(outputDir, "client.truststore.jks");
    KeyStoreTestUtil.createKeyStore(clientKeyStore.toString(), password, password,
            "client_alias", clientKeyPair.getPrivate(), clientCrt);
    KeyStoreTestUtil.createTrustStore(clientTrustStore.toString(), password, "CARoot", caCert);
  }
}
