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
        ScanInformationStoreSingleton.init(env, EnvironmentVars())
    }
}

/**
 * The configuration for Scan Information Cloud Storage module.
 */
class ScanInfoConfig : Configuration() {
    var storeType = "default"
    var keysetFilePathPrefix = "encrypt_key"
    var deleteScan = false // Flag for deleting the scan data from the backend
    var deleteUrl = "https://netverify.com/api/netverify/v2/scans/" // URL prefix for delete API for backend
}

object ConfigRegistry {
    var config = ScanInfoConfig()
}