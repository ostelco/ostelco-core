package org.ostelco.prime.storage.scaninfo

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("scaninfo-datastore")
class ScanInfoModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: Config) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
    }
}

/**
 * The configuration for Scan Information Datastore module.
 */
class Config : Configuration() {
    var datastoreType = "default"
    var namespace = ""
}

object ConfigRegistry {
    lateinit var config: Config
}