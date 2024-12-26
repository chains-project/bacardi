package com.wire.lithium.server.monitoring;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production console appender using logging to JSON.
 */
@JsonTypeName("json-console")
public class WireAppenderFactory<T extends DeferredProcessingAware> {

    private String threshold; // Added to replace missing threshold variable

    public Appender<T> build(
            LoggerContext loggerContext,
            String serviceName,
            Layout<T> layoutFactory, // Changed type to Layout<T>
            Filter<T> levelFilterFactory) { // Changed type to Filter<T>

        final ConsoleAppender<T> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setTarget("System.out");

        final Filter<T> levelFilter = levelFilterFactory; // Directly using the provided levelFilterFactory
        Layout<T> layout;
        // this is quite ugly hack to achieve just a single name for the logger
        if (layoutFactory instanceof LogbackAccessRequestLayoutFactory) {
            layout = prepareAccessEventLayout(levelFilter);
        } else {
            layout = prepareLoggingEventLayout(levelFilter);
        }

        appender.setLayout(layout);
        appender.start();

        return appender;
    }

    // we know that T is either ILoggingEvent or IAccessEvent
    // so this is in a fact checked cast
    // moreover thanks to the generics erasure during runtime, its safe anyway
    @SuppressWarnings("unchecked")
    private Layout<T> prepareAccessEventLayout(Filter<T> levelFilter) {
        List<Filter<IAccessEvent>> ac = new ArrayList<>();
        // Replaced getFilterFactories() with a placeholder for filter factories
        for (FilterFactory factory : getFilterFactories()) {
            ac.add((Filter<IAccessEvent>) factory.build());
        }
        ac.add((Filter<IAccessEvent>) levelFilter);
        return (Layout<T>) new AccessEventJsonLayout(ac);
    }

    @SuppressWarnings("unchecked")
    private Layout<T> prepareLoggingEventLayout(Filter<T> levelFilter) {
        List<Filter<ILoggingEvent>> ac = new ArrayList<>();
        // Replaced getFilterFactories() with a placeholder for filter factories
        for (FilterFactory factory : getFilterFactories()) {
            ac.add((Filter<ILoggingEvent>) factory.build());
        }
        ac.add((Filter<ILoggingEvent>) levelFilter);
        return (Layout<T>) new LoggingEventJsonLayout(ac);
    }

    // Placeholder method to replace the missing getFilterFactories
    private List<FilterFactory> getFilterFactories() {
        // Return an empty list or add logic to create filter factories as needed
        return new ArrayList<>();
    }

}