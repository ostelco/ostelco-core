package org.ostelco.prime.events

import com.lmax.disruptor.EventHandler
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.logger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

/**
 * For normal execution, do not pass `storage`.
 * It will be initialized properly using `getResource()`.
 * Storage is parameterized into constructor to be able to pass mock for unit testing.
 */
class EventProcessor(
        private val storage: ClientDataSource = getResource()) : EventHandler<PrimeEvent> {

    private val LOG by logger()

    override fun onEvent(
            event: PrimeEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        try {
            LOG.info("Updating data bundle balance for {} to {} bytes",
                    event.msisdn, event.bundleBytes)
            val msisdn = event.msisdn
            if (msisdn != null) {
                setRemainingByMsisdn(msisdn, event.bundleBytes)
            }
        } catch (e: Exception) {
            LOG.warn("Exception handling prime event in EventProcessor", e)
        }
    }

    @Throws(EventProcessorException::class)
    private fun setRemainingByMsisdn(
            msisdn: String,
            noOfBytes: Long) {
        try {
            storage.setBalance(msisdn, noOfBytes)
        } catch (e: Exception) {
            throw EventProcessorException(e)
        }
    }
}