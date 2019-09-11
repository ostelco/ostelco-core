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


/**
 * Configuration for profile vendors.  The name is a name alphanumeric + undrescore)
 * the es2plus endpoint is an fqdn, with an optional portnumber.  Similarly for the es9plus
 * endpoint.  The requester identifier is a string that is intended to identify the requester,
 * obviously in addition to client certificate.
 */
class ProfileVendorConfig(
        val name: String,
        private val es2plusEndpoint: String,
        val requesterIdentifier: String,
        val es9plusEndpoint: String
) {

    private val logger by getLogger()

    private var safeEndpoint : String = es2plusEndpoint

    companion object {
        val ALPHANUMERIC = Regex("[a-zA-Z0-9_]+")
        val ENDPOINT = Regex("^https?://[\\.\\-_a-zA-Z0-9_]+(:[0-9]+)?")
    }

    init {

        // Reduce the safe endpoint to be the start of the substring matching the
        // endpoint syntax.
        val match = ENDPOINT.find(es2plusEndpoint)
        if (match != null) {
            safeEndpoint = es2plusEndpoint.substring(match.range)
            if (safeEndpoint != es2plusEndpoint) {
                logger.error("Trunkcating ex2plusEndpoint from '$es2plusEndpoint' to '$safeEndpoint'. Beware!")
            }
        } else {
            val msg = "Illegal es2plusendpoint field in config: $es2plusEndpoint"
            logger.error(msg)
            throw RuntimeException(msg)
        }
        validate()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProfileVendorConfig) return false
        if (this.name  != other.name) return false
        if (this.getEndpoint() != other.getEndpoint()) return false
        if (this.es9plusEndpoint != other.es9plusEndpoint) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    public fun getEndpoint() = safeEndpoint


    // If the instance does not contain valid fields, then cry foul, but don't break.
    // too many things break!
    fun validate() {

        if (!name.matches(ALPHANUMERIC)) {
            logger.warn(NOTIFY_OPS_MARKER, "Profile vendor name '$name' does not match regex ${ALPHANUMERIC.pattern}")
        }

        if (!getEndpoint().matches(ENDPOINT)) {
            logger.warn(NOTIFY_OPS_MARKER, "es2plusEndpoint '${getEndpoint()}' does not match regex ${ENDPOINT.pattern}")
        }

        if (!es9plusEndpoint.matches(ENDPOINT)) {
            logger.warn(NOTIFY_OPS_MARKER, "es9plusEndpoint '$es9plusEndpoint' does not match regex ${ENDPOINT.pattern}")
        }

        if (!requesterIdentifier.matches(ALPHANUMERIC)) {
            logger.warn(NOTIFY_OPS_MARKER, "requesterIdentifier '$es9plusEndpoint' does not match regex ${ALPHANUMERIC.pattern}")
        }
    }
}

data class PhoneTypeConfig(
        val regex: String,
        val profile: String
)