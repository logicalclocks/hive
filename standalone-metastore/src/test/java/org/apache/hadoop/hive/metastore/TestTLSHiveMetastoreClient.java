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
package org.apache.hadoop.hive.metastore;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.hadoop.hive.metastore.utils.MetaStoreUtils;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.ssl.CertificateLocalization;
import org.apache.hadoop.security.ssl.CertificateLocalizationCtx;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;

public class TestTLSHiveMetastoreClient {

  private static Path serverKeyStore, serverTrustStore;
  private static Path clientKeyStore, clientTrustStore;
  private static Path appClientKeyStore, appClientTrustStore;
  private static String outputDir;

  private static final String clientUsername = "Ring__Gandalf";
  private static final String fakeUser = "fake__user";
  private static final String password = "123456";

  private static Configuration hiveConf;

  private HiveMetaStoreClient hmsc = null;
  private HiveMetaStoreClient hmscUser = null;

  @Rule
  public final ExpectedException rule = ExpectedException.none();

  @BeforeClass
  public static void setUp() throws Exception {
    outputDir = KeyStoreTestUtil.getClasspathDir(TestTLSHiveMetastoreClient.class);

    generateCerts();
    Configuration sslServerConf = KeyStoreTestUtil.createServerSSLConfig(serverKeyStore.toString(),
        password, password, serverTrustStore.toString(), password, "");
    Path sslServerPath = Paths.get(outputDir, "ssl-server.xml");
    File sslServer = new File(sslServerPath.toUri());
    KeyStoreTestUtil.saveConfig(sslServer, sslServerConf);

    // Configure SSL
    hiveConf = MetastoreConf.newMetastoreConf();
    MetaStoreTestUtils.setConfForStandloneMode(hiveConf);

    hiveConf.set(MetastoreConf.ConfVars.HIVE_SUPER_USER.getVarname(),
        UserGroupInformation.getCurrentUser().getUserName());

    hiveConf.setBoolean(CommonConfigurationKeysPublic.IPC_SERVER_SSL_ENABLED, true);
    hiveConf.addResource("ssl-server.xml");
    hiveConf.set(SSLFactory.SSL_ENABLED_PROTOCOLS, "TLSv1.2,TLSv1.1,TLSv1");
    hiveConf.set(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY, "ALLOW_ALL");

    MetastoreConf.setVar(hiveConf, MetastoreConf.ConfVars.RAW_STORE_IMPL,
        DummyRawStoreForJdoConnection.class.getName());
    MetastoreConf.setVar(hiveConf,
        MetastoreConf.ConfVars.CONNECT_URL_HOOK, DummyJdoConnectionUrlHook.class.getName());
    MetastoreConf.setVar(hiveConf, MetastoreConf.ConfVars.CONNECT_URL_KEY, DummyJdoConnectionUrlHook.initialUrl);

    // Start Hivemetastore
    hiveConf.setClass(MetastoreConf.ConfVars.EXPRESSION_PROXY_CLASS.getVarname(),
        MockPartitionExpressionForMetastore.class, PartitionExpressionProxy.class);
    hiveConf.setBoolean(MetastoreConf.ConfVars.EXECUTE_SET_UGI.getVarname(), true);

    int msPort = MetaStoreTestUtils.startMetaStoreWithRetry(hiveConf);
    hiveConf.set(MetastoreConf.ConfVars.THRIFT_URIS.getVarname(), "thrift://localhost:" + msPort);
    hiveConf.set(MetastoreConf.ConfVars.INIT_HOOKS.getVarname(), "");
    hiveConf.setBoolean(MetastoreConf.ConfVars.HIVE_SUPPORT_CONCURRENCY.getVarname(), false);
    hiveConf.setInt(MetastoreConf.ConfVars.THRIFT_CONNECTION_RETRIES.getVarname(), 1);
  }


  @After
  public void closeClient() throws Exception {
    if (hmsc != null) {
      hmsc.close();
    }

    cleanCertificateLocalization(clientUsername, null);
    cleanCertificateLocalization(clientUsername, "app");
    cleanCertificateLocalization(fakeUser, "app");
  }


