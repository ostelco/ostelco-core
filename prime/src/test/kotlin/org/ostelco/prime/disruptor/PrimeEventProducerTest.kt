package org.ostelco.prime.disruptor

import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.dsl.Disruptor
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.disruptor.PrimeEventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.disruptor.PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PrimeEventProducerTest {

    private var pep: PrimeEventProducer? = null

    private var disruptor: Disruptor<PrimeEvent>? = null

    private var cdl: CountDownLatch? = null

    private var result: MutableSet<PrimeEvent>? = null

    private// Wait a short while for the thing to process.
    val collectedEvent: PrimeEvent
        @Throws(InterruptedException::class)
        get() {
            assertTrue(cdl!!.await(TIMEOUT.toLong(), TimeUnit.SECONDS))
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
        this.pep = PrimeEventProducer(ringBuffer)

        this.cdl = CountDownLatch(1)
        this.result = HashSet()
        val eh = EventHandler<PrimeEvent> { event, sequence, endOfBatch ->
            result!!.add(event)
            cdl!!.countDown()
        }

        disruptor!!.handleEventsWith(eh)
        disruptor!!.start()
    }

    @After
    fun shutDown() {
        disruptor!!.shutdown()
    }

    @Test
    @Throws(Exception::class)
    fun topupDataBundleBalanceEvent() {

        // Stimulating a response
        pep!!.topupDataBundleBalanceEvent(MSISDN, NO_OF_TOPUP_BYTES)

        // Collect an event (or fail trying).
        val event = collectedEvent

        // Verify some behavior
        assertEquals(MSISDN, event.msisdn)
        assertEquals(NO_OF_TOPUP_BYTES, event.requestedBucketBytes)
        assertEquals(TOPUP_DATA_BUNDLE_BALANCE, event.messageType)
    }

    @Test
    @Throws(Exception::class)
    fun creditControlRequestEvent() {
        val request = CreditControlRequestInfo.newBuilder().setMsisdn(MSISDN).addMscc(MultipleServiceCreditControl.newBuilder()
                .setRequested(ServiceUnit.newBuilder()
                        .setTotalOctets(REQUESTED_BYTES)
                        .build())
                .setUsed(ServiceUnit.newBuilder().setTotalOctets(USED_BYTES).build())
                .setRatingGroup(10)
                .setServiceIdentifier(1)
                .build()
        ).build()

        pep!!.injectCreditControlRequestIntoRingbuffer(request, STREAM_ID)

        val event = collectedEvent
        assertEquals(MSISDN, event.msisdn)
        assertEquals(REQUESTED_BYTES, event.requestedBucketBytes)
        assertEquals(USED_BYTES, event.usedBucketBytes)
        assertEquals(10, event.ratingGroup)
        assertEquals(1, event.serviceIdentifier)
        assertEquals(STREAM_ID, event.ocsgwStreamId)
        assertEquals(CREDIT_CONTROL_REQUEST, event.messageType)
    }

    companion object {

        private const val NO_OF_TOPUP_BYTES = 991234L

        private const val REQUESTED_BYTES = 500L

        private const val USED_BYTES = 300L

        private const val MSISDN = "+4711223344"

        private const val STREAM_ID = "mySecret stream"

        private const val RING_BUFFER_SIZE = 256

        private const val TIMEOUT = 10
    }
}

