package org.ostelco.prime.client.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.cache.CacheBuilderSpec
import io.dropwizard.client.JerseyClientConfiguration
import javax.validation.Valid
import javax.validation.constraints.NotNull

class TopupConfiguration {

    /* TODO: Update to use the Kubernetes way of handling secrets (either stored
             in a file or as an ENV variable). */
    @Valid
    @NotNull
    @get:JsonProperty("secret")
    @set:JsonProperty("secret")
    lateinit var secret: String

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