  @Test
  public void testClientMatchingCNUGI() throws Exception {
    setUpCertificateLocalization(clientUsername, clientKeyStore, clientTrustStore);

    UserGroupInformation ugi =
        UserGroupInformation.createProxyUser(clientUsername, UserGroupInformation.getCurrentUser());

    ugi.doAs((PrivilegedExceptionAction<Object>) () -> {
      hmsc = new HiveMetaStoreClient(hiveConf);
      return null;
    });

    CertificateLocalization certificateLocalization =
        CertificateLocalizationCtx.getInstance().getCertificateLocalization();
    // CertLocService shared with tests
    Assert.assertEquals(2, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());

    hmsc.close();
    hmsc = null;
    Thread.sleep(1000);

    Assert.assertEquals(1, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());
  }

  @Test
  public void testClientNotMatchingCNUGI() throws Exception {
    UserGroupInformation ugi =
        UserGroupInformation.createProxyUser(fakeUser, UserGroupInformation.getCurrentUser());

    setUpCertificateLocalization(fakeUser, clientKeyStore, clientTrustStore);
    ugi.doAs((PrivilegedExceptionAction<Object>) () -> {
      rule.expect(UndeclaredThrowableException.class);
      hmsc = new HiveMetaStoreClient(hiveConf);
      return null;
    });

    cleanCertificateLocalization(fakeUser, null);
  }

  @Test
  public void testAppClient() throws Exception {
    UserGroupInformation ugiApp =
        UserGroupInformation.createProxyUser(clientUsername, UserGroupInformation.getCurrentUser());
    ugiApp.addApplicationId("app");

    setUpCertificateLocalization(clientUsername, appClientKeyStore, appClientTrustStore);
    ugiApp.doAs((PrivilegedExceptionAction<Object>) () -> {
      hmsc = new HiveMetaStoreClient(hiveConf);
      return null;
    });

    CertificateLocalization certificateLocalization =
        CertificateLocalizationCtx.getInstance().getCertificateLocalization();

    // CertLocService shared with the tests.
    Assert.assertEquals(1, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());

    // Expect one certificate saved under the `clientUsername,app`
    Assert.assertEquals(1, certificateLocalization.getX509MaterialLocation(clientUsername,
        "app").getRequestedApplications());

    hmsc.close();
    hmsc = null;
    Thread.sleep(1000);

    rule.expect(FileNotFoundException.class);
    certificateLocalization.getX509MaterialLocation(clientUsername, "app");
    // Check that the counter is decremented correctly
    Assert.assertEquals(1, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());
    cleanCertificateLocalization(clientUsername, null);
  }

  @Test
  public void testConcurrentAppNonAppUser() throws Exception {
    UserGroupInformation ugiApp =
        UserGroupInformation.createProxyUser(clientUsername, UserGroupInformation.getCurrentUser());
    ugiApp.addApplicationId("app");
    setUpCertificateLocalization(clientUsername, appClientKeyStore, appClientTrustStore);

    ugiApp.doAs((PrivilegedExceptionAction<Object>) () -> {
      hmsc = new HiveMetaStoreClient(hiveConf);
      return null;
    });

    UserGroupInformation ugiUser =
        UserGroupInformation.createProxyUser(clientUsername, UserGroupInformation.getCurrentUser());

    byte[] keyStore = Files.readAllBytes(clientKeyStore);
    byte[] trustStore = Files.readAllBytes(clientTrustStore);

    CertificateLocalization certificateLocalization = CertificateLocalizationCtx
        .getInstance().getCertificateLocalization();
    certificateLocalization.updateX509(clientUsername, null, ByteBuffer.wrap(keyStore),
        password, ByteBuffer.wrap(trustStore), password);

    ugiUser.doAs((PrivilegedExceptionAction<Object>) () -> {
      hmscUser = new HiveMetaStoreClient(hiveConf);
      return null;
    });

    // CertLocService shared with the tests
    Assert.assertEquals(2, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());

    // Expect one certificate saved under the `clientUsername,app`
    Assert.assertEquals(1, certificateLocalization.getX509MaterialLocation(clientUsername,
        "app").getRequestedApplications());

    hmsc.close();
    hmscUser.close();
    hmsc = null;
    Thread.sleep(1000);

    // Check that the certificate has been removed correctly
    rule.expect(FileNotFoundException.class);
    certificateLocalization.getX509MaterialLocation(clientUsername, "app");
    // Check that the certificate has been removed correctly
    Assert.assertEquals(1, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());
    cleanCertificateLocalization(clientUsername, null);
  }

