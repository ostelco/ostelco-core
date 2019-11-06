package org.ostelco.prime.analytics.publishers

import org.ostelco.prime.analytics.ConfigRegistry
import org.ostelco.prime.analytics.events.PurchaseEvent
import org.ostelco.common.publisherex.DelegatePubSubPublisher
import org.ostelco.common.publisherex.PubSubPublisher

/**
 * This class publishes the purchase information events to Google Cloud Pub/Sub.
 */
object PurchasePublisher :
        PubSubPublisher by DelegatePubSubPublisher(
                topicId = ConfigRegistry.config.purchaseInfoTopicId,
                projectId = ConfigRegistry.config.projectId) {

    fun publish(customerAnalyticsId: String, purchaseId: String, sku: String, priceAmountCents: Int, priceCurrency: String) {
        publishEvent(PurchaseEvent(
                customerAnalyticsId = customerAnalyticsId,
                purchaseId = purchaseId,
                sku = sku,
                priceAmount = priceAmountCents.toBigDecimal().divide(100.toBigDecimal()),
                priceCurrency = priceCurrency.toUpperCase()
        ))
    }
}
