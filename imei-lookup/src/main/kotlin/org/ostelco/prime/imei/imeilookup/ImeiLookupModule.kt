package org.ostelco.prime.imei.imeilookup

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule


@JsonTypeName("Imei-lookup")
class ImeiLookupModule : PrimeModule {

    @JsonProperty
    lateinit var config: Config

    override fun init(env: Environment) {
        ImeiDdSingleton.loadFile(config.csvFile)
    }
}

data class Config(val csvFile: String)