package org.ostelco.topup.api.auth.helpers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilderSpec;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class TestConfig extends Configuration {
    @NotNull
    @Valid
    private String secret;

    @Valid
    @NotNull
    private CacheBuilderSpec cacheSpec;

    @Valid
    @NotNull
    private JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();

    @JsonProperty("secret")
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    @JsonProperty("secret")
    public String getSecret() {
        return secret;
    }

    @JsonProperty("authenticationCachePolicy")
    public void setAuthenticationCachePolicy(final String spec) {
        this.cacheSpec = CacheBuilderSpec.parse(spec);
    }

    @JsonProperty("authenticationCachePolicy")
    public CacheBuilderSpec getAuthenticationCachePolicy() {
        return cacheSpec;
    }

    @JsonProperty("jerseyClient")
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return jerseyClient;
    }
}
