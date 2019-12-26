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
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.FCMStrings
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

class FirebaseAppNotifier: AppNotifier {
    private val logger by getLogger()

    /* Ref. to Firebase. */
    private val store = getResource<ClientDataSource>()

    /* Firebase messaging failure cases. */
    private val listOfFailureCodes = listOf(
            "messaging/invalid-recipient",
            "messaging/invalid-registration-token",
            "messaging/registration-token-not-registered",
            "registration-token-not-registered"
    )

    override fun notify(notificationType: NotificationType, customerId: String, data: Map<String, Any>) =
            when (notificationType) {
                NotificationType.JUMIO_VERIFICATION_SUCCEEDED -> {
                    logger.info("Notifying customer $customerId of successful JUMIO verification")
                    sendMessage(customerId = customerId,
                            title = FCMStrings.JUMIO_NOTIFICATION_TITLE.s,
                            body = FCMStrings.JUMIO_IDENTITY_VERIFIED.s,
                            data = data)
                }
                NotificationType.JUMIO_VERIFICATION_FAILED -> {
                    logger.info("Notifying customer $customerId of failed JUMIO verification " +
                            "with data $data")
                    sendMessage(customerId = customerId,
                            title = FCMStrings.JUMIO_NOTIFICATION_TITLE.s,
                            body = FCMStrings.JUMIO_IDENTITY_FAILED.s,
                            data = data)
                }
                NotificationType.PAYMENT_METHOD_REQUIRED -> {
                    logger.info("Notifying customer $customerId of failed payment on renewal of " +
                            "subscription to product ${data["sku"]} with data $data")
                    sendMessage(customerId = customerId,
                            title = FCMStrings.SUBSCRIPTION_RENEWAL_TITLE.s,
                            body = FCMStrings.SUBSCRIPTION_PAYMENT_METHOD_REQUIRED.s,
                            data = data)
                }
                NotificationType.USER_ACTION_REQUIRED -> {
                    /* TODO: Add support for 3D secure notification. */
                }
                NotificationType.SUBSCRIPTION_RENEWAL_UPCOMING -> {

                }
                NotificationType.SUBSCRIPTION_RENEWAL_STARTING -> {
                    /* No notification, as a notification will be sent on either successful
                       or failed renewal. */
                }
            }

    override fun notify(customerId: String, title: String, body: String, data: Map<String, Any>) {
        logger.info("Notifying customer $customerId of message $body with data $data")
        sendMessage(customerId, title, body, data)
    }

    private fun sendMessage(customerId: String, title: String, body: String, data: Map<String, Any>) =
            store.getNotificationTokens(customerId)
                    .filter {
                        it.tokenType == "FCM"
                    }
                    .forEach {
                        sendMessage(
                                customerId = customerId,
                                token = it,
                                message = Message.builder()
                                        .setNotification(
                                                Notification(title, body))
                                        .setToken(it.token)
                                        .putAllData(data.mapValues {
                                            it.value.toString()
                                        })
                                        .build()
                        )
                    }

    /* Send a message asynchrounously to the device corresponding to
       the provided registration token. */
    private fun sendMessage(customerId: String,
                            token: ApplicationToken,
                            message: Message) {
        val future = FirebaseMessaging
                .getInstance(FirebaseApp.getInstance("fcm"))
                .sendAsync(message)
        val apiFutureCallback = object : ApiFutureCallback<String> {
            override fun onSuccess(result: String) {
                logger.info("Notification for $customerId with appId: ${token.applicationID} " +
                        "completed with result: $result")
                if (listOfFailureCodes.contains(result)) {
                    store.removeNotificationToken(customerId, token.applicationID)
                }
            }

            override fun onFailure(t: Throwable) {
                if (t is FirebaseMessagingException) {
                    val errorCode = t.errorCode
                    if (listOfFailureCodes.contains(errorCode)) {
                        // Known failure, we should remove this token from our list
                        logger.info("Removing failed token (errorCode: $errorCode) for $customerId with appId: ${token.applicationID} " +
                                "token: $token.token")
                        store.removeNotificationToken(customerId, token.applicationID)
                    } else {
                        // Other failures we should look into.
                        logger.warn("Notification for $customerId  with appId: ${token.applicationID} " +
                                "failed with errorCode: $errorCode")
                    }
                } else {
                    logger.warn("Notification for $customerId  with appId: ${token.applicationID} " +
                            "failed with error: $t")
                }
            }
        }

        addCallback(future, apiFutureCallback, directExecutor())
    }
}
