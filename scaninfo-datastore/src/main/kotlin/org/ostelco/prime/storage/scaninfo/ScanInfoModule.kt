package org.ostelco.prime.storage.scaninfo

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("scaninfo-store")
class ScanInfoModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: ScanInfoConfig) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        ScanInformationStoreSingleton.init(EnvironmentVars())
    }
}

/**
 * The configuration for Scan Information Cloud Storage module.
 */
data class ScanInfoConfig(
    val storeType: String = "default",
    val namespace: String = ""
) : Configuration()

object ConfigRegistry {
    var config = ScanInfoConfig()
}