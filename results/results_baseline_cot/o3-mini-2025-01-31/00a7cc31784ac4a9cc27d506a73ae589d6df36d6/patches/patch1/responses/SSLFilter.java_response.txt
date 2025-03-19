package quickfix.mina.ssl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLFilter extends SslFilter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private boolean useSNI;
    private static final String PEER_ADDRESS = "PEER_ADDRESS";

    public SSLFilter(SSLContext sslContext, boolean autoStart) {
        super(sslContext);
    }

    public SSLFilter(SSLContext sslContext) {
        super(sslContext);
    }

    @Override
    public void setEnabledCipherSuites(String[] cipherSuites) {
    }

    public void setCipherSuites(String[] cipherSuites) {
        super.setEnabledCipherSuites(cipherSuites);
    }

    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter)
        throws SSLException {

        if (useSNI) {
            IoSession session = parent.getSession();
            SocketAddress remoteAddress = session.getRemoteAddress();

            if (remoteAddress instanceof InetSocketAddress) {
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