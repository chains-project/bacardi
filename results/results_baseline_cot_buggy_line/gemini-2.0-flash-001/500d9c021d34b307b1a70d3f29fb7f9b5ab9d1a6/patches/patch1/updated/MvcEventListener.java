package com.example;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;


@ApplicationScoped
public class MvcEventListener {

    @Inject
    Logger LOGGER;

    private void onControllerMatched(@Observes jakarta.mvc.event.BeforeControllerEvent event) {
        LOGGER.info(() -> "Controller matched for " + event.getUriInfo().getRequestUri());
    }

    private void onViewEngineSelected(@Observes jakarta.mvc.event.BeforeProcessViewEvent event) {
        LOGGER.info(() -> "View engine: " + event.getEngine());
    }

    private void onAfterControllerEvent(@Observes jakarta.mvc.event.AfterControllerEvent event) {
        LOGGER.info(() -> "AfterControllerEvent:: " + event.getResourceInfo());
    }

    private void onAfterProcessViewEvent(@Observes jakarta.mvc.event.AfterProcessViewEvent event) {
        LOGGER.info(() -> "AfterProcessViewEvent:: " + event);
    }
    
     private void onControllerRedirectEvent(@Observes jakarta.mvc.event.ControllerRedirectEvent event) {
        LOGGER.info(() -> "ControllerRedirectEvent:: " + event);
    }

    @PostConstruct
    private void init() {
        LOGGER.config(() -> this.getClass().getSimpleName() + " created");
    }
}