package org.ostelco.prime.ocs

import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.dsl.Disruptor
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.consumption.OcsService
import org.ostelco.prime.disruptor.EventProducerImpl
import org.ostelco.prime.disruptor.OcsEvent
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OcsServiceTest {

    private var disruptor: Disruptor<OcsEvent>?= null

    private var countDownLatch: CountDownLatch? = null

    private var result: HashSet<OcsEvent>? = null

    private var service: OcsService? = null

    private// Wait  wait a short while for the thing to process.
    val collectedEvent: OcsEvent?
        get() {
            assertTrue(countDownLatch?.await(TIMEOUT, TimeUnit.SECONDS) ?: false)
            assertFalse(result?.isEmpty() ?: true)
            val event = result?.iterator()?.next()
            assertNotNull(event)
            return event
        }

    @Before
    fun setUp() {

        val disruptor = Disruptor(
                EventFactory<OcsEvent> { OcsEvent() },
                RING_BUFFER_SIZE,
                Executors.defaultThreadFactory())

        this.disruptor = disruptor
        val ringBuffer = disruptor.ringBuffer
        val pep = EventProducerImpl(ringBuffer)

        val countDownLatch = CountDownLatch(1)
        this.countDownLatch = countDownLatch
        val result = HashSet<OcsEvent>()
        this.result = result
        val eh = EventHandler<OcsEvent> { event, _, _ ->
            result.add(event)
            countDownLatch.countDown()
        }

        disruptor.handleEventsWith(eh)
        disruptor.start()
        this.service = OcsService(pep)
    }

    @After
    fun shutDown() {
        disruptor?.shutdown()
    }

    @Test
    fun onEvent() {
        // tbd
    }


    @Test
    fun fetchDataBucket() {
        // tbd
    }

    @Test
    fun returnUnusedData() {
        // tbd
    }

    @Test
    fun activate() {
        // tbd
    }

    companion object {

        private const val RING_BUFFER_SIZE = 256

        private const val TIMEOUT: Long = 10
    }
}