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
        val database: DataSourceFactory = DataSourceFactory(),
        val httpClient: HttpClientConfiguration = HttpClientConfiguration(),
        val openApi: OpenapiResourceAdderConfig = OpenapiResourceAdderConfig(),
        val profileVendors: List<ProfileVendorConfig>,
        var hssAdapter: HssAdapterConfig? = null,
        @JsonProperty("hlrs") val hssVendors: List<HssConfig>,
        val phoneTypes: List<PhoneTypeConfig>
) : Configuration() {

    init {
        ProfileVendorConfig.validateConfigList(profileVendors)
    }
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


class ProfileVendorConfigException(msg: String): Exception(msg)

/**
 * Configuration for profile vendors.  The name is a name alphanumeric + undrescore)
 * the es2plus endpoint is an fqdn, with an optional portnumber.  Similarly for the es9plus
 * endpoint.  The requester identifier is a string that is intended to identify the requester,
 * obviously in addition to client certificate.
 */
data class ProfileVendorConfig(
        val name: String,
        val es2plusEndpoint: String,
        val requesterIdentifier: String,
        val es9plusEndpoint: String
) {
    companion object {
        val ALPHANUMERIC_REGEX = Regex("[a-zA-Z0-9_]+")
        val ENDPOINT_REGEX = Regex("[\\.-a-zA-Z0-9_]+(:[0-9]+)?")

        // Do remember to call this function, it's easy to mess up the configuration
        fun validateConfigList(profileVendors: List<ProfileVendorConfig>) {
                profileVendors.forEach { it.validate() }
        }
    }


    fun validate() {
        if (!name.matches(ALPHANUMERIC_REGEX)) {
            throw  ProfileVendorConfigException("Profile vendor name '$name' does not match regex ${ALPHANUMERIC_REGEX.pattern}")
        }

        if (!es2plusEndpoint.matches(ENDPOINT_REGEX)) {
            throw  ProfileVendorConfigException("es2plusEndpoint '$es2plusEndpoint' does not match regex ${ENDPOINT_REGEX.pattern}")
        }

        if (!es9plusEndpoint.matches(ENDPOINT_REGEX)) {
            throw  ProfileVendorConfigException("es9plusEndpoint '$es9plusEndpoint' does not match regex ${ENDPOINT_REGEX.pattern}")
        }

        if (!requesterIdentifier.matches(ALPHANUMERIC_REGEX)) {
            throw  ProfileVendorConfigException("requesterIdentifier '$es9plusEndpoint' does not match regex ${ALPHANUMERIC_REGEX.pattern}")
        }
    }
}

data class PhoneTypeConfig(
        val regex: String,
        val profile: String
)