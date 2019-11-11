package org.ostelco.prime.analytics.publishers

import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.analytics.events.RefundEvent
import org.ostelco.common.publisherex.DelegatePubSubPublisher
import org.ostelco.common.publisherex.PubSubPublisher


/**
 * This class publishes the refund information events to Google Cloud Pub/Sub.
 */
object RefundPublisher :
        PubSubPublisher by DelegatePubSubPublisher(
                topicId = ConfigRegistry.config.refundsTopicId,
                projectId = ConfigRegistry.config.projectId) {

    fun publish(customerAnalyticsId: String, purchaseId: String, reason: String?) {
        publishEvent(RefundEvent(
                customerAnalyticsId = customerAnalyticsId,
                purchaseId = purchaseId,
                reason = reason
        ))
    }
}
