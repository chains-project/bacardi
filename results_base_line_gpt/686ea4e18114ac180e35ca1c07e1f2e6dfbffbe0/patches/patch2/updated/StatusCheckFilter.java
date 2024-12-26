package com.wire.lithium.server.monitoring;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
// import ch.qos.logback.core.spi.FilterFactory; // Removed updated import

@JsonTypeName("status-filter-factory")
public class StatusCheckFilter extends Filter<IAccessEvent> {
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
}