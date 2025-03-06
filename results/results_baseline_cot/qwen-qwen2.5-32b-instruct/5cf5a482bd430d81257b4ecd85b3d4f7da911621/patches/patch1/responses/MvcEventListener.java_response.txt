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

    @PostConstruct
    private void init() {
        LOGGER.config(() -> this.getClass().getSimpleName() + " created");
    }
}