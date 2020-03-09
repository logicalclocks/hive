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

package org.apache.hive.jdbc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.apache.hive.service.server.HiveServer2;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.security.KeyPair;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * TestHS2TLSServer -- executes tests from HiveServer2 HTTP Server
 */
public class TestHS2TLSServer {

  private Path serverKeyStore, serverTrustStore;
  private Path clientKeyStore, clientTrustStore;
  private String passwd = "123456";
  private int portTLS = 12345;
  private int portHops = 12346;
  private String outputDir = "";

  private static HiveServer2 hiveServer2 = null;
  private static HiveConf hiveConf = null;

  private Connection hs2Conn = null;
  private Statement stmt = null;
  private ResultSet rs = null;

  private List<String> fileToClean = new ArrayList<>();

  @Rule
  public final ExpectedException rule = ExpectedException.none();

  @Before
  public void beforeTests() throws Exception {
    outputDir = KeyStoreTestUtil.getClasspathDir(TestHS2TLSServer.class);

    generateCerts();
    Configuration sslServerConf = KeyStoreTestUtil.createServerSSLConfig(serverKeyStore.toString(),
        passwd, passwd, serverTrustStore.toString(), passwd, "");
    Path sslServerPath = Paths.get(outputDir, "ssl-server.xml");
    fileToClean.add(sslServerConf.toString());
    File sslServer = new File(sslServerPath.toUri());
    KeyStoreTestUtil.saveConfig(sslServer, sslServerConf);


    // Configure SSL
    hiveConf = new HiveConf();
    hiveConf.setVar(HiveConf.ConfVars.HIVE_SUPER_USER, UserGroupInformation.getCurrentUser().getUserName());
    hiveConf.setBoolean(CommonConfigurationKeysPublic.IPC_SERVER_SSL_ENABLED, true);
    hiveConf.addResource("ssl-server.xml");
    hiveConf.set(SSLFactory.SSL_ENABLED_PROTOCOLS_KEY, "TLSv1.2,TLSv1.1,TLSv1");
    hiveConf.set(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY, "ALLOW_ALL");

    hiveConf.setIntVar(HiveConf.ConfVars.HIVE_SERVER2_THRIFT_PORT_2WSSL, portTLS);
    hiveConf.setIntVar(HiveConf.ConfVars.HIVE_SERVER2_THRIFT_PORT, portHops);
    hiveConf.setVar(HiveConf.ConfVars.HIVE_AUTHENTICATOR_MANAGER, "");
    hiveConf.setVar(HiveConf.ConfVars.HIVE_SERVER2_AUTHENTICATION, "CERTIFICATES");

    // Disable concurrency support for this test
    hiveConf.setBoolVar(HiveConf.ConfVars.HIVE_SUPPORT_CONCURRENCY, false);

    // Only check for authentication, not authorization
    hiveConf.setBoolVar(HiveConf.ConfVars.HIVE_SERVER2_ENABLE_DOAS, false);

    hiveServer2 = new HiveServer2();
    hiveServer2.init(hiveConf);
    hiveServer2.start();
    Thread.sleep(5000);
  }

  @After
  public void afterTests() throws Exception {
    if (rs != null) {
      rs.close();
    }

    if (stmt != null) {
      stmt.close();
    }

    if (hs2Conn != null) {
      hs2Conn.close();
    }

    hiveServer2.stop();

    for (String path : fileToClean) {
      File f = new File(path);
      if (f.exists()) {
        f.delete();
      }
    }

    Thread.sleep(5000);
  }

  @Test
  public void testSingleWayTLSPortNotEnabled() throws Exception {
    String sslConnectionSettings = "jdbc:hive2://localhost:" + String.valueOf(portHops) +
        "/default;auth=noSasl;ssl=true;" +
        "sslTrustStore=" + clientTrustStore.toString() + ";" +
        "trustStorePassword=" + passwd;
    rule.expect(SQLException.class);
    hs2Conn = DriverManager.getConnection(sslConnectionSettings);
  }

