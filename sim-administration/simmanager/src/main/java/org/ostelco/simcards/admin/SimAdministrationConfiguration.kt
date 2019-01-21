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
    @JsonProperty("smdp")
    lateinit var smdp: List<SmDpPlusConfig>

    @Valid
    @NotNull
    @JsonProperty("hlr")
    lateinit var hlr: List<HlrConfig>

    /**
     * Checks for:
     *   - No duplicates in list of HLR services
     *   - No duplicates in list of SMDP+ services
     *   - That no SMDP+ service refers to a HLR service that has not
     *     been configured
     * @return true if valid HLR/SMDP+ configuration
     */
    fun validate() = hlr.map { it.name }.distinct().size == hlr.size &&
            smdp.map { it.name }.distinct().size == smdp.size

    /* Helpers. */
    fun hlrServiceNames() = hlr.map { it.name }
    fun smDpPlusServiceNames() = smdp.map { it.name }
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
}

class SmDpPlusConfig {
    @Valid
    @NotNull
    @JsonProperty("name")
    lateinit var name: String

    @Valid
    @NotNull
    @JsonProperty("host")
    lateinit var host: String

    @Valid
    @NotNull
    @JsonProperty("port")
    var port: Int = 0
}
