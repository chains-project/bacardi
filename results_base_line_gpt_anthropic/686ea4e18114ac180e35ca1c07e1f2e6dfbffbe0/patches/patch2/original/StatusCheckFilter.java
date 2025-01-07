package com.wire.lithium.server.monitoring;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
// Update the import to the correct location of FilterFactory based on the newer version
import io.dropwizard.logging.filter.FilterFactory;

// Update the class to implement the correct interface
@JsonTypeName("status-filter-factory")
public class StatusCheckFilter implements Filter<IAccessEvent> {
    @Override
    public Filter<IAccessEvent> build() {
        return new Filter<>() {
            @Override
            public FilterReply decide(IAccessEvent event) {
                if (event.getRequestURI().contains("/status")) {
                    return FilterReply.DENY;
                }
                if (event.getRequestURI().contains("/swagger")) {
                    return FilterReply.DENY;
                }
                if (event.getRequestURI().contains("/metrics")) {
                    return FilterReply.DENY;
                }
                return FilterReply.NEUTRAL;
            }
        };
    }
}