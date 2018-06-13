package org.ostelco.prime.appnotifier

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures.addCallback
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.legacy.Storage
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class FirebaseAppNotifier: AppNotifier {

    override fun notify(msisdn: String, title: String, body: String) {
        println("Will try to notify msisdn : $msisdn")
        sendNotification(msisdn, title, body)
    }

    private fun sendNotification(msisdn: String, title: String, body: String) {

        val store = getResource<Storage>()

        // This registration token comes from the client FCM SDKs.
        val applicationTokens = store.getNotificationTokens(msisdn)

        for (applicationToken in applicationTokens) {

            // ToDo : Currently we asume that all tokens are for FCM

            // See documentation on defining a message payload.
            val message = Message.builder()
                    .setNotification(Notification(title, body))
                    .setToken(applicationToken.token)
                    .build()

            // Send a message to the device corresponding to the provided
            // registration token.
            val future = FirebaseMessaging
                    .getInstance(FirebaseApp.getInstance("fcm"))
                    .sendAsync(message)

            val apiFutureCallback = object : ApiFutureCallback<String> {
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
}