package net.jadler.stubbing.server.jetty;

import net.jadler.RequestManager;
import net.jadler.stubbing.server.StubHttpServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.Validate;

/**
 * Default stub http server implementation using Jetty as an http server.
 */
public class JettyStubHttpServer implements StubHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(JettyStubHttpServer.class);
    private final Server server;
    private final ServerConnector httpConnector;

    public JettyStubHttpServer() {
        this(0);
    }

    public JettyStubHttpServer(final int port) {
        this.server = new Server();
        
        this.httpConnector = new ServerConnector(server);
        this.httpConnector.setPort(port);
        server.addConnector(this.httpConnector);
    }

    /**
     * {@inheritDoc}
     */
    public void registerRequestManager(final RequestManager ruleProvider) {
        Validate.notNull(ruleProvider, "ruleProvider cannot be null");

        server.setHandler(new JadlerHandler(ruleProvider));
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws Exception {
        logger.debug("starting jetty");
        server.start();
        logger.debug("jetty started");
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws Exception {
        logger.debug("stopping jetty");
        server.stop();
        logger.debug("jetty stopped");
    }

    /**
     * {@inheritDoc}
     */
    public int getPort() {
        return httpConnector.getLocalPort();
    }
}