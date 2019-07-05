package org.ostelco.prime.appnotifier

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.dropwizard.setup.Environment
import org.ostelco.common.firebasex.usingCredentialsFile
import org.ostelco.prime.module.PrimeModule
import java.io.IOException

@JsonTypeName("firebase-app-notifier")
class FirebaseModule : PrimeModule {

    @JsonProperty("config")
    private lateinit var config: FirebaseConfig

    override fun init(env: Environment) {
        try {
            val options = FirebaseOptions.Builder()
                    .usingCredentialsFile(config.configFile)
                    .build()
            try {
                FirebaseApp.getInstance("fcm")
            } catch (e: Exception) {
                FirebaseApp.initializeApp(options, "fcm")
            }
        } catch (ex: IOException) {
            throw AppNotifierException(ex)
        }
    }
}

data class FirebaseConfig(val configFile: String)