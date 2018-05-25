package org.ostelco.topup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilderSpec;
import io.dropwizard.client.JerseyClientConfiguration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class TopupConfiguration {

    /* TODO: Update to use the Kubernetes way of handling secrets (either stored
             in a file or as an ENV variable). */
    @Valid
    @NotNull
    private String secret;

    @Valid
    @NotNull
    private CacheBuilderSpec cacheSpec;

    @Valid
    @NotNull
    private JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();

    @JsonProperty("secret")
    public String getSecret() {
        return secret;
    }

    @JsonProperty("secret")
    public void setSecret(final String secret) {
        this.secret = secret;
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
