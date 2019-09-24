package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.metrics.CustomMetricsRegistry
import org.ostelco.prime.analytics.publishers.DataConsumptionInfoPublisher
import org.ostelco.prime.analytics.publishers.PurchaseInfoPublisher
import org.ostelco.prime.analytics.publishers.SimProvisioningPublisher
import org.ostelco.prime.analytics.publishers.SubscriptionStatusUpdatePublisher
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.SimProfileStatus

class AnalyticsServiceImpl : AnalyticsService {

    override fun reportTrafficInfo(subscriptionAnalyticsId: String, usedBucketBytes: Long, bundleBytes: Long, apn: String?, mccMnc: String?) {
        DataConsumptionInfoPublisher.publish(subscriptionAnalyticsId, usedBucketBytes, bundleBytes, apn, mccMnc)
    }

    override fun reportMetric(primeMetric: PrimeMetric, value: Long) {
        CustomMetricsRegistry.updateMetricValue(primeMetric, value)
    }

    override fun reportPurchaseInfo(purchaseRecord: PurchaseRecord, customerAnalyticsId: String, status: String) {
        PurchaseInfoPublisher.publish(purchaseRecord, customerAnalyticsId, status)
    }

    override fun reportSimProvisioning(subscriptionAnalyticsId: String, customerAnalyticsId: String, regionCode: String) {
        SimProvisioningPublisher.publish(subscriptionAnalyticsId, customerAnalyticsId, regionCode)
    }

    override fun reportSimStatusUpdate(subscriptionAnalyticsId: String, status: SimProfileStatus) {
        SubscriptionStatusUpdatePublisher.publish(subscriptionAnalyticsId, status)
    }
}
