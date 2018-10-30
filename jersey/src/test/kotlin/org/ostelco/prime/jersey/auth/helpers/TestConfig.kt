package org.ostelco.prime.jersey.auth.helpers

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.cache.CacheBuilderSpec
import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import javax.validation.Valid
import javax.validation.constraints.NotNull

class TestConfig : Configuration() {
    @NotNull
    @Valid
    @get:JsonProperty("secret")
    @set:JsonProperty("secret")
    lateinit var secret: String

    @Valid
    @NotNull
    @get:JsonProperty("authenticationCachePolicy")
    lateinit var authenticationCachePolicy: CacheBuilderSpec
        private set

    @Valid
    @NotNull
    @get:JsonProperty("jerseyClient")
    val jerseyClientConfiguration = JerseyClientConfiguration()

    @JsonProperty("authenticationCachePolicy")
    fun setAuthenticationCachePolicy(spec: String) {
        this.authenticationCachePolicy = CacheBuilderSpec.parse(spec)
    }
}
