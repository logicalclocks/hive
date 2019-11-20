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

package org.apache.hadoop.hive.metastore.model.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import org.apache.hadoop.fs.Path;

/**
 * This class helps retrieve the inode id given the path of the file/directory
 */

public class InodeHelper implements Configurable {

  /**
   * STATIC VARIABLES FOR HOPS
   */
  private int ROOT_DIR_PARTITION_KEY = 0;
  private short ROOT_DIR_DEPTH = 0;
  private int RANDOM_PARTITIONING_MAX_LEVEL = 1;
  private long ROOT_INODE_ID = 1;

  private final Logger logger = LoggerFactory.getLogger(InodeHelper.class.getName());

  private static InodeHelper instance = null;
  private static DataSource connPool = null;

  private Configuration metastoreConf = null;

  public static InodeHelper getInstance() {
    if (instance == null) {
      instance = new InodeHelper();
    }

    return instance;
  }

  public void setConf(Configuration metastoreConf) {
    this.metastoreConf = metastoreConf;
    ROOT_DIR_PARTITION_KEY = MetastoreConf.getIntVar(metastoreConf, MetastoreConf.ConfVars.HOPSROOTDIRPARTITIONKEY);
    ROOT_DIR_DEPTH = (short)MetastoreConf.getIntVar(metastoreConf, MetastoreConf.ConfVars.HOPSROOTDIRDEPTH);
    RANDOM_PARTITIONING_MAX_LEVEL = MetastoreConf.getIntVar(metastoreConf, MetastoreConf.ConfVars.HOPSRANDOMPARTITIONINGMAXLEVEL);
    ROOT_INODE_ID = MetastoreConf.getIntVar(metastoreConf, MetastoreConf.ConfVars.HOPSROOTINODEID);

    if (connPool == null && MetastoreConf.getBoolVar(metastoreConf, MetastoreConf.ConfVars.METADATACONSISTENCY)){
      initConnections();
    }
  }

  public Configuration getConf() {
    return metastoreConf;
  }

  private synchronized void initConnections() {

    // Setup connections.
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(MetastoreConf.getVar(metastoreConf, MetastoreConf.ConfVars.HOPSDBURLKEY));
    config.setUsername(MetastoreConf.getVar(metastoreConf, MetastoreConf.ConfVars.CONNECTION_USER_NAME));
    config.setPassword(MetastoreConf.getVar(metastoreConf, MetastoreConf.ConfVars.PWD));

    connPool = new HikariDataSource(config);
  }

  private Connection getDbConn() throws SQLException {
    //Rety to get a connection up to 10 times
    int rc = 10;
    Connection dbConn = null;
    while (true) {
      try {
        dbConn = connPool.getConnection();
        return dbConn;
      } catch (SQLException e){
        if (dbConn != null && !dbConn.isClosed()) {
          dbConn.close();
        }
        if ((--rc) <= 0) throw e;
        logger.error("There is a problem with a connection from the pool, retrying", e);
      }
    }
  }

  public InodePK getInodePK(String path) throws MetaException{

    // Check MetastoreConf if consistency disabled.
    if (!MetastoreConf.getBoolVar(metastoreConf, MetastoreConf.ConfVars.METADATACONSISTENCY)){
      return new InodePK();
    }

    // Check for null paths (Virtual view case) or empty strings
    if (path == null || path.isEmpty()){
      return new InodePK();
    }

    // Strip ip address and port hopsfs://ip:port/path    
    Path p = new Path(path);
    path = p.toUri().getPath();

    // Get connection from the pool
    Connection dbConn = null;
    try {
      dbConn = getDbConn();
    } catch (SQLException e) {
      throw new MetaException(e.getMessage());
    }

    InodePK pk = getInodePK(dbConn, path);

    try {
      dbConn.close();
    } catch (SQLException e) {
      throw new MetaException(e.getMessage());
    }

    return pk;
  }

  private InodePK getInodePK(Connection dbConn, String path) throws MetaException{
    // Get the path components
    String[] p;
    if (path.charAt(0) == '/') {
      p = path.substring(1).split("/");
    } else {
      p = path.split("/");
    }

    if (p.length < 1) {
      throw new MetaException("Invalid Path");
    }

    long partitionId = calculatePartitionId(ROOT_INODE_ID, p[0], ROOT_DIR_DEPTH + 1);
    long parentId = ROOT_INODE_ID;

    //Get the right root node
    long curr = findByInodePK(dbConn, parentId, p[0], partitionId);
    if (curr == -1) {
      try {
        dbConn.close();
      } catch (SQLException e) { }
      throw new MetaException("Could not resolve inode at path: " + path);
    }

    //Move down the path
    for (int i = 1; i < p.length; i++) {
      partitionId = calculatePartitionId(curr, p[i], i+1);
      long next = findByInodePK(dbConn, curr, p[i], partitionId);
      if (next == -1) {
        try {
          dbConn.close();
        } catch (SQLException e) { }
        throw new MetaException("Could not resolve inode at path: " + path);
      } else {
        parentId = curr;
        curr = next;
      }
    }

    return new InodePK(partitionId, parentId, p[p.length-1]);
  }

  private long findByInodePK(Connection conn, long parentId, String name, long partitionId) throws MetaException{
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.prepareStatement(
          "SELECT id FROM hdfs_inodes WHERE partition_id = ? " +
              " and parent_id = ? " +
              " and name = ?");
      stmt.setLong(1, partitionId);
      stmt.setLong(2, parentId);
      stmt.setString(3, name);
      rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getLong("id");
      }
    } catch (SQLException e) {
      throw new MetaException(e.getMessage());
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException e) {
        throw new MetaException(e.getMessage());
      }
    }

    return -1;
  }

  private long calculatePartitionId(long parentId, String name, int depth) {
    if (depth <= RANDOM_PARTITIONING_MAX_LEVEL) {
      if (depth == ROOT_DIR_DEPTH) {
        return ROOT_DIR_PARTITION_KEY;
      } else {
        return (name + parentId).hashCode();
      }
    } else {
      return parentId;
    }
  }
}