  @Test
  public void testTLSAuthenticationNoCerts() throws Exception {
    String sslConnectionSettings = "jdbc:hive2://localhost:" + String.valueOf(portTLS) +
        "/default;auth=noSasl;";
    rule.expect(SQLException.class);
    hs2Conn = DriverManager.getConnection(sslConnectionSettings);
  }

  @Test
  public void testTLSAuthenticationValidCerts() throws Exception {
    String sslConnectionSettings = "jdbc:hive2://localhost:" + String.valueOf(portTLS) +
        "/default;auth=noSasl;ssl=true;twoWay=true;" +
        "sslTrustStore=" + clientTrustStore.toString() + ";" +
        "trustStorePassword=" + passwd + ";" +
        "sslKeyStore=" + clientKeyStore.toString() + ";" +
        "keyStorePassword=" + passwd + ";";
    hs2Conn = DriverManager.getConnection(sslConnectionSettings);
    Statement stmt = hs2Conn.createStatement();
    ResultSet rs = stmt.executeQuery("select logged_in_user()");
    rs.next();
    Assert.assertEquals(rs.getString(1), "Ring__Gandalf");
  }

  @Test
  public void testTLSAuthenticationMachineCerts() throws Exception {
    String sslConnectionSettings = "jdbc:hive2://localhost:" + String.valueOf(portTLS) +
        "/default;auth=noSasl;ssl=true;twoWay=true;" +
        "sslTrustStore=" + serverTrustStore.toString() + ";" +
        "trustStorePassword=" + passwd + ";" +
        "sslKeyStore=" + serverKeyStore.toString() + ";" +
        "keyStorePassword=" + passwd + ";";
    hs2Conn = DriverManager.getConnection(sslConnectionSettings);
    Statement stmt = hs2Conn.createStatement();
    ResultSet rs = stmt.executeQuery("select logged_in_user()");
    rs.next();
    Assert.assertEquals(rs.getString(1), UserGroupInformation.getCurrentUser().getUserName());
  }

  private void generateCerts() throws Exception {
    String keyAlg = "RSA";
    String signAlg = "SHA256withRSA";

    // Generate CA
    KeyPair caKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    X509Certificate caCert = KeyStoreTestUtil.generateCertificate("CN=CARoot", caKeyPair, 42, signAlg);

    // Generate server certificate signed by CA
    KeyPair serverKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    X509Certificate serverCrt = KeyStoreTestUtil.generateSignedCertificate("CN=" +
            NetUtils.getHostNameOfIP("127.0.0.1"), serverKeyPair, 42,
            signAlg, caKeyPair.getPrivate(), caCert);

    serverKeyStore = Paths.get(outputDir, "server.keystore.jks");
    fileToClean.add(serverKeyStore.toString());
    serverTrustStore = Paths.get(outputDir, "server.truststore.jks");
    fileToClean.add(serverTrustStore.toString());
    KeyStoreTestUtil.createKeyStore(serverKeyStore.toString(), passwd, passwd,
            "server_alias", serverKeyPair.getPrivate(), serverCrt);
    KeyStoreTestUtil.createTrustStore(serverTrustStore.toString(), passwd, "CARoot", caCert);

    // Generate client certificate with the correct CN field and signed by the CA
    KeyPair c_clientKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    String c_cn = "CN=Ring__Gandalf";
    X509Certificate c_clientCrt = KeyStoreTestUtil.generateSignedCertificate(c_cn, c_clientKeyPair, 42,
            signAlg, caKeyPair.getPrivate(), caCert);

    clientKeyStore = Paths.get(outputDir, "c_client.keystore.jks");
    fileToClean.add(clientKeyStore.toString());
    clientTrustStore = Paths.get(outputDir, "c_client.truststore.jks");
    fileToClean.add(clientTrustStore.toString());
    KeyStoreTestUtil.createKeyStore(clientKeyStore.toString(), passwd, passwd,
            "c_client_alias", c_clientKeyPair.getPrivate(), c_clientCrt);
    KeyStoreTestUtil.createTrustStore(clientTrustStore.toString(), passwd, "CARoot", caCert);
  }
}
