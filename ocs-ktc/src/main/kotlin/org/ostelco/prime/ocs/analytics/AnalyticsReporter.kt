package org.ostelco.prime.ocs.analytics

import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource

/**
 * This class publishes the data consumption information events analytics.
 */
object AnalyticsReporter {

    private val logger by getLogger()

    private val analyticsReporter by lazy { getResource<AnalyticsService>() }

    fun report(subscriptionAnalyticsId: String, request: CreditControlRequestInfo, bundleBytes: Long, mccMnc: String) {
        logger.info("Sent Data Consumption info event to analytics")

        analyticsReporter.reportDataConsumption(
                subscriptionAnalyticsId = subscriptionAnalyticsId,
                usedBucketBytes = request.msccList?.firstOrNull()?.used?.totalOctets ?: 0L,
                bundleBytes = bundleBytes,
                apn = request.serviceInformation?.psInformation?.calledStationId,
                mccMnc = mccMnc
        )
    }
}
