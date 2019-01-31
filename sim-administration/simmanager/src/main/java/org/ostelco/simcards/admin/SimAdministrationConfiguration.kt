package org.ostelco.simcards.admin

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import io.dropwizard.db.DataSourceFactory
import org.ostelco.dropwizardutils.OpenapiResourceAdderConfig
import javax.validation.Valid
import javax.validation.constraints.NotNull


class SimAdministrationConfiguration : Configuration() {
    @Valid
    @NotNull
    @JsonProperty("database")
    val database: DataSourceFactory = DataSourceFactory()

    @Valid
    @NotNull
    @JsonProperty
    private val httpClient = JerseyClientConfiguration()

    fun getJerseyClientConfiguration(): JerseyClientConfiguration {
        return httpClient
    }

    @Valid
    @NotNull
    @JsonProperty("openApi")
    val openApi = OpenapiResourceAdderConfig()

    @Valid
    @NotNull
    @JsonProperty("profileVendors")
    lateinit var profileVendors: List<ProfileVendorConfig>

    @Valid
    @NotNull
    @JsonProperty("hlrs")
    lateinit var hlrVendors: List<HlrConfig>
}

class HlrConfig {
    @Valid
    @NotNull
    @JsonProperty("name")
    lateinit var name: String

    @Valid
    @NotNull
    @JsonProperty("url")
    lateinit var url: String

    @Valid
    @NotNull
    @JsonProperty("userId")
    lateinit var userId: String

    @Valid
    @NotNull
    @JsonProperty("apiKey")
    lateinit var apiKey: String
}

class ProfileVendorConfig {
    @Valid
    @NotNull
    @JsonProperty("name")
    lateinit var name: String

    @Valid
    @NotNull
    @JsonProperty("url")
    lateinit var url: String
}
