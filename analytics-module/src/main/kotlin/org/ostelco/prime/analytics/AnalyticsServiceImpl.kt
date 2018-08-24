package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.publishers.DataConsumptionInfoPublisher
import org.ostelco.prime.logger

class AnalyticsServiceImpl : AnalyticsService {

    private val logger by logger()

    override fun reportTrafficInfo(msisdn: String, usedBytes: Long, bundleBytes: Long) {
        logger.info("reportTrafficInfo : msisdn {} usedBytes {} bundleBytes {}", msisdn, usedBytes, bundleBytes)
        DataConsumptionInfoPublisher.publish(msisdn, usedBytes, bundleBytes)
    }
}