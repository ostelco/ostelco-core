package org.ostelco.prime.appnotifier

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures.addCallback
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

class FirebaseAppNotifier: AppNotifier {
    private val logger by getLogger()

    val listOfFailureCodes = listOf(
            "messaging/invalid-recipient",
            "messaging/invalid-registration-token",
            "messaging/registration-token-not-registered"
    )

    override fun notify(customerId: String, title: String, body: String) {
        logger.info("Will try to notify customer with Id : $customerId")
        sendNotification(customerId, title, body, data = null)
    }

    override fun notify(customerId: String, title: String, body: String, data: Map<String, String>) {
        logger.info("Will try to notify-with-data customer with Id : $customerId $body, $data")
        sendNotification(customerId, title, body, data)
    }

    private fun sendNotification(customerId: String, title: String, body: String, data: Map<String, String>?) {

        val store = getResource<ClientDataSource>()

        // This registration token comes from the client FCM SDKs.
        val applicationTokens = store.getNotificationTokens(customerId)

        for (applicationToken in applicationTokens) {

            if (applicationToken.tokenType == "FCM") {
                // See documentation on defining a message payload.
                val builder = Message.builder()
                        .setNotification(Notification(title, body))
                        .setToken(applicationToken.token)
                if (data != null) {
                    builder.putAllData(data)
                }
                val message = builder.build()

                // Send a message to the device corresponding to the provided
                // registration token.
                val future = FirebaseMessaging
                        .getInstance(FirebaseApp.getInstance("fcm"))
                        .sendAsync(message)

                val apiFutureCallback = object : ApiFutureCallback<String> {
                    override fun onSuccess(result: String) {
                        logger.info("Notification for $customerId with appId: ${applicationToken.applicationID} completed with result: $result")
                        if (listOfFailureCodes.contains(result)) {
                            store.removeNotificationToken(customerId, applicationToken.applicationID)
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        if (t is FirebaseMessagingException) {
                            val errorCode = t.errorCode
                            logger.warn("Notification for $customerId  with appId: ${applicationToken.applicationID} failed with errorCode: $errorCode")
                            if (listOfFailureCodes.contains(errorCode)) {
                                logger.warn("Removing failed token for $customerId with appId: ${applicationToken.applicationID} token: $applicationToken.token")
                                store.removeNotificationToken(customerId, applicationToken.applicationID)
                            }
                        } else {
                            logger.warn("Notification for $customerId  with appId: ${applicationToken.applicationID} failed with error: $t")
                        }
                    }
                }
                addCallback(future, apiFutureCallback, directExecutor())
            }
        }
    }
}