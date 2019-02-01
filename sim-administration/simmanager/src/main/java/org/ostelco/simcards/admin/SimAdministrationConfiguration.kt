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

    @Valid
    @NotNull
    @JsonProperty("phoneTypes")
    lateinit var phoneTypes: List<PhoneTypeConfig>

    /* XXX Ideally the regex should be buildt when the config file is loaded,
           not when it is used. */

    /**
     * Get profile based on given phone type/getProfileForPhoneType.
     * @param name  phone type/getProfileForPhoneType
     * @return  profile name
     */
    fun getProfileForPhoneType(name: String) = phoneTypes.filter { name.matches(it.regex.toRegex(RegexOption.IGNORE_CASE)) }
            .map { it.profile }
            .first()
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

class PhoneTypeConfig {
    @Valid
    @NotNull
    @JsonProperty("regex")
    lateinit var regex: String

    @Valid
    @NotNull
    @JsonProperty("profile")
    lateinit var profile: String
}