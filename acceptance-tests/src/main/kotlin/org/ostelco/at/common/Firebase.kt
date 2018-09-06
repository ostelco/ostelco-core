package org.ostelco.at.common

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

object Firebase {

    private fun setupFirebaseInstance(): FirebaseDatabase {

        try {
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            val databaseName = "pantel-2decb"
            val configFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS") ?: "config/pantel-prod.json"

            val credentials: GoogleCredentials = if (Files.exists(Paths.get(configFile))) {
                FileInputStream(configFile).use { serviceAccount -> GoogleCredentials.fromStream(serviceAccount) }
            } else {
                throw Exception()
            }

            val options = FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .setDatabaseUrl("https://$databaseName.firebaseio.com/")
                    .build()

            FirebaseApp.initializeApp(options)
        }

        return FirebaseDatabase.getInstance()
    }

    fun deleteAllPaymentCustomers() {
        setupFirebaseInstance().getReference("test/paymentId").removeValueAsync().get()
    }
}