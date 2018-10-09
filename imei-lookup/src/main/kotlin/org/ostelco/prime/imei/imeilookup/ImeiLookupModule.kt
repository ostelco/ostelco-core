package org.ostelco.prime.imei.imeilookup

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

        val fileName = config?.csvFile  ?: ""
        logger.info("CSV file set to $fileName")
        ImeiDb.ImeiDdSingleton.loadFile(fileName);
    }
}

class Config {
    @JsonProperty
    var csvFile: String = "default.txt"
}