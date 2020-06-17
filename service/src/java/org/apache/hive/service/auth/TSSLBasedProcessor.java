package org.apache.hive.service.auth;

import io.hops.security.CertificateLocalizationCtx;
import io.hops.security.HopsUtil;
import io.hops.security.HopsX509AuthenticationException;
import io.hops.security.HopsX509Authenticator;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.net.HopsSSLSocketFactory;
import org.apache.hive.service.rpc.thrift.TCLIService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.security.cert.X509Certificate;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TSSLBasedProcessor<I extends TCLIService.Iface> extends TSetIpAddressProcessor<TCLIService.Iface> {
  private static final Logger LOGGER = LoggerFactory.getLogger(TSetIpAddressProcessor.class.getName());
  private static final Pattern PROJECT_USER = Pattern.compile(HopsSSLSocketFactory.USERNAME_PATTERN);
  private final HopsX509Authenticator hopsX509Authenticator;
  private HiveConf hiveConf = null;

  public TSSLBasedProcessor(TCLIService.Iface iface, HiveConf hiveConf) {
    super(iface);
    this.hiveConf = hiveConf;
    hopsX509Authenticator = new HopsX509Authenticator(hiveConf);
    CertificateLocalizationCtx.getInstance().setProxySuperusers(hiveConf);
  }

  @Override
  protected void setUserName(final TProtocol in) throws TException {

    // Do not check the certificates if the username has already been set for this connection
    if (THREAD_LOCAL_USER_NAME.get() != null) {
      return;
    }

    try {
      TTransport transport = in.getTransport();
      Socket socket = ((TSocket) transport).getSocket();
      X509Certificate[] certs = ((SSLSocket) socket).getSession().getPeerCertificateChain();

      // Make sure it's 2 way ssl, i.e. client certificate is available
      if (certs.length == 0) {
        throw new TException("Missing certificates");
      }

      // Client certificate is always the first
      String DN = certs[0].getSubjectDN().getName();
      String cn = HopsUtil.extractCNFromSubject(DN);
      if (cn == null) {
        throw new TException("Cannot authenticate the user: Unrecognized CN format");
      }

      Matcher matcher = PROJECT_USER.matcher(cn);
      if (matcher.matches()) {
        // The certificate is in the format projectName__userName
        THREAD_LOCAL_USER_NAME.set(cn);
      } else {
        try {
          if (hopsX509Authenticator.isTrustedConnection(InetAddress.getByName(THREAD_LOCAL_IP_ADDRESS.get()), cn)) {
            String locality = HopsUtil.extractLFromSubject(DN);
            if (CertificateLocalizationCtx.getInstance().getProxySuperusers().contains(locality)) {
              // Operate as superuser
              THREAD_LOCAL_USER_NAME.set(hiveConf.getVar(HiveConf.ConfVars.HIVE_SUPER_USER));
              return;
            }
          }
        } catch (UnknownHostException ex) {
          LOGGER.error("Cannot resolve machine address: ", ex);
          throw new TException("Cannot authenticate the user");
        } catch (HopsX509AuthenticationException ex) {
          LOGGER.debug("Cannot authenticate super user", ex);
          throw new TException("Authentication failure", ex);
        }

        throw new TException("Failed to authenticate superuser");
      }
    } catch (SSLException e) {
      throw new TException(e);
    }
  }
}
