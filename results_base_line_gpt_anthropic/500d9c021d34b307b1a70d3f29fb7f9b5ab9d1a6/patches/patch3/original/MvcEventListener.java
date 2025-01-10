package com.example;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import jakarta.mvc.Viewable; // Update to use jakarta package
import jakarta.mvc.event.AfterControllerEvent; // Update to use jakarta package
import jakarta.mvc.event.AfterProcessViewEvent; // Update to use jakarta package
import jakarta.mvc.event.BeforeControllerEvent; // Update to use jakarta package
import jakarta.mvc.event.BeforeProcessViewEvent; // Update to use jakarta package
import jakarta.mvc.event.ControllerRedirectEvent; // Update to use jakarta package

@ApplicationScoped
public class MvcEventListener {

    @Inject
    Logger LOGGER;

    private void onControllerMatched(@Observes BeforeControllerEvent event) {
        LOGGER.info(() -> "Controller matched for " + event.getUriInfo().getRequestUri());
    }

    private void onViewEngineSelected(@Observes BeforeProcessViewEvent event) {
        LOGGER.info(() -> "View engine: " + event.getEngine());
    }

    private void onAfterControllerEvent(@Observes AfterControllerEvent event) {
        LOGGER.info(() -> "AfterControllerEvent:: " + event.getResourceInfo());
    }

    private void onAfterProcessViewEvent(@Observes AfterProcessViewEvent event) {
        LOGGER.info(() -> "AfterProcessViewEvent:: " + event);
    }
    
    private void onControllerRedirectEvent(@Observes ControllerRedirectEvent event) {
        LOGGER.info(() -> "ControllerRedirectEvent:: " + event);
    }

    @PostConstruct
    private void init() {
        LOGGER.config(() -> this.getClass().getSimpleName() + " created");
    }
}