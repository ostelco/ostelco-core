package org.ostelco.prime.disruptor

import com.lmax.disruptor.EventHandler
import org.ostelco.prime.getLogger

object ClearingEvent : EventHandler<OcsEvent> {

    private val logger by getLogger()

    override fun onEvent(
            event: OcsEvent,
            sequence: Long,
            endOfBatch: Boolean) {
        try {
            event.clear()
        } catch (e: Exception) {
            logger.warn("Exception clearing the prime event", e)
        }
    }
}
