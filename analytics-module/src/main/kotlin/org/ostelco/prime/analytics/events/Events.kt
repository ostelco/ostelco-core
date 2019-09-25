package org.ostelco.prime.analytics.events

import org.ostelco.prime.model.SimProfileStatus
import java.math.BigDecimal
import java.time.Instant

sealed class Event(val timestamp: Instant = Instant.now()) {
    fun toJsonByteString() = CommonPubSubJsonSerializer.toJsonByteString(this)
}

data class PurchaseEvent(
        val customerAnalyticsId: String,
        val purchaseId: String,
        val sku: String,
        val priceAmount: BigDecimal,
        val priceCurrency: String
) : Event()

data class RefundEvent(
        val customerAnalyticsId: String,
        val purchaseId: String,
        val reason: String?
) : Event()

data class SimProvisioningEvent(
        val subscriptionAnalyticsId: String,
        val customerAnalyticsId: String,
        val regionCode: String
) : Event()

data class SubscriptionStatusUpdateEvent(
        val subscriptionAnalyticsId: String,
        val status: SimProfileStatus
) : Event()

data class DataConsumptionEvent(
        val subscriptionAnalyticsId: String,
        val usedBucketBytes: Long,
        val bundleBytes: Long,
        val apn: String?,
        val mccMnc: String?
) : Event()
