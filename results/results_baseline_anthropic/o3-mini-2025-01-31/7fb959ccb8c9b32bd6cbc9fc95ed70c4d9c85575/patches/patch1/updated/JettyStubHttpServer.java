/*
 * Copyright (c) 2012 - 2016 Jadler contributors
 * This program is made available under the terms of the MIT License.
 */
package net.jadler.stubbing.server.jetty;

import net.jadler.RequestManager;
import net.jadler.stubbing.server.StubHttpServer;
import org.apache.commons.lang.Validate;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
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
        
        // Create an HttpConfiguration and set the desired properties.
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(true);
        
        // Create a new ServerConnector with the HttpConfiguration.
        this.httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        this.httpConnector.setPort(port);
        server.addConnector(this.httpConnector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerRequestManager(final RequestManager ruleProvider) {
        Validate.notNull(ruleProvider, "ruleProvider cannot be null");
        server.setHandler(new JadlerHandler(ruleProvider));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws Exception {
        logger.debug("starting jetty");
        server.start();
        logger.debug("jetty started");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
        logger.debug("stopping jetty");
        server.stop();
        logger.debug("jetty stopped");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return httpConnector.getLocalPort();
    }
}