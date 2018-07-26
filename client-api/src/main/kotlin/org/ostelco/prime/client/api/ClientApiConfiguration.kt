package org.ostelco.prime.client.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.cache.CacheBuilderSpec
import io.dropwizard.client.JerseyClientConfiguration
import javax.validation.Valid
import javax.validation.constraints.NotNull

class ClientApiConfiguration {

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

    @NotNull
    var pseudonymEndpoint: String? = null
        // TODO make @NotBlank or @NotEmpty work again
        set(value) {
            if (value == null || value.isBlank()) {
                throw Error("modules.type['api'].config.pseudonymEndpoint is blank")
            }
            field = value
        }
}
