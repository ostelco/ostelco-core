package org.ostelco.prime.disruptor

import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.TimeoutException
import com.lmax.disruptor.dsl.Disruptor
import io.dropwizard.lifecycle.Managed
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PrimeDisruptor : Managed {

    /**
     * Buffer size defaults to 65536 = 2^16
     */
    companion object {
        private const val BUFFER_SIZE = 65536
        private const val TIMEOUT_IN_SECONDS = 10
    }

    val disruptor: Disruptor<PrimeEvent>

    init {
        val threadFactory = Executors.privilegedThreadFactory()
        this.disruptor = Disruptor(EventFactory<PrimeEvent> { PrimeEvent() }, BUFFER_SIZE, threadFactory)
    }

    override fun start() {
        disruptor.start()
    }

    @Throws(TimeoutException::class)
    override fun stop() {
        disruptor.shutdown(TIMEOUT_IN_SECONDS.toLong(), TimeUnit.SECONDS)
    }
}
