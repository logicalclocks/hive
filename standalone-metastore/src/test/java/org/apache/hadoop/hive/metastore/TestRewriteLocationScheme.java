/*
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
import org.apache.hadoop.hive.metastore.annotation.MetastoreUnitTest;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

@Category(MetastoreUnitTest.class)
public class TestRewriteLocationScheme {

  private HiveMetaStoreClient client;

  @Before
  public void setUp() throws Exception {
    client = Mockito.mock(HiveMetaStoreClient.class, Mockito.CALLS_REAL_METHODS);
  }

  private void configureRewrite(String sourceScheme, String targetScheme) throws Exception {
    Field patternField = HiveMetaStoreClient.class.getDeclaredField("locationSchemePattern");
    patternField.setAccessible(true);
    patternField.set(client, Pattern.compile("^" + Pattern.quote(sourceScheme) + "://"));

    Field prefixField = HiveMetaStoreClient.class.getDeclaredField("targetRewriteSchemePrefix");
    prefixField.setAccessible(true);
    prefixField.set(client, targetScheme + "://");
  }

  @Test
  public void testEmptyConfiguration() {
    Configuration conf = new Configuration();
    String[] rewriteConfig = client.locationSchemeRewrite(conf);
    Assert.assertNull(rewriteConfig[0]);
    Assert.assertNull(rewriteConfig[1]);
  }

  @Test
  public void testIncompleteConfiguration() {
    Configuration conf = new Configuration();
    conf.set(MetastoreConf.ConfVars.LOCATION_SCHEME_REWRITE.getHiveName(), "hdfs,");
    String[] rewriteConfig = client.locationSchemeRewrite(conf);
    Assert.assertNull(rewriteConfig[0]);
    Assert.assertNull(rewriteConfig[1]);
  }

  @Test
  public void testIncompleteConfigurationWithWhitespace() {
    Configuration conf = new Configuration();
    conf.set(MetastoreConf.ConfVars.LOCATION_SCHEME_REWRITE.getHiveName(), "hdfs, ");
    String[] rewriteConfig = client.locationSchemeRewrite(conf);
    Assert.assertNull(rewriteConfig[0]);
    Assert.assertNull(rewriteConfig[1]);
  }

  @Test
  public void testRewriteMatchingScheme() throws Exception {
    configureRewrite("hdfs", "rahdfs");
    Assert.assertEquals("rahdfs://namenode:8020/data/warehouse",
        client.rewriteLocationScheme("hdfs://namenode:8020/data/warehouse"));
  }

  @Test
  public void testRewriteDisabled() {
    // Fields are null by default on the mock (no configureRewrite call)
    Assert.assertEquals("hdfs://namenode:8020/data/warehouse",
        client.rewriteLocationScheme("hdfs://namenode:8020/data/warehouse"));
  }

  @Test
  public void testRewriteOnlyReplacesPrefix() throws Exception {
    configureRewrite("hdfs", "rahdfs");
    Assert.assertEquals("rahdfs://namenode:8020/data/hdfs/warehouse",
        client.rewriteLocationScheme("hdfs://namenode:8020/data/hdfs/warehouse"));
  }

  @Test
  public void testRewriteDoesNotMatchMidString() throws Exception {
    configureRewrite("hdfs", "rahdfs");
    Assert.assertEquals("s3a://bucket/hdfs://something",
        client.rewriteLocationScheme("s3a://bucket/hdfs://something"));
  }
}
