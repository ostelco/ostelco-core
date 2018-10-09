package org.ostelco.prime.imei.ImeiDb

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.PrimeModule


@JsonTypeName("Imei-lookup")
class ImeiLookupModule : PrimeModule {

    private val logger by getLogger()

    @JsonProperty
    var config: Config? = null

    override fun init(env: Environment) {

        logger.info("ImeiLookupModule env: $env")
        logger.info("CSV file set to ${config?.imeiLookupConfig?.csvFile}")
    }
}


class Config {
    @JsonProperty("sqlite")
    lateinit var imeiLookupConfig: ImeiLookupConfig
}

class ImeiLookupConfig {
    @JsonProperty
    var csvFile: String = "default.txt"
}