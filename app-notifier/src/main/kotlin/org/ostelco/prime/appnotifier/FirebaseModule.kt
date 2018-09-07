package org.ostelco.prime.appnotifier

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.module.PrimeModule
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@JsonTypeName("firebase-app-notifier")
class FirebaseModule : PrimeModule {

    @JsonProperty("config")
    fun setConfig(config: FirebaseConfig) {
        setupFirebaseApp(config.databaseName, config.configFile)
    }

    private fun setupFirebaseApp(
            databaseName: String,
            configFile: String) {

        try {
            val credentials: GoogleCredentials = if (Files.exists(Paths.get(configFile))) {
                FileInputStream(configFile).use { serviceAccount -> GoogleCredentials.fromStream(serviceAccount) }
            } else {
                GoogleCredentials.getApplicationDefault()
            }

            val options = FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .setDatabaseUrl("https://$databaseName.firebaseio.com/")
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

class FirebaseConfig {

    @NotEmpty
    @JsonProperty("databaseName")
    lateinit var databaseName: String

    @NotEmpty
    @JsonProperty("configFile")
    lateinit var configFile: String
}