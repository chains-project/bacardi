package com.wire.lithium.server.filters;

import jakarta.ws.rs.container.DynamicFeature; // Updated import
import jakarta.ws.rs.container.ResourceInfo; // Updated import
import jakarta.ws.rs.core.FeatureContext; // Updated import
import jakarta.ws.rs.ext.Provider; // Updated import
import io.swagger.annotations.Authorization;

@Provider
public class AuthenticationFeature implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(Authorization.class) != null) {
            context.register(AuthenticationFilter.class);
        }
    }
}