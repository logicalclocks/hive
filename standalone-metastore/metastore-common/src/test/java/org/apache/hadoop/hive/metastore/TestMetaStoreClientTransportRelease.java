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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.thrift.TConfiguration;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * A connect attempt that fails after the socket is established must release it. open() reassigns
 * the transport field on every iteration of its retry loop, so a socket left behind by a failed
 * attempt becomes unreachable with no handle to close it, and the metastore goes on holding a
 * worker thread for it until the client process exits.
 *
 * <p>set_crypto is the path that makes this reachable: it runs after transport.open() has
 * succeeded and the TLS handshake is done, and a failure there throws MetaException, which the
 * loop only records before trying the next URI.
 *
 * <p>These tests drive the release directly rather than through open(), which would need a fake
 * metastore speaking Hops TLS. What they do assert is the part that was actually broken: that the
 * socket is genuinely returned to the peer, and that the shutdown path and the failed-attempt path
 * account for it identically.
 */
public class TestMetaStoreClientTransportRelease {

  private static final int TIMEOUT_MILLIS = 10_000;

  private ServerSocket server;
  private Socket accepted;

  @Before
  public void startPeer() throws Exception {
    server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
  }

  @After
  public void stopPeer() throws Exception {
    if (accepted != null) {
      accepted.close();
    }
    if (server != null) {
      server.close();
    }
  }

  /**
   * HiveMetaStoreClient's only constructor connects to a metastore. mock() builds the instance
   * without running it; CALLS_REAL_METHODS keeps close() and the release path real.
   */
  private HiveMetaStoreClient newUnconnectedClient() throws Exception {
    HiveMetaStoreClient client = Mockito.mock(HiveMetaStoreClient.class,
        Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
    Configuration conf = MetastoreConf.newMetastoreConf();
    setField(client, "conf", conf);
    return client;
  }

  /** An established connection to the peer, as a failed attempt would leave behind. */
  private TTransport connectedTransport() throws Exception {
    TSocket socket = new TSocket(new TConfiguration(), server.getInetAddress().getHostAddress(),
        server.getLocalPort(), TIMEOUT_MILLIS, TIMEOUT_MILLIS);
    socket.open();
    accepted = server.accept();
    accepted.setSoTimeout(TIMEOUT_MILLIS);
    assertTrue("precondition: the transport must be open", socket.isOpen());
    return socket;
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field f = HiveMetaStoreClient.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static void invokeCloseTransport(HiveMetaStoreClient client) throws Exception {
    Method m = HiveMetaStoreClient.class.getDeclaredMethod("closeTransport");
    m.setAccessible(true);
    m.invoke(client);
  }

  /** connCount is static, so only the delta across a call is meaningful. */
  private static int connCount() throws Exception {
    Field f = HiveMetaStoreClient.class.getDeclaredField("connCount");
    f.setAccessible(true);
    return ((AtomicInteger) f.get(null)).get();
  }

  /** The peer sees EOF only once the socket is really gone, not merely flagged closed. */
  private void assertPeerSawDisconnect() throws Exception {
    InputStream in = accepted.getInputStream();
    assertEquals("the metastore side must see the connection go away", -1, in.read());
  }

  @Test
  public void releasingAFailedAttemptClosesTheSocket() throws Exception {
    HiveMetaStoreClient client = newUnconnectedClient();
    TTransport transport = connectedTransport();
    setField(client, "transport", transport);
    int before = connCount();

    invokeCloseTransport(client);

    assertFalse("the abandoned transport must be closed", transport.isOpen());
    assertPeerSawDisconnect();
    assertEquals("the release must be accounted for exactly once", before - 1, connCount());
  }

  @Test
  public void shutdownReleasesTheSocketTheSameWay() throws Exception {
    HiveMetaStoreClient client = newUnconnectedClient();
    TTransport transport = connectedTransport();
    setField(client, "transport", transport);
    int before = connCount();

    client.close();

    assertFalse("close() must release the transport", transport.isOpen());
    assertPeerSawDisconnect();
    assertEquals(before - 1, connCount());
  }

  /**
   * open() calls the release on every failed attempt, including ones that never got a socket, and
   * close() then runs over whatever is left. Neither may throw or double-count.
   */
  @Test
  public void releasingNothingIsANoOp() throws Exception {
    HiveMetaStoreClient client = newUnconnectedClient();
    int before = connCount();

    invokeCloseTransport(client);
    assertEquals("a client with no transport must not be counted", before, connCount());

    TTransport transport = connectedTransport();
    setField(client, "transport", transport);
    invokeCloseTransport(client);
    assertEquals(before - 1, connCount());

    // A failed attempt already released this one; shutdown must not decrement it again.
    invokeCloseTransport(client);
    client.close();
    assertEquals("an already-released transport must not be counted twice",
        before - 1, connCount());
  }
}
