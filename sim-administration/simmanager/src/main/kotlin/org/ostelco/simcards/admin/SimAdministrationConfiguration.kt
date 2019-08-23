package org.ostelco.simcards.admin

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.jackson.Discoverable
import org.ostelco.dropwizardutils.OpenapiResourceAdderConfig
import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import javax.validation.Valid



data class SimAdministrationConfiguration(
        var database: DataSourceFactory = DataSourceFactory(),
        var httpClient: HttpClientConfiguration = HttpClientConfiguration(),
        var openApi: OpenapiResourceAdderConfig = OpenapiResourceAdderConfig(),
        var profileVendors: List<ProfileVendorConfig>,
        var hssAdapter: HssAdapterConfig? = null,
        @JsonProperty("hlrs") val hssVendors: List<HssConfig>,
        var phoneTypes: List<PhoneTypeConfig>
) : Configuration() {

    private val logger by getLogger()

    /* XXX Ideally the regex should be built when the config file is loaded,
       not when it is used. */

    /**
     * Get profile based on given phone type/getProfileForPhoneType.
     * @param name  phone type/getProfileForPhoneType
     * @return  profile metricName
     */
    fun getProfileForPhoneType(name: String): String? {
        val result = phoneTypes
                .firstOrNull {
                    name.matches(it.regex.toRegex(RegexOption.IGNORE_CASE))
                }
                ?.profile
        if (result == null) {
            logger.warn(NOTIFY_OPS_MARKER, "Could not allocate profile for phone type = '$name'.")
        }
        return result
    }
}

class HssAdapterConfig {

    @Valid
    @JsonProperty("hostname")
    lateinit var hostname: String

    @Valid
    @JsonProperty("port")
    var port: Int = 0
}


/**
 * Class used to input configuration data to the sim manager, that it
 * will use when communicating with HSS (Home Subscriber Service) entities
 * that keep track of authentication information used to authenticate
 * SIM profiles.
 */

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "hlrType")
sealed class HssConfig(
        /**
         * The metricName of the HSS used when referring to it in the sim manager's database.
         */
        open val name: String
) : Discoverable

/**
 * To differentiate between types of HSSes with potentially different
 * APIs.   The  current implementation types are "dummy" and "swt".
 */
@JsonTypeName("DUMMY")
data class DummyHssConfig(
        override val name: String
) : HssConfig(name = name)

@JsonTypeName("SWT")
data class SwtHssConfig(

        /**
         * The metricName of the HSS used when referring to it in the sim manager's database.
         */
        override val name: String,

        /**
         * The metricName of the hss used when contacting the HSS over the API.
         */
        val hssNameUsedInAPI: String,

        /**
         * An URL used to contact the HSS over
         */
        val endpoint: String,

        /**
         * UserId used to authenticate towards the API.
         */
        val userId: String,

        /**
         * API key (secret) used when authenticating towards the API.
         */
        val apiKey: String
) : HssConfig(name = name)

data class ProfileVendorConfig(
        val name: String,
        val es2plusEndpoint: String,
        val requesterIdentifier: String,
        val es9plusEndpoint: String
)

data class PhoneTypeConfig(
        val regex: String,
        val profile: String
)