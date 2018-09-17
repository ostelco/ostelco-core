package org.ostelco.prime.storage.firebase

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("firebase")
class FirebaseModule : PrimeModule {

    @JsonProperty("config")
    fun setConfig(config: FirebaseConfig) {
        FirebaseConfigRegistry.firebaseConfig = config
    }
}

object FirebaseConfigRegistry {
    lateinit var firebaseConfig: FirebaseConfig
}

data class FirebaseConfig(val configFile: String, val rootPath: String)