package org.ostelco.prime.appnotifier

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.ostelco.common.firebasex.usingCredentialsFile
import org.ostelco.prime.module.PrimeModule
import java.io.IOException

@JsonTypeName("firebase-app-notifier")
class FirebaseModule : PrimeModule {

    @JsonProperty("config")
    fun setConfig(config: FirebaseConfig) {
        setupFirebaseApp(config.configFile)
    }

    private fun setupFirebaseApp(configFile: String) {

        try {
            val options = FirebaseOptions.Builder()
                    .usingCredentialsFile(configFile)
                    .build()
            try {
                FirebaseApp.getInstance("fcm")
            } catch (e: Exception) {
                FirebaseApp.initializeApp(options, "fcm")
            }

            // (un)comment next line to turn on/of extended debugging
            // from firebase.
            // this.firebaseDatabase.setLogLevel(com.google.firebase.database.Logger.Level.DEBUG);
        } catch (ex: IOException) {
            throw AppNotifierException(ex)
        }
    }
}

data class FirebaseConfig(val configFile: String)