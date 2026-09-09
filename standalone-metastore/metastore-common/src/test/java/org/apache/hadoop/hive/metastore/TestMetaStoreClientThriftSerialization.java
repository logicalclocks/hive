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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf.ConfVars;
import org.apache.thrift.TException;
import org.junit.Test;

/**
 * A generated Thrift client owns one socket and one {@code seqid_}, so every caller of it has to
 * be serialised. Callers of the {@link IMetaStoreClient} API already are, by
 * {@code newSynchronizedClient()}, but {@code ClientCertUpdater} holds the {@code client} field
 * directly and pushes rotated certificates with {@code set_crypto} from its own thread, which
 * bypasses that wrapper. The result is a corrupted reply stream: {@code TApplicationException:
 * ... out of sequence response: expected N but got N-1} when the interleaving lands on a message
 * boundary, an unparseable body when it lands inside one.
 *
 * <p>The load-bearing test here is {@link #anUnwrappedClientIsNotSerialised()}. Without it the
 * two serialisation tests could pass on a broken build simply because the harness never managed
 * to overlap two callers, and a concurrency test that cannot observe the defect it guards is
 * worse than no test. It asserts the same harness does see overlap on a bare client.
 */
public class TestMetaStoreClientThriftSerialization {

  private static final int THREADS = 8;
  private static final int CALLS_PER_THREAD = 40;
  private static final long AWAIT_SECONDS = 30;

