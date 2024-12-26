package com.wire.lithium.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.lithium.API;
import com.wire.lithium.server.monitoring.MDCUtils;
import com.wire.xenon.tools.Logger;

import okhttp3.OkHttpClient; // updated import
import okhttp3.Response; // updated import

public class Outbound extends HealthCheck {
    private final OkHttpClient client; // updated type

    public Outbound(OkHttpClient client) { // updated type
        this.client = client;
    }

    protected Result check() {
        MDCUtils.put("healthCheck", "Outbound"); // tag the logs with health check
        Logger.debug("Starting Outbound healthcheck");
        API api = new API(client, null);

        try {
            Response response = api.status(); // updated to work with OkHttpClient
            String s = response.body().string(); // updated to work with OkHttpClient
            int status = response.code(); // updated to work with OkHttpClient
            return status == 200 ? Result.healthy() : Result.unhealthy(String.format("%s. status: %d", s, status));
        } catch (Exception e) {
            final String message = String.format("Unable to reach: %s, error: %s", api.getWireHost(), e.getMessage());
            Logger.exception(message, e);
            return Result.unhealthy(message);
        } finally {
            Logger.debug("Finished Outbound healthcheck");
        }
    }
}