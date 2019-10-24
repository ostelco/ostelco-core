package org.ostelco.prime.analytics.publishers

import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.analytics.events.RefundEvent


/**
 * This class publishes the refund information events to Google Cloud Pub/Sub.
 */
object RefundPublisher :
        PubSubPublisher by DelegatePubSubPublisher(topicId = ConfigRegistry.config.refundsTopicId) {

    fun publish(customerAnalyticsId: String, purchaseId: String, reason: String?) {
        publishEvent(RefundEvent(
                customerAnalyticsId = customerAnalyticsId,
                purchaseId = purchaseId,
                reason = reason
        ))
    }
}
