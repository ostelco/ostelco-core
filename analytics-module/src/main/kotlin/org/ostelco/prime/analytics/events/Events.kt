package org.ostelco.prime.analytics.events

import org.ostelco.prime.model.SimProfileStatus
import java.math.BigDecimal
import java.time.Instant

/**
 * Abstraction for a point-in-time analytics event.
 *
 * @property timestamp time at which the event occurs
 */
sealed class Event(val timestamp: Instant = Instant.now()) {
    fun toJsonByteString() = CommonPubSubJsonSerializer.toJsonByteString(this)
}

/**
 * Represents a new purchase.
 *
 * WARNING: When modifying the structure of this class, you must also update the schema for the
 * corresponding BigQuery table.
 *
 * @property customerAnalyticsId customer analytics ID _(foreign key)_
 * @property purchaseId purchase ID from Stripe _(foreign key)_
 * @property sku SKU for the purchased product _(foreign key)_
 * @property priceAmount amount that was charged, in whole currency units (e.g. NOK 10.50 => 10.50, *not* 1050 as in Stripe)
 * @property priceCurrency ISO 4217-compliant currency code
 */
data class PurchaseEvent(
        val customerAnalyticsId: String,
        val purchaseId: String,
        val sku: String,
        val priceAmount: BigDecimal,
        val priceCurrency: String
) : Event()

/**
 * Represents a refund of a previous purchase.
 *
 * WARNING: When modifying the structure of this class, you must also update the schema for the
 * corresponding BigQuery table.
 *
 * @property customerAnalyticsId customer analytics ID _(foreign key)_
 * @property purchaseId purchase ID from Stripe _(foreign key)_
 * @property reason reason for the refund
 */
data class RefundEvent(
        val customerAnalyticsId: String,
        val purchaseId: String,
        val reason: String?
) : Event()

/**
 * Represents the provisioning of a new SIM card for a subscription in a given region.
 *
 * WARNING: When modifying the structure of this class, you must also update the schema for the
 * corresponding BigQuery table.
 *
 * @property subscriptionAnalyticsId subscription analytics ID _(foreign key)_
 * @property customerAnalyticsId customer analytics ID _(foreign key)_
 * @property regionCode region code _(foreign key)_
 */
data class SimProvisioningEvent(
        val subscriptionAnalyticsId: String,
        val customerAnalyticsId: String,
        val regionCode: String
) : Event()

/**
 * Represents an update on the SIM profile attached to the subscription.
 *
 * WARNING: When modifying the structure of this class, you must also update the schema for the
 * corresponding BigQuery table.
 *
 * @property subscriptionAnalyticsId subscription analytics ID _(foreign key)_
 * @property status new status for the SIM profile linked to the subscription
 */
data class SubscriptionStatusUpdateEvent(
        val subscriptionAnalyticsId: String,
        val status: SimProfileStatus
) : Event()

/**
 * Represents the consumption of a portion of a data bundle by a subscription.
 *
 * WARNING: When modifying the structure of this class, you must also update the schema for the
 * corresponding BigQuery table.
 *
 * @property subscriptionAnalyticsId subscription analytics ID _(foreign key)_
 * @property usedBucketBytes bytes used just now, before the event is fired (in Bytes)
 * @property bundleBytes bytes remaining in the bundle (in Bytes)
 * @property apn access point name
 * @property mccMnc MCC-MNC pair
 */
data class DataConsumptionEvent(
        val subscriptionAnalyticsId: String,
        val usedBucketBytes: Long,
        val bundleBytes: Long,
        val apn: String?,
        val mccMnc: String?
) : Event()
