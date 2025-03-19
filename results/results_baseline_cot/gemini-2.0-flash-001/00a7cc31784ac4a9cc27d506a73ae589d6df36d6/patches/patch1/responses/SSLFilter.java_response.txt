package quickfix.mina.ssl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.net.ssl.SSLContext;

import javax.net.ssl.SSLException;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extended SSL filter based on MINA {@link SslFilter} that applies
 * some adaptations.
 */
public class SSLFilter extends SslFilter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private boolean useSNI;
    private static final String PEER_ADDRESS = SSLFilter.class.getName() + ".peerAddress";


    public SSLFilter(SSLContext sslContext, boolean autoStart) {
        super(sslContext);
    }

    public SSLFilter(SSLContext sslContext) {
        super(sslContext);
    }

    /**
     * Called from {@link SslFilter#onPreAdd} every time a new
     * session is created which makes it impossible to override enabled cipher
     * suites configuration.
     */
    @Override
    public void setEnabledCipherSuites(String[] cipherSuites) {
    }

    public void setCipherSuites(String[] cipherSuites) {
        super.setEnabledCipherSuites(cipherSuites);
    }

    /**
     * Called before filter is added into the chain.
     * We activate Server Name Indication if it is enabled in the session config.
     */
    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter)
        throws SSLException {

        if (useSNI) {
            IoSession session = parent.getSession();
            SocketAddress remoteAddress = session.getRemoteAddress();

            if (remoteAddress instanceof InetSocketAddress) {
                // activate the SNI support in the JSSE SSLEngine
                log.info("Activating TLS SNI support for peer address: {}", remoteAddress);
                session.setAttribute(PEER_ADDRESS, remoteAddress);
            }
        }

        super.onPreAdd(parent, name, nextFilter);
    }

    public void setUseSNI(boolean useSNI) {
        this.useSNI = useSNI;
    }
}