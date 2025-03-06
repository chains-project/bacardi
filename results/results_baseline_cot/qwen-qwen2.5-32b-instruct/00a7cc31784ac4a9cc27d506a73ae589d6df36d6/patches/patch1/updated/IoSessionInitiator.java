package quickfix.mina.initiator;

import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.proxy.ProxyConnector;
import org.apache.mina.transport.socket.SocketConnector;
import quickfix.ConfigError;
import quickfix.LogUtil;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SystemTime;
import quickfix.mina.CompositeIoFilterChainBuilder;
import quickfix.mina.EventHandlingStrategy;
import quickfix.mina.NetworkingOptions;
import quickfix.mina.SessionConnector;
import quickfix.mina.message.FIXProtocolCodecFactory;
import quickfix.mina.ssl.SSLConfig;
import quickfix.mina.ssl.SSLContextFactory;
import quickfix.mina.ssl.SSLFilter;
import quickfix.mina.ssl.SSLSupport;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoSessionInitiator {
    private final static long CONNECT_POLL_TIMEOUT = 2000L;
    private final ScheduledExecutorService executor;
    private final ConnectTask reconnectTask;
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Future<?> reconnectFuture;

    public IoSessionInitiator(Session fixSession, SocketAddress[] socketAddresses,
            SocketAddress localAddress, IoFilterChainBuilder userIoFilterChainBuilder,
            SessionSettings sessionSettings, NetworkingOptions networkingOptions, EventHandlingStrategy eventHandlingStrategy, SSLConfig sslConfig,
            String proxyType, String proxyVersion, String proxyHost, int proxyPort, String proxyUser, String proxyPassword, String proxyDomain, String proxyWorkstation, ScheduledExecutorService executor) throws ConfigError, GeneralSecurityException {
        this.executor = executor;
        this.reconnectTask = new ConnectTask(sslEnabled, socketAddresses, localAddress, userIoFilterChainBuilder, fixSession, reconnectIntervalInMillis, sessionSettings, networkingOptions, eventHandlingStrategy, sslConfig, proxyType, proxyVersion, proxyHost, proxyPort, proxyUser, proxyPassword, proxyDomain, proxyWorkstation, log);
    }

    private static class ConnectTask implements Runnable {
        private final boolean sslEnabled;
        private final SocketAddress[] socketAddresses;
        private final SocketAddress localAddress;
        private final IoFilterChainBuilder userIoFilterChainBuilder;
        private IoConnector ioConnector;
        private final Session fixSession;
        private final long[] reconnectIntervalInMillis;
        private final SessionSettings sessionSettings;
        private final NetworkingOptions networkingOptions;
        private final EventHandlingStrategy eventHandlingStrategy;
        private final SSLConfig sslConfig;
        private final Logger log;

        private final String proxyType;
        private final String proxyVersion;
        private final String proxyHost;
        private final int proxyPort;
        private final String proxyUser;
        private final String proxyPassword;
        private final String proxyDomain;
        private final String proxyWorkstation;

        public ConnectTask(boolean sslEnabled, SocketAddress[] socketAddresses,
                SocketAddress localAddress, IoFilterChainBuilder userIoFilterChainBuilder,
                Session fixSession, long[] reconnectIntervalInMillis,
                SessionSettings sessionSettings, NetworkingOptions networkingOptions, EventHandlingStrategy eventHandlingStrategy, SSLConfig sslConfig,
                String proxyType, String proxyVersion, String proxyHost, int proxyPort, String proxyUser, String proxyPassword, String proxyDomain, String proxyWorkstation, Logger log) throws ConfigError, GeneralSecurityException {
            this.sslEnabled = sslEnabled;
            this.socketAddresses = socketAddresses;
            this.localAddress = localAddress;
            this.userIoFilterChainBuilder = userIoFilterChainBuilder;
            this.fixSession = fixSession;
            this.reconnectIntervalInMillis = reconnectIntervalInMillis;
            this.sessionSettings = sessionSettings;
            this.networkingOptions = networkingOptions;
            this.eventHandlingStrategy = eventHandlingStrategy;
            this.sslConfig = sslConfig;
            this.log = log;

            this.proxyType = proxyType;
            this.proxyVersion = proxyVersion;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.proxyUser = proxyUser;
            this.proxyPassword = proxyPassword;
            this.proxyDomain = proxyDomain;
            this.proxyWorkstation = proxyWorkstation;

            setupIoConnector();
        }

        private void setupIoConnector() throws ConfigError, GeneralSecurityException {
            final CompositeIoFilterChainBuilder ioFilterChainBuilder = new CompositeIoFilterChainBuilder(userIoFilterChainBuilder);

            boolean hasProxy = proxyType != null && proxyPort > 0 && socketAddresses[nextSocketAddressIndex] instanceof InetSocketAddress;

            SSLFilter sslFilter = null;
            if (sslEnabled) {
                sslFilter = installSslFilter(ioFilterChainBuilder, !hasProxy);
            }

            ioFilterChainBuilder.addLast(SSLSupport.FILTER_NAME, sslFilter);
            ioFilterChainBuilder.addLast(FIXProtocolCodecFactory.FILTER_NAME, new ProtocolCodecFilter(new FIXProtocolCodecFactory()));

            IoConnector newConnector;
            newConnector = ProtocolFactory.createIoConnector(socketAddresses[nextSocketAddressIndex]);
            networkingOptions.apply(newConnector);
            newConnector.setHandler(new InitiatorIoHandler(fixSession, sessionSettings, networkingOptions, eventHandlingStrategy));
            newConnector.setFilterChainBuilder(ioFilterChainBuilder);

            if (hasProxy) {
                ProxyConnector proxyConnector = ProtocolFactory.createIoProxyConnector(
                        (SocketConnector) newConnector,
                        (InetSocketAddress) socketAddresses[nextSocketAddressIndex],
                        new InetSocketAddress(proxyHost, proxyPort),
                        proxyType, proxyVersion, proxyUser, proxyPassword, proxyDomain, proxyWorkstation
                );

                proxyConnector.setHandler(new InitiatorProxyIoHandler(
                        new InitiatorIoHandler(fixSession, sessionSettings, networkingOptions, eventHandlingStrategy),
                        sslFilter
                ));

                newConnector = proxyConnector;
            }

            if (ioConnector != null) {
                SessionConnector.closeManagedSessionsAndDispose(ioConnector, true, log);
            }
            ioConnector = newConnector;
        }

        private SSLFilter installSslFilter(CompositeIoFilterChainBuilder ioFilterChainBuilder, boolean autoStart)
                throws GeneralSecurityException {
            final SSLContext sslContext = SSLContextFactory.getInstance(sslConfig);
            final SSLFilter sslFilter = new SSLFilter(sslContext, autoStart);
            sslFilter.setCipherSuites(sslConfig.getEnabledCipherSuites() != null ? sslConfig.getEnabledCipherSuites()
                    : SSLSupport.getDefaultCipherSuites(sslContext));
            sslFilter.setEnabledProtocols(sslConfig.getEnabledProtocols() != null ? sslConfig.getEnabledProtocols()
                    : SSLSupport.getSupportedProtocols(sslContext);
            sslFilter.setUseSNI(sslConfig.isUseSNI());
            return sslFilter;
        }

        // ... (rest of the class remains unchanged)
    }

    // ... (rest of the class remains unchanged)
}