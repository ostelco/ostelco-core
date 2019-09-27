package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.metrics.CustomMetricsRegistry
import org.ostelco.prime.analytics.publishers.*
import org.ostelco.prime.model.SimProfileStatus
import java.math.BigDecimal

class AnalyticsServiceImpl : AnalyticsService {
    override fun reportMetric(primeMetric: PrimeMetric, value: Long) {
        CustomMetricsRegistry.updateMetricValue(primeMetric, value)
    }

    override fun reportDataConsumption(subscriptionAnalyticsId: String, usedBucketBytes: Long, bundleBytes: Long, apn: String?, mccMnc: String?) {
        DataConsumptionInfoPublisher.publish(subscriptionAnalyticsId, usedBucketBytes, bundleBytes, apn, mccMnc)
    }

    override fun reportPurchase(customerAnalyticsId: String, purchaseId: String, sku: String, priceAmountCents: Int, priceCurrency: String) {
        PurchasePublisher.publish(customerAnalyticsId, purchaseId, sku, priceAmountCents, priceCurrency)
    }

    override fun reportRefund(customerAnalyticsId: String, purchaseId: String, reason: String?) {
        RefundPublisher.publish(customerAnalyticsId, purchaseId, reason)
    }

    override fun reportSimProvisioning(subscriptionAnalyticsId: String, customerAnalyticsId: String, regionCode: String) {
        SimProvisioningPublisher.publish(subscriptionAnalyticsId, customerAnalyticsId, regionCode)
    }

    override fun reportSubscriptionStatusUpdate(subscriptionAnalyticsId: String, status: SimProfileStatus) {
        SubscriptionStatusUpdatePublisher.publish(subscriptionAnalyticsId, status)
    }
}
