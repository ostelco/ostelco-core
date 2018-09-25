package org.ostelco.prime.disruptor

import com.lmax.disruptor.EventHandler
import org.ostelco.prime.disruptor.EventMessageType.ADD_MSISDN_TO_BUNDLE_MAPPING
import org.ostelco.prime.disruptor.EventMessageType.UPDATE_BUNDLE
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

/**
 * For normal execution, do not pass `storage`.
 * It will be initialized properly using `getResource()`.
 * Storage is parameterized into constructor to be able to pass mock for unit testing.
 */
class BundleBalanceStore(private val storage: ClientDataSource = getResource()) : EventHandler<OcsEvent> {

    private val logger by getLogger()

    override fun onEvent(
            event: OcsEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        try {
            if (event.messageType != UPDATE_BUNDLE && event.messageType != ADD_MSISDN_TO_BUNDLE_MAPPING) {

                logger.info("Updating data bundle balance for bundleId : {} to {} bytes",
                        event.bundleId, event.bundleBytes)
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