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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf.ConfVars;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * The certificate reloader has to die with the client that owns it: it is a non-static inner
 * class, so a thread that outlives close() pins its HiveMetaStoreClient and that client's
 * HiveConf for the lifetime of the JVM.
 *
 * <p>Two independent defects used to break this, and either one alone is enough to leak, so
 * each is asserted separately: the run loop swallowed the InterruptedException that close()
 * raises, and getHopsSecurityMaterial() is reached twice per connect, which used to overwrite
 * the single thread field and leave the first thread unreachable.
 */
public class TestClientCertUpdaterLifecycle {

  private static final long AWAIT_MILLIS = 10_000;

  /**
   * HiveMetaStoreClient's only constructor connects to a metastore. mock() builds the instance
   * without running it; CALLS_REAL_METHODS keeps close() real, so the test drives the same
   * interrupt() the production shutdown path does. client and transport stay null, which
   * close() handles.
   */
  private HiveMetaStoreClient newUnconnectedClient() throws Exception {
    HiveMetaStoreClient client = Mockito.mock(HiveMetaStoreClient.class,
        Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
    Configuration conf = MetastoreConf.newMetastoreConf();
    // Long enough that the reloader is always parked in sleep() when we interrupt it.
    MetastoreConf.setLongVar(conf, ConfVars.CERT_RELOAD_THREAD_SLEEP, 3_600_000L);
    setField(client, "conf", conf);
    return client;
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field f = HiveMetaStoreClient.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static Thread updaterThread(HiveMetaStoreClient client) throws Exception {
    Field f = HiveMetaStoreClient.class.getDeclaredField("clientCertUpdaterThread");
    f.setAccessible(true);
    return (Thread) f.get(client);
  }

  /** Runs the private starter the two getMaterialForUser() paths funnel through. */
  private static void startUpdater(HiveMetaStoreClient client) throws Exception {
    Class<?> materialClass =
        Class.forName("org.apache.hadoop.hive.metastore.HiveMetaStoreClient$HopsSecurityMaterial");
    Method m = HiveMetaStoreClient.class
        .getDeclaredMethod("startClientCertUpdater", materialClass);
    m.setAccessible(true);
    m.invoke(client, new Object[] { null });
  }

  /**
   * The reloader clears its own interrupt status by calling Thread.sleep, so it has to be
   * parked in sleep() before the interrupt for the defect to be reachable at all. Interrupting
   * a thread that has not got there yet leaves the flag set and the loop exits on its own,
   * which would make a broken implementation look fixed.
   */
  private static void awaitSleeping(Thread t) throws Exception {
    long deadline = System.currentTimeMillis() + AWAIT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      if (t.getState() == Thread.State.TIMED_WAITING) {
        return;
      }
      Thread.sleep(5);
    }
    throw new AssertionError("reloader never reached Thread.sleep(), state=" + t.getState());
  }

  private static boolean died(Thread t) throws Exception {
    t.join(AWAIT_MILLIS);
    return !t.isAlive();
  }

  @Test
  public void closeStopsTheReloader() throws Exception {
    HiveMetaStoreClient client = newUnconnectedClient();
    startUpdater(client);
    Thread updater = updaterThread(client);
    assertNotNull(updater);
    awaitSleeping(updater);

    client.close();

    assertTrue("close() must stop the certificate reloader", died(updater));
  }

  @Test
  public void startingASecondReloaderStopsTheFirst() throws Exception {
    HiveMetaStoreClient client = newUnconnectedClient();
    startUpdater(client);
    Thread first = updaterThread(client);
    awaitSleeping(first);

    // open() reaches getHopsSecurityMaterial() twice: once building the TLS transport and
    // once for set_crypto after connecting.
    startUpdater(client);
    Thread second = updaterThread(client);
    assertNotSame("the second start must replace the tracked thread", first, second);

    assertTrue("the superseded reloader must be stopped, not orphaned", died(first));
  }

  /** The whole point: a client that connected once leaves no reloader behind. */
  @Test
  public void aClosedClientLeavesNoReloaderRunning() throws Exception {
    HiveMetaStoreClient client = newUnconnectedClient();
    startUpdater(client);
    Thread first = updaterThread(client);
    awaitSleeping(first);
    startUpdater(client);
    Thread second = updaterThread(client);
    awaitSleeping(second);

    client.close();

    assertTrue("first reloader still running after close()", died(first));
    assertTrue("second reloader still running after close()", died(second));
  }
}
