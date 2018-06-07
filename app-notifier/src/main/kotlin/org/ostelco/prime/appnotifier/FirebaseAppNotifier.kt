package org.ostelco.prime.appnotifier

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures.addCallback
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class FirebaseAppNotifier: AppNotifier {

    override fun notify(msisdn: String) {
        println("Will try to notify msisdn : $msisdn")
        sendNotification(msisdn)
    }

    private fun sendNotification(msisdn: String) {
        // This registration token comes from the client FCM SDKs.
        val applicationToken = "THE_APP_TOKEN"

        // See documentation on defining a message payload.
        val message = Message.builder()
                .putData("score", "850")
                .putData("time", "2:45")
                .setToken(applicationToken)
                .build()

        // Send a message to the device corresponding to the provided
        // registration token.
        val future = FirebaseMessaging
                .getInstance(FirebaseApp.getInstance("fcm"))
                .sendAsync(message)

        val apiFutureCallback = object : ApiFutureCallback<String>  {
            override fun onSuccess(result: String) {
                println("Notification completed with result: $result")
            }

            override fun onFailure(t: Throwable) {
                println("Notification failed with error: $t")
            }
        }

        addCallback(future, apiFutureCallback)
    }
}