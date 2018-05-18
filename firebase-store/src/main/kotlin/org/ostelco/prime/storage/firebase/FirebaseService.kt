package org.ostelco.prime.storage.firebase

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.provider.Service

@JsonTypeName("firebase")
class FirebaseService : Service {

    @JsonProperty("config")
    fun setConfig(config: FirebaseConfig) {
        FirebaseConfigRegistry.firebaseConfig = config
    }
}

object FirebaseConfigRegistry {
    lateinit var firebaseConfig: FirebaseConfig
}

class FirebaseConfig {

    @NotEmpty
    @JsonProperty("databaseName")
    lateinit var databaseName: String

    @NotEmpty
    @JsonProperty("configFile")
    lateinit var configFile: String
}