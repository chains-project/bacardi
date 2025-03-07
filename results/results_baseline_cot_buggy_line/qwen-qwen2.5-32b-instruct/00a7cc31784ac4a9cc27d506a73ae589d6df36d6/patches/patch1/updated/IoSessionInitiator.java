/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix.mina.initiator;

import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.proxy.ProxyConnector;
import quickfix.ConfigError;
import quickfix.LogUtil;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SystemTime;
import quickfix.mina.CompositeIoFilterChainBuilder;
import quickfix.mina.EventHandlingStrategy;
import quickfix.mina.NetworkingOptions;
import quickfix.mina.ProtocolFactory;
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

public class IoSessionInitiator {
    private final static long CONNECT_POLL_TIMEOUT = 2000L;
    private final ScheduledExecutorService executor;
    private final ConnectTask reconnectTask;
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Future<?> reconnectFuture;

    public IoSessionInitiator(Session fixSession, SocketAddress[] socketAddresses,
            SocketAddress localAddress, IoFilterChainBuilder userIoFilterChainBuilder,
            SessionSettings sessionSettings, NetworkingOptions networkingOptions, EventHandlingStrategy eventHandlingStrategy, SSLConfig sslConfig,
            String proxyType, String proxyVersion, String proxyHost, int proxyPort, String proxyUser, String proxyPassword, String proxyDomain, String proxyWorkstation) throws ConfigError, GeneralSecurityException {
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

        private SSLFilter installSslFilter(CompositeIoFilterChainBuilder ioFilterChainBuilder, boolean autoStart)
                throws GeneralSecurityException {
            final SSLContext sslContext = SSLContextFactory.getInstance(sslConfig);
            final SSLFilter sslFilter = new SSLFilter(sslContext, autoStart);
            // Removed setUseClientMode as it is not available in the updated dependency
            sslFilter.setCipherSuites(sslConfig.getEnabledCipherSuites() != null ? sslConfig.getEnabledCipherSuites()
                    : SSLSupport.getDefaultCipherSuites(sslContext));
            sslFilter.setEnabledProtocols(sslConfig.getEnabledProtocols() != null ? sslConfig.getEnabledProtocols()
                    : SSLSupport.getSupportedProtocols(sslContext));
            sslFilter.setUseSNI(sslConfig.isUseSNI());
            ioFilterChainBuilder.addLast(SSLSupport.FILTER_NAME, sslFilter);
            return sslFilter;
        }

        @Override
        public void run() {
            resetIoConnector();
            try {
                if (connectFuture == null) {
                    if (shouldReconnect()) {
                        connect();
                    }
                } else {
                    pollConnectFuture();
                }
            } catch (Throwable e) {
                handleConnectException(e);
            }
        }

        private void connect() {
            try {
                lastReconnectAttemptTime = SystemTime.currentTimeMillis();
                SocketAddress nextSocketAddress = getNextSocketAddress();
                if (localAddress == null) {
                    connectFuture = ioConnector.connect(nextSocketAddress);
                } else {
                    // QFJ-482
                    connectFuture = ioConnector.connect(nextSocketAddress, localAddress);
                }
                pollConnectFuture();
            } catch (Throwable e) {
                handleConnectException(e);
            }
        }

        private void pollConnectFuture() {
            try {
                connectFuture.awaitUninterruptibly(CONNECT_POLL_TIMEOUT);
                if (connectFuture.getSession() != null) {
                    ioSession = connectFuture.getSession();
                    connectionFailureCount = 0;
                    nextSocketAddressIndex = 0;
                    lastConnectTime = System.currentTimeMillis();
                    connectFuture = null;
                } else {
                    fixSession.getLog().onEvent(
                            "Pending connection not established after "
                                    + (System.currentTimeMillis() - lastReconnectAttemptTime)
                                    + " ms.");
                }
            } catch (Throwable e) {
                handleConnectException(e);
            }
        }

        private void handleConnectException(Throwable e) {
            ++connectionFailureCount;
            SocketAddress socketAddress = socketAddresses[getCurrentSocketAddressIndex()];
            while (e.getCause() != null) {
                e = e.getCause();
            }
            final String nextRetryMsg = " (Next retry in " + computeNextRetryConnectDelay() + " milliseconds)";
            if (e instanceof IOException) {
                fixSession.getLog().onErrorEvent(e.getClass().getName() + " during connection to " + socketAddress + ": " + e + nextRetryMsg);
                fixSession.getStateListener().onConnectException(fixSession.getSessionID(), (IOException) e);
            } else {
                LogUtil.logThrowable(fixSession.getLog(), "Exception during connection to " + socketAddress + nextRetryMsg, e);
                fixSession.getStateListener().onConnectException(fixSession.getSessionID(), new Exception(e));
            }
            connectFuture = null;
        }

        private SocketAddress getNextSocketAddress() {
            SocketAddress socketAddress = socketAddresses[nextSocketAddressIndex];

            // Recreate socket address to avoid cached address resolution
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress inetAddr = (InetSocketAddress) socketAddress;
                socketAddress = new InetSocketAddress(inetAddr.getHostName(), inetAddr.getPort());
                socketAddresses[nextSocketAddressIndex] = socketAddress;
            }
            nextSocketAddressIndex = (nextSocketAddressIndex + 1) % socketAddresses.length;
            return socketAddress;
        }

        private int getCurrentSocketAddressIndex() {
            return (nextSocketAddressIndex + socketAddresses.length - 1) % socketAddresses.length;
        }

        private boolean shouldReconnect() {
            return (ioSession == null || !ioSession.isConnected()) && isTimeForReconnect()
                    && (fixSession.isEnabled() && fixSession.isSessionTime());
        }

        private long computeNextRetryConnectDelay() {
            int index = connectionFailureCount - 1;
            if (index < 0)
                index = 0;
            return reconnectIntervalInMillis[index];
        }

        private boolean isTimeForReconnect() {
            return SystemTime.currentTimeMillis() - lastReconnectAttemptTime >= computeNextRetryConnectDelay();
        }

        private void resetIoConnector() {
            if (ioSession != null && Boolean.TRUE.equals(ioSession.getAttribute(SessionConnector.QFJ_RESET_IO_CONNECTOR))) {
                try {
                    setupIoConnector();
                    if (connectFuture != null) {
                        connectFuture.cancel();
                    }
                    connectFuture = null;
                    if (!ioSession.isClosing()) {
                        ioSession.closeNow();
                    }
                    ioSession = null;
                } catch (Throwable e) {
                    LogUtil.logThrowable(fixSession.getLog(), "Exception during resetIoConnector call", e);
                }
            }
        }
    }

    synchronized void start() {
        if (reconnectFuture == null) {
            // The following logon reenabled the session. The actual logon will take place as a side-effect of the session timer task (not the reconnect task).
            reconnectTask.getFixSession().logon(); // only enables the session
            reconnectFuture = executor
                    .scheduleWithFixedDelay(reconnectTask, 0, 1, TimeUnit.SECONDS);
        }
    }

    synchronized void stop() {
        if (reconnectFuture != null) {
            reconnectFuture.cancel(true);
            reconnectFuture = null;
        }
        SessionConnector.closeManagedSessionsAndDispose(reconnectTask.ioConnector, true, log);
    }

    public SessionID getSessionID() {
        return reconnectTask.fixSession.getSessionID();
    }

    public SocketAddress getLocalAddress() {
        return reconnectTask.localAddress;
    }

    public SocketAddress[] getSocketAddresses() {
        return Arrays.copyOf(reconnectTask.socketAddresses, reconnectTask.socketAddresses.length);
    }
}