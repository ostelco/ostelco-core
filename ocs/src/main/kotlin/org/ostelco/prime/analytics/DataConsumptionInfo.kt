package org.ostelco.prime.analytics

import com.lmax.disruptor.EventHandler
import org.ostelco.prime.disruptor.OcsEvent
import org.ostelco.prime.disruptor.EventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.logger
import org.ostelco.prime.module.getResource

/**
 * This class publishes the data consumption information events analytics.
 */
class DataConsumptionInfo() : EventHandler<OcsEvent> {

    private val logger by logger()

    private val analyticsReporter by lazy { getResource<AnalyticsService>() }

    override fun onEvent(
            event: OcsEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        if (event.messageType != CREDIT_CONTROL_REQUEST) {
            return
        }

        if (event.msisdn != null) {
            logger.info("Sent DataConsumptionInfo event to analytics")
            analyticsReporter.reportTrafficInfo(event.msisdn!!, event.usedBucketBytes, event.bundleBytes)
        }
    }
}
