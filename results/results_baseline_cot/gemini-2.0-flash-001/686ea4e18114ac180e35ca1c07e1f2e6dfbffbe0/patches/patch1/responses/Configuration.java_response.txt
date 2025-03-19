package com.wire.lithium;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.dropwizard.core.Configuration as BaseConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Application configuration class. Extend this class to add your custom configuration
 */
public class Configuration extends BaseConfiguration {
    @JsonProperty
    @Valid
    public Database database = new Database();

    @JsonProperty
    @NotNull
    public String token;   // Service token. Obtained when the Service is registered with Wire

    @JsonProperty
    public boolean healthchecks = true;

    @Valid
    private _JerseyClientConfiguration jerseyClient = new _JerseyClientConfiguration();

    @JsonProperty("swagger")
    public SwaggerBundleConfiguration swagger = new _SwaggerBundleConfiguration();

    @JsonProperty
    public String apiHost = "https://prod-nginz-https.wire.com";

    @JsonProperty("jerseyClient")
    public JerseyClientConfiguration getJerseyClient() {
        return jerseyClient;
    }

    @JsonProperty("jerseyClient")
    public void setJerseyClient(_JerseyClientConfiguration jerseyClient) {
        this.jerseyClient = jerseyClient;
    }

    public static class Database extends DataSourceFactory {
        @JsonProperty
        public boolean baseline;
    }

    public static class _JerseyClientConfiguration extends JerseyClientConfiguration {
        public _JerseyClientConfiguration() {
            setChunkedEncodingEnabled(false);
            setGzipEnabled(false);
            setGzipEnabledForRequests(false);
        }
    }

    private static class _SwaggerBundleConfiguration extends SwaggerBundleConfiguration {
        _SwaggerBundleConfiguration() {
            setResourcePackage("com.wire.lithium.server.resources");
        }
    }
}