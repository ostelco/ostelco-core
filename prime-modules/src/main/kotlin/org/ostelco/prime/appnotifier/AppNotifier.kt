package org.ostelco.prime.appnotifier

/* Prime specific notification types. */
enum class NotificationType {
    JUMIO_VERIFICATION_SUCCEEDED,
    JUMIO_VERIFICATION_FAILED,
}

interface AppNotifier {

    /**
     * Prime specific interface for sending notification messages to
     * customers/clients.
     * @param notificationType Type of notification to be sent.
     * @param customerId Id of the receipient/customer.
     * @param data Additional data to be added to the message if any.
     */
    fun notify(notificationType: NotificationType, customerId: String, data: Map<String, Any> = emptyMap())

    /**
     * Low level interface for sending notification messages to
     * customers/clients.
     * @param customerId Id of the customer to receive the notification.
     * @param title The 'title' part of the notification.
     * @param body The message part of the notification.
     * @param data Additional data to be added to the message if any.
     */
    fun notify(customerId: String, title: String, body: String, data: Map<String, Any> = emptyMap())
}
