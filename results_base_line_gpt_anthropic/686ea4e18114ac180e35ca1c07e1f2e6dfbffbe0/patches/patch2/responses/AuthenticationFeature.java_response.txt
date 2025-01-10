package com.wire.lithium.server.filters;

import io.swagger.annotations.Authorization;

import jakarta.ws.rs.container.DynamicFeature; // Changed import from javax to jakarta
import jakarta.ws.rs.container.ResourceInfo; // Changed import from javax to jakarta
import jakarta.ws.rs.core.FeatureContext; // Changed import from javax to jakarta
import jakarta.ws.rs.ext.Provider; // Changed import from javax to jakarta

@Provider
public class AuthenticationFeature implements DynamicFeature {
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(Authorization.class) != null) {
            context.register(AuthenticationFilter.class);
        }
    }
}