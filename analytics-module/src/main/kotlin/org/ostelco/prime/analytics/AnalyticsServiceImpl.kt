package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.metrics.CustomMetricsRegistry
import org.ostelco.prime.analytics.publishers.DataConsumptionInfoPublisher
import org.ostelco.prime.analytics.publishers.PurchaseInfoPublisher
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.PurchaseRecord

class AnalyticsServiceImpl : AnalyticsService {

    private val logger by getLogger()

    override fun reportTrafficInfo(subscriptionAnalyticsId: String, usedBucketBytes: Long, bundleBytes: Long, apn: String?, mccMnc: String?) {
        logger.info("reportTrafficInfo : subscriptionAnalyticsId {} usedBucketBytes {} bundleBytes {} apn {} mccMnc {}", subscriptionAnalyticsId, usedBucketBytes, bundleBytes, apn, mccMnc)
        DataConsumptionInfoPublisher.publish(subscriptionAnalyticsId, usedBucketBytes, bundleBytes, apn, mccMnc)
    }

    override fun reportMetric(primeMetric: PrimeMetric, value: Long) {
        CustomMetricsRegistry.updateMetricValue(primeMetric, value)
    }

    override fun reportPurchaseInfo(purchaseRecord: PurchaseRecord, customerAnalyticsId: String, status: String) {
        PurchaseInfoPublisher.publish(purchaseRecord, customerAnalyticsId, status)
    }
}