  /**
   * Counts how many callers are inside the delegate at once. The sleep is what makes the window
   * wide enough to observe: without it two unserialised threads can interleave and still never
   * be inside the recorder simultaneously.
   */
  private static final class CallRecorder implements InvocationHandler {
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger maxInFlight = new AtomicInteger();
    private final AtomicLong calls = new AtomicLong();

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      int now = inFlight.incrementAndGet();
      maxInFlight.accumulateAndGet(now, Math::max);
      try {
        Thread.sleep(1);
        return null;
      } finally {
        calls.incrementAndGet();
        inFlight.decrementAndGet();
      }
    }
  }

  private static ThriftHiveMetastore.Iface fakeClient(InvocationHandler handler) {
    return (ThriftHiveMetastore.Iface) Proxy.newProxyInstance(
        TestMetaStoreClientThriftSerialization.class.getClassLoader(),
        new Class[]{ThriftHiveMetastore.Iface.class},
        handler);
  }

  /**
   * The wrapper is private, so it is reached the same way {@code TestClientCertUpdaterLifecycle}
   * reaches {@code startClientCertUpdater}: production keeps its narrow surface, the test does
   * not get a seam it would then be the only user of.
   */
  private static ThriftHiveMetastore.Iface wrap(ThriftHiveMetastore.Iface delegate)
      throws Exception {
    Method m = HiveMetaStoreClient.class
        .getDeclaredMethod("newSynchronizedThriftClient", ThriftHiveMetastore.Iface.class);
    m.setAccessible(true);
    return (ThriftHiveMetastore.Iface) m.invoke(null, delegate);
  }

  /** An ordinary metastore call, standing in for whatever the application thread is doing. */
  private static void applicationCall(ThriftHiveMetastore.Iface client) throws TException {
    client.get_databases("*");
  }

  /** Exactly the call ClientCertUpdater.run() makes on the shared client. */
  private static void certReloaderCall(ThriftHiveMetastore.Iface client) throws TException {
    client.set_crypto(ByteBuffer.allocate(1), "pw", ByteBuffer.allocate(1), "pw", true);
  }

  private interface Body {
    void run(ThriftHiveMetastore.Iface client) throws Exception;
  }

  /** Runs {@code bodies.length} threads concurrently, rethrowing the first failure. */
  private static void hammer(ThriftHiveMetastore.Iface client, Body... bodies) throws Exception {
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(bodies.length);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    for (Body body : bodies) {
      Thread t = new Thread(() -> {
        try {
          start.await();
          for (int i = 0; i < CALLS_PER_THREAD; i++) {
            body.run(client);
          }
        } catch (Throwable e) {
          failure.compareAndSet(null, e);
        } finally {
          done.countDown();
        }
      });
      t.setDaemon(true);
      t.start();
    }
    start.countDown();
    assertTrue("threads did not finish within " + AWAIT_SECONDS + "s",
        done.await(AWAIT_SECONDS, TimeUnit.SECONDS));
    if (failure.get() != null) {
      throw new AssertionError("a caller failed", failure.get());
    }
  }

  private static Body[] applicationCallers(int n) {
    Body[] bodies = new Body[n];
    for (int i = 0; i < n; i++) {
      bodies[i] = TestMetaStoreClientThriftSerialization::applicationCall;
    }
    return bodies;
  }

  /**
   * The harness has to be able to fail. If a bare client shows no overlap then the two tests
   * below prove nothing, so this is asserted first and separately rather than assumed.
   */
  @Test
  public void anUnwrappedClientIsNotSerialised() throws Exception {
    CallRecorder recorder = new CallRecorder();
    hammer(fakeClient(recorder), applicationCallers(THREADS));

    assertEquals("every call must reach the delegate",
        (long) THREADS * CALLS_PER_THREAD, recorder.calls.get());
    assertTrue("the harness cannot observe concurrency, so it cannot guard against it; "
            + "observed max in-flight " + recorder.maxInFlight.get(),
        recorder.maxInFlight.get() > 1);
  }

  @Test
  public void concurrentCallersAreSerialised() throws Exception {
    CallRecorder recorder = new CallRecorder();
    hammer(wrap(fakeClient(recorder)), applicationCallers(THREADS));

    assertEquals("every call must reach the delegate",
        (long) THREADS * CALLS_PER_THREAD, recorder.calls.get());
    assertEquals("two threads were inside one Thrift client at once",
        1, recorder.maxInFlight.get());
  }

  /**
   * The reported defect: the certificate reloader's set_crypto against an application call. Both
   * sides are asserted to have run, so the test cannot pass by one of them doing nothing.
   */
  @Test
  public void theCertReloadersCallIsSerialisedWithApplicationCalls() throws Exception {
    CallRecorder recorder = new CallRecorder();
    hammer(wrap(fakeClient(recorder)),
        TestMetaStoreClientThriftSerialization::applicationCall,
        TestMetaStoreClientThriftSerialization::certReloaderCall);

    assertEquals("both callers must have run to completion",
        2L * CALLS_PER_THREAD, recorder.calls.get());
    assertEquals("set_crypto interleaved with an application call on one Thrift client",
        1, recorder.maxInFlight.get());
  }

  /**
   * The wrapper must not change what callers see on failure. {@code RetryingMetaStoreClient}
   * switches on the exception type, so an InvocationTargetException or an UndeclaredThrowable
   * leaking out here would silently change every retry decision above it.
   */
  @Test
  public void exceptionsFromTheDelegateArePreserved() throws Exception {
    TException thrown = new TException("from the delegate");
    ThriftHiveMetastore.Iface client = wrap(fakeClient((proxy, method, args) -> {
      throw thrown;
    }));

    try {
      applicationCall(client);
      fail("the delegate's exception must propagate");
    } catch (TException e) {
      assertSame("the delegate's own exception must arrive unwrapped", thrown, e);
    }
  }

  /**
   * Guards the reentrancy the current implementation gets from {@code synchronized}: a future
   * switch to a non-reentrant lock would deadlock here rather than in production.
   */
  @Test
  public void reentrantCallsDoNotDeadlock() throws Exception {
    AtomicReference<ThriftHiveMetastore.Iface> self = new AtomicReference<>();
    AtomicInteger depth = new AtomicInteger();
    self.set(wrap(fakeClient((proxy, method, args) -> {
      if (depth.getAndIncrement() == 0) {
        applicationCall(self.get());
      }
      return null;
    })));

    applicationCall(self.get());

    assertEquals("the nested call did not run", 2, depth.get());
  }

  /**
   * The wrapper is worthless if the connect path stops installing it, and that is a one-line
   * regression nothing else here would catch. Driven against a real loopback socket, the way
   * {@code TestMetaStoreClientTransportRelease} does, with set_ugi and Hops TLS off so open()
   * completes without issuing any RPC to a listener that would never answer.
   */
  @Test
  public void openInstallsTheSynchronizedWrapper() throws Exception {
    try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      Configuration conf = MetastoreConf.newMetastoreConf();
      MetastoreConf.setVar(conf, ConfVars.THRIFT_URIS,
          "thrift://" + listener.getInetAddress().getHostAddress() + ":" + listener.getLocalPort());
      MetastoreConf.setBoolVar(conf, ConfVars.EXECUTE_SET_UGI, false);
      MetastoreConf.setBoolVar(conf, ConfVars.METASTORE_HOPS_HIVE_TLS, false);
      // close() sends shutdown() to a listener that never answers; without this the test would
      // sit on the 600s default read timeout.
      MetastoreConf.setTimeVar(conf, ConfVars.CLIENT_SOCKET_TIMEOUT, 1, TimeUnit.SECONDS);

      try (HiveMetaStoreClient client = new HiveMetaStoreClient(conf)) {
        Object thriftClient = thriftClientField(client);
        assertTrue("open() must install the synchronizing wrapper, got "
                + (thriftClient == null ? "null" : thriftClient.getClass().getName()),
            thriftClient != null && Proxy.isProxyClass(thriftClient.getClass()));
        assertEquals("the wrapper must be this class's handler",
            "SynchronizedThriftHandler",
            Proxy.getInvocationHandler(thriftClient).getClass().getSimpleName());
      }
    }
  }

  private static Object thriftClientField(HiveMetaStoreClient client) throws Exception {
    java.lang.reflect.Field f = HiveMetaStoreClient.class.getDeclaredField("client");
    f.setAccessible(true);
    return f.get(client);
  }
}
