package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.MetricType.GAUGE
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.SimProvisioning

interface AnalyticsService {
    fun reportTrafficInfo(subscriptionAnalyticsId: String, usedBucketBytes: Long, bundleBytes: Long, apn: String?, mccMnc: String?)
    fun reportMetric(primeMetric: PrimeMetric, value: Long)
    fun reportPurchaseInfo(purchaseRecord: PurchaseRecord, customerAnalyticsId: String, status: String)
    fun reportSimProvisioning(subscriptionAnalyticsId: String, customerAnalyticsId: String, regionCode: String)
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
