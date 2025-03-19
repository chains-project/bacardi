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
import io.dropwizard.request.logging.layout.LogbackAccessRequestLayoutFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JsonTypeName("json-console")
public class WireAppenderFactory<T extends DeferredProcessingAware> {

    private String threshold = "INFO";

    public Appender<T> build(
            LoggerContext loggerContext,
            String serviceName,
            LayoutFactory<T> layoutFactory,
            LevelFilterFactory<T> levelFilterFactory,
            AsyncAppenderFactory<T> asyncAppenderFactory) {

        final ConsoleAppender<T> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setTarget("System.out");

        final Filter<T> levelFilter = levelFilterFactory.build(threshold);
        Layout<T> layout;
        if (layoutFactory instanceof LogbackAccessRequestLayoutFactory) {
            layout = prepareAccessEventLayout(levelFilter);
        } else {
            layout = prepareLoggingEventLayout(levelFilter);
        }

        appender.setLayout(layout);
        appender.start();

        return appender;
    }

    protected List<FilterFactory<T>> getFilterFactories() {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Layout<T> prepareAccessEventLayout(Filter<T> levelFilter) {
        List<Filter<IAccessEvent>> ac = getFilterFactories().stream()
                .map(filter -> (Filter<IAccessEvent>) filter.build())
                .collect(Collectors.toList());
        ac.add((Filter<IAccessEvent>) levelFilter);
        return (Layout<T>) new AccessEventJsonLayout(ac);
    }

    @SuppressWarnings("unchecked")
    private Layout<T> prepareLoggingEventLayout(Filter<T> levelFilter) {
        List<Filter<ILoggingEvent>> ac = getFilterFactories().stream()
                .map(filter -> (Filter<ILoggingEvent>) filter.build())
                .collect(Collectors.toList());
        ac.add((Filter<ILoggingEvent>) levelFilter);
        return (Layout<T>) new LoggingEventJsonLayout(ac);
    }

    public interface LayoutFactory<T> {
    }

    public interface LevelFilterFactory<T> {
        Filter<T> build(String threshold);
    }

    public interface AsyncAppenderFactory<T> {
    }

    public interface FilterFactory<T> {
        Filter<T> build();
    }
}