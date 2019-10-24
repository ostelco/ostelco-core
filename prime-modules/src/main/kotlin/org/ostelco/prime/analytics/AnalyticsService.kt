package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.MetricType.GAUGE
import org.ostelco.prime.model.SimProfileStatus
import java.math.BigDecimal

interface AnalyticsService {
    fun reportMetric(primeMetric: PrimeMetric, value: Long)

    fun reportDataConsumption(subscriptionAnalyticsId: String, usedBucketBytes: Long, bundleBytes: Long, apn: String?, mccMnc: String?)
    fun reportPurchase(customerAnalyticsId: String, purchaseId: String, sku: String, priceAmountCents: Int, priceCurrency: String)
    fun reportRefund(customerAnalyticsId: String, purchaseId: String, reason: String?)
    fun reportSimProvisioning(subscriptionAnalyticsId: String, customerAnalyticsId: String, regionCode: String)
    fun reportSubscriptionStatusUpdate(subscriptionAnalyticsId: String, status: SimProfileStatus)
}

enum class PrimeMetric(val metricType: MetricType) {

    // sorted alphabetically
    ACTIVE_SESSIONS(GAUGE),
    TOTAL_USERS(GAUGE),
    USERS_ACQUIRED_THROUGH_REFERRALS(GAUGE);

    val metricName: String
        get() = name.toLowerCase()
}

enum class MetricType {
    COUNTER,
    GAUGE,
}
