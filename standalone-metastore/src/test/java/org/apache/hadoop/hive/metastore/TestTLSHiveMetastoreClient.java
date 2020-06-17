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

import io.hops.security.CertificateLocalization;
import io.hops.security.CertificateLocalizationCtx;
import io.hops.security.SuperuserKeystoresLoader;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.ProxyUsers;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

  private static X509Certificate caCert;
  private static KeyPair caKeyPair;

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

    // Configure SSL
    hiveConf = MetastoreConf.newMetastoreConf();
    MetaStoreTestUtils.setConfForStandloneMode(hiveConf);
    String username = UserGroupInformation.getCurrentUser().getUserName();
    hiveConf.set(ProxyUsers.CONF_HADOOP_PROXYUSER + "." + username, "*");
    generateCerts();

    hiveConf.set(MetastoreConf.ConfVars.HIVE_SUPER_USER.getVarname(), username);

    hiveConf.setBoolean(CommonConfigurationKeysPublic.IPC_SERVER_SSL_ENABLED, true);
    hiveConf.set(SSLFactory.SSL_ENABLED_PROTOCOLS_KEY, "TLSv1.2,TLSv1.1,TLSv1");
    hiveConf.set(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY, "ALLOW_ALL");

    MetastoreConf.setVar(hiveConf, MetastoreConf.ConfVars.RAW_STORE_IMPL,
        DummyRawStoreForJdoConnection.class.getName());
    MetastoreConf.setVar(hiveConf,
        MetastoreConf.ConfVars.CONNECT_URL_HOOK, DummyJdoConnectionUrlHook.class.getName());
    MetastoreConf.setVar(hiveConf, MetastoreConf.ConfVars.CONNECT_URL_KEY, DummyJdoConnectionUrlHook.initialUrl);
    MetastoreConf.setLongVar(hiveConf, MetastoreConf.ConfVars.CERT_RELOAD_THREAD_SLEEP, 1000);

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

  @AfterClass
  public static void afterClass() {
    if (outputDir != null) {
      FileUtils.deleteQuietly(new File(outputDir));
    }
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

    // Shared between server and tests
    CertificateLocalization certificateLocalization = CertificateLocalizationCtx
        .getInstance().getCertificateLocalization();
    Assert.assertEquals(3,
        certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());

    hmsc.close();
    hmsc = null;
    Thread.sleep(1000);

    Assert.assertEquals(2,
        certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());

    hmscUser.close();
    hmscUser = null;

    Thread.sleep(1000);

    // Check that the certificate has been removed correctly - left one loaded by the tests.
    Assert.assertEquals(1,
        certificateLocalization.getX509MaterialLocation(clientUsername).getRequestedApplications());
  }

  @Test
  public void testAppCertificateRotation() throws Exception {
    UserGroupInformation ugiUser = UserGroupInformation.createProxyUser(clientUsername,
        UserGroupInformation.getCurrentUser());

    FileUtils.copyFile(appClientKeyStore.toFile(), Paths.get("k_certificate").toFile());
    FileUtils.copyFile(appClientTrustStore.toFile(), Paths.get("t_certificate").toFile());
    FileUtils.write(Paths.get("material_passwd").toFile(), password);

    ugiUser.doAs((PrivilegedExceptionAction<Object>) () -> {
      hmsc = new HiveMetaStoreClient(hiveConf);
      return null;
    });


    CertificateLocalization certificateLocalization = CertificateLocalizationCtx
        .getInstance().getCertificateLocalization();
    Assert.assertEquals(1,
        certificateLocalization.getX509MaterialLocation(clientUsername, "app").getRequestedApplications());
    int keystore1_size = certificateLocalization.getX509MaterialLocation(clientUsername, "app").getKeyStoreMem().capacity();

    generateCertificate("CN=" + clientUsername + ",O=app,OU=100000",
        "c_client_alias", appClientKeyStore, appClientTrustStore);

    FileUtils.deleteQuietly(Paths.get("k_certificate").toFile());
    FileUtils.deleteQuietly(Paths.get("t_certificate").toFile());
    FileUtils.copyFile(appClientKeyStore.toFile(), Paths.get("k_certificate").toFile());
    FileUtils.copyFile(appClientTrustStore.toFile(), Paths.get("t_certificate").toFile());

    Thread.sleep(10000);

    Assert.assertEquals(1,
        certificateLocalization.getX509MaterialLocation(clientUsername, "app").getRequestedApplications());
    int keystore2_size = certificateLocalization.getX509MaterialLocation(clientUsername, "app").getKeyStoreMem().capacity();

    Assert.assertNotEquals(keystore1_size, keystore2_size);

    hmsc.close();
    hmsc = null;

    FileUtils.deleteQuietly(Paths.get("k_certificate").toFile());
    FileUtils.deleteQuietly(Paths.get("t_certificate").toFile());
    FileUtils.deleteQuietly(Paths.get("material_passwd").toFile());

    cleanCertificateLocalization(clientUsername, "app");
  }

  @Test
  public void testSuperuserCertificates() throws Exception {
    hmsc = new HiveMetaStoreClient(hiveConf);
  }

  private static void generateCerts() throws Exception {
    String keyAlg = "RSA";
    String signAlg = "SHA256withRSA";

    // Generate CA
    caKeyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    caCert = KeyStoreTestUtil.generateCertificate("CN=CARoot", caKeyPair, 42, signAlg, true);

    String username = UserGroupInformation.getCurrentUser().getUserName();
    SuperuserKeystoresLoader loader = new SuperuserKeystoresLoader(hiveConf);
    serverKeyStore = Paths.get(outputDir, loader.getSuperKeystoreFilename(username));
    serverTrustStore = Paths.get(outputDir, loader.getSuperTruststoreFilename(username));
    generateCertificate("CN=" + NetUtils.getHostNameOfIP("127.0.0.1") + ",L=" + username, "server_alias",
            serverKeyStore, serverTrustStore);

    Path passwdFile = Paths.get(outputDir, loader.getSuperMaterialPasswdFilename(username));
    FileUtils.writeStringToFile(passwdFile.toFile(), password, Charset.defaultCharset());
    hiveConf.set(CommonConfigurationKeys.HOPS_TLS_SUPER_MATERIAL_DIRECTORY, outputDir);

    // Generate client certificate with the correct CN field and signed by the CA
    clientKeyStore = Paths.get(outputDir, "c_client.keystore.jks");
    clientTrustStore = Paths.get(outputDir, "c_client.truststore.jks");
    generateCertificate("CN=" + clientUsername, "c_client_alias", clientKeyStore, clientTrustStore);

    appClientKeyStore = Paths.get(outputDir, "c_app_client.keystore.jks");
    appClientTrustStore = Paths.get(outputDir, "c_app_client.truststore.jks");
    generateCertificate("CN=" + clientUsername + ",O=app,OU=0", "c_client_alias", appClientKeyStore, appClientTrustStore);
  }

  private static void generateCertificate(String cn, String alias, Path keystore, Path truststore) throws Exception {
    String keyAlg = "RSA";
    String signAlg = "SHA256withRSA";

    // Generate client certificate with the correct CN field and signed by the CA
    KeyPair keyPair = KeyStoreTestUtil.generateKeyPair(keyAlg);
    X509Certificate clientCrt = KeyStoreTestUtil.generateSignedCertificate(cn, keyPair, 42,
            signAlg, caKeyPair.getPrivate(), caCert);

    KeyStoreTestUtil.createKeyStore(keystore.toString(), password, password,
            alias, keyPair.getPrivate(), clientCrt);
    KeyStoreTestUtil.createTrustStore(truststore.toString(), password, "CARoot", caCert);
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

