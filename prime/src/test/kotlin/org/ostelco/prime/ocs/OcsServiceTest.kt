package org.ostelco.prime.ocs

import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.dsl.Disruptor
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventFactory
import org.ostelco.prime.disruptor.PrimeEventProducerImpl
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OcsServiceTest {

    private var disruptor: Disruptor<PrimeEvent>? = null

    private var countDownLatch: CountDownLatch? = null

    private var result: HashSet<PrimeEvent>? = null

    private var service: OcsService? = null

    private// Wait  wait a short while for the thing to process.
    val collectedEvent: PrimeEvent
        @Throws(InterruptedException::class)
        get() {
            assertTrue(countDownLatch!!.await(TIMEOUT, TimeUnit.SECONDS))
            assertFalse(result!!.isEmpty())
            val event = result!!.iterator().next()
            assertNotNull(event)
            return event
        }

    @Before
    fun setUp() {
        this.disruptor = Disruptor<PrimeEvent>(
                PrimeEventFactory(),
                RING_BUFFER_SIZE,
                Executors.defaultThreadFactory())
        val ringBuffer = disruptor!!.ringBuffer
        val pep = PrimeEventProducerImpl(ringBuffer)

        this.countDownLatch = CountDownLatch(1)
        this.result = HashSet()
        val eh = EventHandler<PrimeEvent> { event, sequence, endOfBatch ->
            result!!.add(event)
            countDownLatch!!.countDown()
        }

        disruptor!!.handleEventsWith(eh)
        disruptor!!.start()
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