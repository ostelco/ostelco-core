package org.ostelco.simcards.admin

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientConfiguration
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
    val httpClient = HttpClientConfiguration()

    @Valid
    @NotNull
    @JsonProperty("openApi")
    val openApi = OpenapiResourceAdderConfig()

    @Valid
    @NotNull
    @JsonProperty("profileVendors")
    lateinit var profileVendors: List<ProfileVendorConfig>


    @Valid
    @JsonProperty("hssAdapter")
    var hssAdapter: HssAdapterConfig? = null

    // XXX Make this optional once the hssAdapter mechanism
    //     has been made operational and stable.
    @Valid
    @NotNull
    @JsonProperty("hlrs")
    lateinit var hssVendors: List<HssConfig>

    @Valid
    @NotNull
    @JsonProperty("phoneTypes")
    lateinit var phoneTypes: List<PhoneTypeConfig>

    /* XXX Ideally the regex should be built when the config file is loaded,
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



class HssAdapterConfig {

    @Valid
    @JsonProperty("hostname")
    lateinit var hostname: String

    @Valid
    @JsonProperty("port")
    var port: Int = 0
}


enum class HSSType {
    // A type of HSS that is incapable of doing any actions (they will all fail),
    // but _is_ capable of being referred to in the database schema.  Useful for
    // HSSes that we don't  have API access to, and for which we get pre-activated
    // profiles.
    DUMMY,

    // The old "Software Telco" HSS interface.
    SWT
}

/**
 * Class used to input configuration data to the sim manager, that it
 * will use when communicating with HSS (Home Subscriber Service) entities
 * that keep track of authentication information used to authenticate
 * SIM profiles.
 */
class HssConfig {

    /**
     * To differentiate between types of HSSes with potentially different
     * APIs.   The  current implementation types are "DUMMY" and "SWT".
     */
    @Valid
    @NotNull
    @JsonProperty("hlrType")
    lateinit var type: HSSType

    /**
     * The name of the HSS used when referring to it in the sim manager's database.
     */
    @Valid
    @NotNull
    @JsonProperty("name")
    lateinit var name: String


    //
    //   Parameters that only make sense if the type is "SWT"
    //
    /**
     * The name of the hss used when contacting the HSS over the API.
     */
    @Valid
    // @NotNull
    @JsonProperty("hssNameUsedInAPI")
    lateinit var hssNameUsedInAPI: String

    /**
     * An URL used to contact the HSS over
     */
    @Valid
    // @NotNull
    @JsonProperty("endpoint")
    lateinit var endpoint: String

    /**
     * Userid used to authenticate towards the API.
     */
    @Valid
    // @NotNull
    @JsonProperty("userId")
    lateinit var userId: String

    /**
     * API key (secret) used when authenticating towards the API.
     */
    @Valid
    // @NotNull
    @JsonProperty("apiKey")
    lateinit var apiKey: String


    private fun validateAsWg2Config() {
        check (::hssNameUsedInAPI.isInitialized) { "hssNameUsedInAPI must be non null"}
        check (::endpoint.isInitialized) {"endpoint must be non null"}
        check (::userId.isInitialized) { "userId must be non null"}
        check (::apiKey.isInitialized) { "apiKey must be non null"}
    }

    private fun validateAsDummyConfig() {}

    fun validate() {
        return when(type) {
            HSSType.SWT -> validateAsWg2Config()
            HSSType.DUMMY -> validateAsDummyConfig()
        }
    }
}

class ProfileVendorConfig {
    @Valid
    @NotNull
    @JsonProperty("name")
    lateinit var name: String

    @Valid
    @NotNull
    @JsonProperty("es2plusEndpoint")
    lateinit var es2plusEndpoint: String

    @Valid
    @NotNull
    @JsonProperty("requesterIdentifier")
    lateinit var requesterIndentifier: String

    @Valid
    @NotNull
    @JsonProperty("es9plusEndpoint")
    lateinit var es9plusEndpoint: String
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