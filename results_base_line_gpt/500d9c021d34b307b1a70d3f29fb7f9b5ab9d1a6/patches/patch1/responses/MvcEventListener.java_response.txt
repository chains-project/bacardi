package com.example;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.enterprise.event.Event; // Updated import
import javax.ws.rs.core.UriInfo; // Updated import
import javax.ws.rs.core.Response; // Updated import

@ApplicationScoped
public class MvcEventListener {

    @Inject
    Logger LOGGER;

    private void onControllerMatched(@Observes Object event) { // Updated parameter type
        UriInfo uriInfo = (UriInfo) event; // Cast to UriInfo
        LOGGER.info(() -> "Controller matched for " + uriInfo.getRequestUri());
    }

    private void onViewEngineSelected(@Observes Object event) { // Updated parameter type
        LOGGER.info(() -> "View engine: " + event);
    }

    private void onAfterControllerEvent(@Observes Object event) { // Updated parameter type
        LOGGER.info(() -> "AfterControllerEvent:: " + event);
    }

    private void onAfterProcessViewEvent(@Observes Object event) { // Updated parameter type
        LOGGER.info(() -> "AfterProcessViewEvent:: " + event);
    }
    
    private void onControllerRedirectEvent(@Observes Object event) { // Updated parameter type
        LOGGER.info(() -> "ControllerRedirectEvent:: " + event);
    }

    @PostConstruct
    private void init() {
        LOGGER.config(() -> this.getClass().getSimpleName() + " created");
    }
}