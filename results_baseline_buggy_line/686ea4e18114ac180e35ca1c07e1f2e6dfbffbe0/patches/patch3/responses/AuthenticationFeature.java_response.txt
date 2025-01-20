package com.wire.lithium.server.filters;

import jakarta.ws.rs.container.DynamicFeature; // Updated import
import jakarta.ws.rs.container.ResourceInfo; // Updated import
import jakarta.ws.rs.core.FeatureContext; // Updated import
import jakarta.ws.rs.ext.Provider; // Updated import
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // Updated import for Authorization

@Provider
public class AuthenticationFeature implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(SecurityRequirement.class) != null) { // Updated to use SecurityRequirement
            context.register(AuthenticationFilter.class);
        }
    }
}