package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.metrics.CustomMetricsRegistry
import org.ostelco.prime.analytics.publishers.DataConsumptionInfoPublisher
import org.ostelco.prime.analytics.publishers.PurchaseInfoPublisher
import org.ostelco.prime.logger
import org.ostelco.prime.model.PurchaseRecord

class AnalyticsServiceImpl : AnalyticsService {

    private val logger by logger()

    override fun reportTrafficInfo(msisdn: String, usedBytes: Long, bundleBytes: Long) {
        logger.info("reportTrafficInfo : msisdn {} usedBytes {} bundleBytes {}", msisdn, usedBytes, bundleBytes)
        DataConsumptionInfoPublisher.publish(msisdn, usedBytes, bundleBytes)
    }

    override fun reportMetric(primeMetric: PrimeMetric, value: Long) {
        CustomMetricsRegistry.updateMetricValue(primeMetric, value)
    }

    override fun reportPurchaseInfo(purchaseRecord: PurchaseRecord, subscriberId: String, status: String) {
        PurchaseInfoPublisher.publish(purchaseRecord, subscriberId, status)
    }
}