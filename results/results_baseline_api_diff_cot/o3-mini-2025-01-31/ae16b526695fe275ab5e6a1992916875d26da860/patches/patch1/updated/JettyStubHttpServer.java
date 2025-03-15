package net.jadler.stubbing.server.jetty;

import net.jadler.RequestManager;
import net.jadler.stubbing.server.StubHttpServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.Validate;

public class JettyStubHttpServer implements StubHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(JettyStubHttpServer.class);
    private final Server server;
    private final ServerConnector httpConnector;

    public JettyStubHttpServer() {
        this(0);
    }
    
    public JettyStubHttpServer(final int port) {
        this.server = new Server();
        
        // Setup HttpConfiguration: disabled server version and enabled date header.
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(true);
        
        // Create a ServerConnector with the given configuration.
        ServerConnector connector = new ServerConnector(this.server, new HttpConnectionFactory(httpConfig));
        connector.setPort(port);
        this.server.addConnector(connector);
        this.httpConnector = connector;
    }
    
    @Override
    public void registerRequestManager(final RequestManager ruleProvider) {
        Validate.notNull(ruleProvider, "ruleProvider cannot be null");
        server.setHandler(new JadlerHandler(ruleProvider));
    }
    
    @Override
    public void start() throws Exception {
        logger.debug("starting jetty");
        server.start();
        logger.debug("jetty started");
    }
    
    @Override
    public void stop() throws Exception {
        logger.debug("stopping jetty");
        server.stop();
        logger.debug("jetty stopped");
    }
    
    @Override
    public int getPort() {
        return httpConnector.getLocalPort();
    }
}