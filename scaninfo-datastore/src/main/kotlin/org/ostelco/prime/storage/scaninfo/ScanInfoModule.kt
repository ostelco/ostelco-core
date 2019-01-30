package org.ostelco.prime.storage.scaninfo

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("scaninfo-store")
class ScanInfoModule : PrimeModule {
    private val logger by getLogger()

    @JsonProperty
    fun setConfig(config: ScanInfoConfig) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        logger.info("Initializing ScanInfoModule")
        ScanInformationStoreSingleton.init(env, EnvironmentVars())
    }
}

/**
 * The configuration for Scan Information Datastore module.
 */
class ScanInfoConfig : Configuration() {
    var storeType = "default"
}

object ConfigRegistry {
    var config = ScanInfoConfig()
}