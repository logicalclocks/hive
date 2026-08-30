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
package io.hops.hive.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.hive.service.rpc.thrift.TCLIService;
import org.apache.thrift.TConfiguration;
import org.apache.thrift.transport.TTransport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Covers the resources a connection has to give back. The interesting case is a connection
 * that never finished opening: isClosed stays true until the very last statement of
 * openSession(), so close() must not treat it as "nothing to do" -- openTransport() has
 * already bound a socket by then.
 */
public class TestHiveConnectionClose {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  /** A transport that records whether it was closed. */
  private static class RecordingTransport extends TTransport {
    private boolean open = true;
    private int closeCalls = 0;

    @Override public boolean isOpen() { return open; }
    @Override public void open() { open = true; }
    @Override public void close() { closeCalls++; open = false; }
    @Override public int read(byte[] buf, int off, int len) { return 0; }
    @Override public void write(byte[] buf, int off, int len) { }
    @Override public TConfiguration getConfiguration() { return new TConfiguration(); }
    @Override public void updateKnownMessageSize(long size) { }
    @Override public void checkReadBytesAvailable(long numBytes) { }
  }

  private static void set(Object target, String field, Object value) throws Exception {
    Field f = HiveConnection.class.getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, value);
  }

  /**
   * The connect path that leaked: openTransport() bound the socket, openSession() then threw,
   * so isClosed was never flipped to false and close() skipped the whole body.
   */
  @Test
  public void closeReleasesTheTransportOfAConnectionThatNeverOpenedASession() throws Exception {
    HiveConnection conn = new HiveConnection();
    RecordingTransport transport = new RecordingTransport();
    set(conn, "transport", transport);
    // isClosed is left at its initial value, which is what a failed openSession() leaves behind
    assertTrue("precondition: the connection must look unopened", conn.isClosed());

    conn.close();

    assertEquals("the bound transport must be closed exactly once", 1, transport.closeCalls);
    assertFalse(transport.isOpen());
  }

  /** An established connection must still close its server session, then the transport. */
  @Test
  public void closeEndsTheSessionAndThenTheTransport() throws Exception {
    List<String> calls = new ArrayList<>();
    TCLIService.Iface client = (TCLIService.Iface) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] { TCLIService.Iface.class },
        new InvocationHandler() {
          @Override public Object invoke(Object proxy, Method m, Object[] args) {
            calls.add(m.getName());
            return null;
          }
        });

    HiveConnection conn = new HiveConnection();
    RecordingTransport transport = new RecordingTransport();
    set(conn, "transport", transport);
    set(conn, "client", client);
    set(conn, "isClosed", Boolean.FALSE);

    conn.close();

    assertEquals("the server session must be closed", 1, calls.size());
    assertEquals("CloseSession", calls.get(0));
    assertEquals(1, transport.closeCalls);
    assertTrue(conn.isClosed());
  }

  /** close() is idempotent: try-with-resources and an explicit close must not double-close. */
  @Test
  public void closeIsIdempotent() throws Exception {
    HiveConnection conn = new HiveConnection();
    RecordingTransport transport = new RecordingTransport();
    set(conn, "transport", transport);

    conn.close();
    conn.close();

    assertEquals("the transport must not be closed twice", 1, transport.closeCalls);
  }

  /** The init-sql path must not leak the statement or the result set it opens. */
  @Test
  public void executeInitSqlClosesItsStatementAndResultSet() throws Exception {
    File init = tmp.newFile("init.sql");
    try (FileWriter w = new FileWriter(init)) {
      w.write("SELECT 1;\n");
    }

    List<String> closed = new ArrayList<>();
    Object[] resultSetHolder = new Object[1];
    ResultSet rs = (ResultSet) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] { ResultSet.class },
        new InvocationHandler() {
          @Override public Object invoke(Object proxy, Method m, Object[] args) {
            if ("close".equals(m.getName())) { closed.add("resultSet"); return null; }
            if ("next".equals(m.getName())) { return Boolean.FALSE; }
            return null;
          }
        });
    resultSetHolder[0] = rs;
    Statement st = (Statement) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[] { Statement.class },
        new InvocationHandler() {
          @Override public Object invoke(Object proxy, Method m, Object[] args) {
            switch (m.getName()) {
              case "execute": return Boolean.TRUE;
              case "getResultSet": return resultSetHolder[0];
              case "close": closed.add("statement"); return null;
              default: return null;
            }
          }
        });

    HiveConnection conn = new HiveConnection() {
      @Override public Statement createStatement() { return st; }
    };
    set(conn, "initFile", init.getAbsolutePath());

    Method executeInitSql = HiveConnection.class.getDeclaredMethod("executeInitSql");
    executeInitSql.setAccessible(true);
    executeInitSql.invoke(conn);

    assertTrue("the init-sql result set must be closed", closed.contains("resultSet"));
    assertTrue("the init-sql statement must be closed", closed.contains("statement"));
  }

  /** Guards the contract the fix relies on: nothing else may be needed to reach close(). */
  @Test
  public void aFreshConnectionReportsItselfClosed() throws Exception {
    assertTrue(new HiveConnection().isClosed());
  }
}
