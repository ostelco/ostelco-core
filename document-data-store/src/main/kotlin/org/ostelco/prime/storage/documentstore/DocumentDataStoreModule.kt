package org.ostelco.prime.storage.documentstore

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("doc-data-store")
class DocumentDataStoreModule : PrimeModule {

    @JsonProperty("config")
    fun setConfig(config: Config) {
        ConfigRegistry.config = config
    }
}

object ConfigRegistry {
    var config: Config = Config()
}

data class Config(
        val storeType: String = "default",
        val namespace: String = "")