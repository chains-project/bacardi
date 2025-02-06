package com.example;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.mvc.event.AfterControllerEvent;
import javax.mvc.event.AfterProcessViewEvent;
import javax.mvc.event.BeforeControllerEvent;
import javax.mvc.event.BeforeProcessViewEvent;
import javax.mvc.event.ControllerRedirectEvent;

@ApplicationScoped
public class MvcEventListener {

    @Inject
    Logger LOGGER;

    private void onControllerMatched(@Observes Object event) {
        LOGGER.info(() -> "Controller matched for " + event);
    }

    private void onViewEngineSelected(@Observes Object event) {
        LOGGER.info(() -> "View engine: " + event);
    }

    private void onAfterControllerEvent(@Observes Object event) {
        LOGGER.info(() -> "AfterControllerEvent:: " + event);
    }

    private void onAfterProcessViewEvent(@Observes Object event) {
        LOGGER.info(() -> "AfterProcessViewEvent:: " + event);
    }
    
    private void onControllerRedirectEvent(@Observes Object event) {
        LOGGER.info(() -> "ControllerRedirectEvent:: " + event);
    }

    @PostConstruct
    private void init() {
        LOGGER.config(() -> this.getClass().getSimpleName() + " created");
    }
}