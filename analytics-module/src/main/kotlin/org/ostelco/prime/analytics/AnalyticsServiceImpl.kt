package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.metrics.CustomMetricsRegistry
import org.ostelco.prime.analytics.publishers.DataConsumptionInfoPublisher
import org.ostelco.prime.analytics.publishers.PurchaseInfoPublisher
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.PurchaseRecord

class AnalyticsServiceImpl : AnalyticsService {

    private val logger by getLogger()

    override fun reportTrafficInfo(msisdnAnalyticsId: String, usedBytes: Long, bundleBytes: Long, apn: String?, mccMnc: String?) {
        logger.info("reportTrafficInfo : msisdnAnalyticsId {} usedBytes {} bundleBytes {} apn {} mccMnc {}", msisdnAnalyticsId, usedBytes, bundleBytes, apn, mccMnc)
        DataConsumptionInfoPublisher.publish(msisdnAnalyticsId, usedBytes, bundleBytes, apn, mccMnc)
    }

    override fun reportMetric(primeMetric: PrimeMetric, value: Long) {
        CustomMetricsRegistry.updateMetricValue(primeMetric, value)
    }

    override fun reportPurchaseInfo(purchaseRecord: PurchaseRecord, customerAnalyticsId: String, status: String) {
        PurchaseInfoPublisher.publish(purchaseRecord, customerAnalyticsId, status)
    }
}