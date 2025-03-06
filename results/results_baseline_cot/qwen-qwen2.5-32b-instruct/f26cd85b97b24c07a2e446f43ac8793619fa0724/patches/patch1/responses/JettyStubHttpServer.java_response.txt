package net.jadler.stubbing.server.jetty;

import net.jadler.RequestManager;
import net.jadler.stubbing.server.StubHttpServer;
import org.apache.commons.lang.Validate;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        httpConnector.setPort(port);
        server.addConnector(httpConnector);
    }

    public void registerRequestManager(final RequestManager ruleProvider) {
        Validate.notNull(ruleProvider, "ruleProvider cannot be null");

        server.setHandler(new JadlerHandler(ruleProvider));
    }

    public void start() throws Exception {
        logger.debug("starting jetty");
        server.start();
        logger.debug("jetty started");
    }

    public void stop() throws Exception {
        logger.debug("stopping jetty");
        server.stop();
        logger.debug("jetty stopped");
    }

    public int getPort() {
        return httpConnector.getLocalPort();
    }
}