  @Test
  public void testConcurrentUsers() throws Exception {
    UserGroupInformation ugiUser =
        UserGroupInformation.createProxyUser(clientUsername, UserGroupInformation.getCurrentUser());
    setUpCertificateLocalization(clientUsername, clientKeyStore, clientTrustStore);

    ugiUser.doAs((PrivilegedExceptionAction<Object>) () -> {
      hmsc = new HiveMetaStoreClient(hiveConf);
      return null;
    });

    ugiUser.doAs((PrivilegedExceptionAction<Object>) () -> {
      hmscUser = new HiveMetaStoreClient(hiveConf);
      return null;
    });

    // Expect only one here as it should have been saved
    CertificateLocalization certificateLocalization = CertificateLocalizationCtx
        .getInstance().getCertificateLocalization();
    Assert.assertEquals(3, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());

    hmsc.close();
    hmsc = null;
    Thread.sleep(1000);

    Assert.assertEquals(2, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());

    hmscUser.close();
    hmscUser = null;

    Thread.sleep(1000);

    // Check that the certificate has been removed correctly - left one loaded by the tests.
    Assert.assertEquals(1, certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());
  }

  @Test
  public void testAppCertificateRotation() throws Exception {
    // TODO(Fabio)
  }

  @Test
  public void testClientMachineCertificates() throws Exception {
    hmsc = new HiveMetaStoreClient(hiveConf);
  }

  private static void generateCerts() throws Exception {
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
    serverTrustStore = Paths.get(outputDir, "server.truststore.jks");
    KeyStoreTestUtil.createKeyStore(serverKeyStore.toString(), password, password,
            "server_alias", serverKeyPair.getPrivate(), serverCrt);
    KeyStoreTestUtil.createTrustStore(serverTrustStore.toString(), password, "CARoot", caCert);

    // Generate client certificate with the correct CN field and signed by the CA
    KeyPair c_clientKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    String c_cn = "CN=" + clientUsername;
    X509Certificate c_clientCrt = KeyStoreTestUtil.generateSignedCertificate(c_cn, c_clientKeyPair, 42,
            signAlg, caKeyPair.getPrivate(), caCert);

    clientKeyStore = Paths.get(outputDir, "c_client.keystore.jks");
    clientTrustStore = Paths.get(outputDir, "c_client.truststore.jks");
    KeyStoreTestUtil.createKeyStore(clientKeyStore.toString(), password, password,
            "c_client_alias", c_clientKeyPair.getPrivate(), c_clientCrt);
    KeyStoreTestUtil.createTrustStore(clientTrustStore.toString(), password, "CARoot", caCert);

    // Generate client certificate with the correct CN field and signed by the CA
    KeyPair c_appClientKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    String c_appCn = "CN=" + clientUsername + ",O=app";
    X509Certificate c_appClientCrt = KeyStoreTestUtil.generateSignedCertificate(c_appCn, c_appClientKeyPair, 42,
            signAlg, caKeyPair.getPrivate(), caCert);

    appClientKeyStore = Paths.get(outputDir, "c_app_client.keystore.jks");
    appClientTrustStore = Paths.get(outputDir, "c_app_client.truststore.jks");
    KeyStoreTestUtil.createKeyStore(appClientKeyStore.toString(), password, password,
            "c_client_alias", c_appClientKeyPair.getPrivate(), c_appClientCrt);
    KeyStoreTestUtil.createTrustStore(appClientTrustStore.toString(), password, "CARoot", caCert);
  }

  private void setUpCertificateLocalization(String username, Path keyStorePath, Path trustStorePath) throws Exception {
    /*
     * Test run in a single JVM so the CertificateLocalizationContext is shared between the Metastore and the client
     * Shouldn't cause any issue, except for the testSendCrypto in which we should see that the counter for the number of
     * instances of the client certificates is 2 (1 in the client and 1 in the metastore)
     */
    byte[] keyStore = Files.readAllBytes(keyStorePath);
    byte[] trustStore = Files.readAllBytes(trustStorePath);

    CertificateLocalization certificateLocalization = CertificateLocalizationCtx
        .getInstance().getCertificateLocalization();
    certificateLocalization.materializeCertificates(username, username, ByteBuffer.wrap(keyStore),
        password, ByteBuffer.wrap(trustStore), password);
  }

  private void cleanCertificateLocalization(String username, String appId) {
    CertificateLocalization certificateLocalization = CertificateLocalizationCtx
        .getInstance().getCertificateLocalization();
    try {
      certificateLocalization.removeX509Material(username, appId);
    } catch (Exception e) {}
  }
}

