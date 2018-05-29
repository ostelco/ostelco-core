package org.ostelco.prime.client.api.auth.helpers

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
    var secret: String? = null

    @Valid
    @NotNull
    @get:JsonProperty("authenticationCachePolicy")
    var authenticationCachePolicy: CacheBuilderSpec? = null
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
