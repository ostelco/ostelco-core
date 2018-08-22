package org.ostelco.prime.events

import com.lmax.disruptor.EventHandler
import org.ostelco.prime.disruptor.EventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.disruptor.EventMessageType.RELEASE_RESERVED_BUCKET
import org.ostelco.prime.disruptor.EventMessageType.REMOVE_MSISDN_TO_BUNDLE_MAPPING
import org.ostelco.prime.disruptor.EventMessageType.TOPUP_DATA_BUNDLE_BALANCE
import org.ostelco.prime.disruptor.OcsEvent
import org.ostelco.prime.logger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

/**
 * For normal execution, do not pass `storage`.
 * It will be initialized properly using `getResource()`.
 * Storage is parameterized into constructor to be able to pass mock for unit testing.
 */
class EventProcessor(
        private val storage: ClientDataSource = getResource()) : EventHandler<OcsEvent> {

    private val logger by logger()

    override fun onEvent(
            event: OcsEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        try {
            if (event.messageType == CREDIT_CONTROL_REQUEST
                    || event.messageType == RELEASE_RESERVED_BUCKET
                    || event.messageType == TOPUP_DATA_BUNDLE_BALANCE
                    || event.messageType == REMOVE_MSISDN_TO_BUNDLE_MAPPING) {
                logger.info("Updating data bundle balance for {} : {} to {} bytes",
                        event.msisdn, event.bundleId, event.bundleBytes)
                val bundleId = event.bundleId
                if (bundleId != null) {
                    storage.updateBundle(Bundle(bundleId, event.bundleBytes))
                }
            }
        } catch (e: Exception) {
            logger.warn("Exception handling prime event in EventProcessor", e)
        }
    }
}