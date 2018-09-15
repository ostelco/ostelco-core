package org.ostelco.at.common

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import org.ostelco.common.firebasex.usingCredentialsFile

object Firebase {

    private fun setupFirebaseInstance(): FirebaseDatabase {

        try {
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            val configFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS") ?: "config/pantel-prod.json"

            val options = FirebaseOptions.Builder()
                    .usingCredentialsFile(configFile)
                    .build()

            FirebaseApp.initializeApp(options)
        }

        return FirebaseDatabase.getInstance()
    }

    fun deleteAllPaymentCustomers() {
        setupFirebaseInstance().getReference("test/paymentId").removeValueAsync().get()
    }
}