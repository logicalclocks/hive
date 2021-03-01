package org.apache.hadoop.hive.metastore.auth;

import org.apache.hadoop.conf.Configuration;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;

import static io.hops.hadoop.shaded.org.apache.hadoop.security.ssl.SSLFactory.DEFAULT_SSL_ENABLED_PROTOCOLS;
import static io.hops.hadoop.shaded.org.apache.hadoop.security.ssl.SSLFactory.SSL_ENABLED_PROTOCOLS;
import static io.hops.hadoop.shaded.org.apache.hadoop.security.ssl.SSLFactory.SSL_SERVER_EXCLUDE_CIPHER_LIST;

public class TServerSocketFactory {

  public enum TSocketType {
    PLAIN,
    TLS,
    TWOWAYTLS
  }

  public static TServerSocket getServerSocket(Configuration conf, TSocketType socketType,
                                              String hiveHost, int portNum ) throws TTransportException {
    InetSocketAddress serverAddress = getServerAddress(hiveHost, portNum);
    switch (socketType) {
      case PLAIN:
        return new TServerSocket(serverAddress);
      default:
        HopsTLSTSocketFactory.HopsTLSTransportParams params =
            new HopsTLSTSocketFactory.HopsTLSTransportParams();
        params.ifAddress = serverAddress;
        params.clientAuth = socketType == TSocketType.TWOWAYTLS;
        params.clientTimeout = 0;
        params.enabledProtocols = conf.getStrings(SSL_ENABLED_PROTOCOLS, DEFAULT_SSL_ENABLED_PROTOCOLS);

        String excludeCiphersConf = conf.get(SSL_SERVER_EXCLUDE_CIPHER_LIST, "");
        if (excludeCiphersConf.isEmpty()) {
          params.excludeCiphers = new LinkedList<>();
        } else {
          params.excludeCiphers = Arrays.asList(excludeCiphersConf.split(","));
        }

        return HopsTLSTSocketFactory.getServerSocket(conf, params);
    }
  }

  private static InetSocketAddress getServerAddress(String hiveHost, int portNum) {
    InetSocketAddress serverAddress;
    if (hiveHost == null || hiveHost.isEmpty()) {
      // Wildcard bind
      serverAddress = new InetSocketAddress(portNum);
    } else {
      serverAddress = new InetSocketAddress(hiveHost, portNum);
    }
    return serverAddress;
  }
}