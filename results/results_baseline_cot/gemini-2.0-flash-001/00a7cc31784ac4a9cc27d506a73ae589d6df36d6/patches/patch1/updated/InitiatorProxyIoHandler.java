package quickfix.mina.initiator;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.proxy.AbstractProxyIoHandler;

import quickfix.mina.ssl.SSLFilter;

class InitiatorProxyIoHandler extends AbstractProxyIoHandler {
    private final InitiatorIoHandler initiatorIoHandler;
    private final SSLFilter sslFilter;

    InitiatorProxyIoHandler(InitiatorIoHandler initiatorIoHandler, SSLFilter sslFilter) {
        super();
        this.initiatorIoHandler = initiatorIoHandler;
        this.sslFilter = sslFilter;
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        this.initiatorIoHandler.sessionCreated(session);
    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        this.initiatorIoHandler.sessionClosed(ioSession);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        this.initiatorIoHandler.messageReceived(session, message);
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        this.initiatorIoHandler.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable cause) throws Exception {
        this.initiatorIoHandler.exceptionCaught(ioSession, cause);
    }

    @Override
    public void proxySessionOpened(IoSession ioSession) throws Exception {
        if (this.sslFilter != null) {
            ioSession.getFilterChain().addFirst("sslFilter", this.sslFilter);
        }
    }
}