package org.ostelco.prime.disruptor

import com.lmax.disruptor.EventHandler
import org.ostelco.prime.logger

class ClearingEventHandler : EventHandler<PrimeEvent> {

    private val logger by logger()

    override fun onEvent(
            event: PrimeEvent,
            sequence: Long,
            endOfBatch: Boolean) {
        try {
            event.clear()
        } catch (e: Exception) {
            logger.warn("Exception clearing the prime event", e)
        }
    }
}
