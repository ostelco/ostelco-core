package org.ostelco.prime.storage.scaninfo

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("scaninfo-datastore")
class ScanInfoModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: ScanInfoConfig) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        println("Initializing ScanInfoModule")
        ScanInformationStoreSingleton.init(env)
        println("Done Initializing ScanInfoModule")

    }
}

/**
 * The configuration for Scan Information Datastore module.
 */
class ScanInfoConfig : Configuration() {
    var datastoreType = "default"
    var namespace = ""
    var apiToken = ""
    var apiSecret = ""
}

object ConfigRegistry {
    var config = ScanInfoConfig()
}