package org.ostelco.prime.disruptor

import com.lmax.disruptor.EventFactory
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
import org.ostelco.prime.disruptor.EventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.disruptor.EventMessageType.TOPUP_DATA_BUNDLE_BALANCE
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PrimeEventProducerTest {

    private lateinit var primeEventProducer: EventProducerImpl

    private var disruptor: Disruptor<OcsEvent>? = null

    private lateinit var countDownLatch: CountDownLatch

    private lateinit var result: MutableSet<OcsEvent>

    private// Wait a short while for the thing to process.
    val collectedEvent: OcsEvent
        get() {
            assertTrue(countDownLatch.await(TIMEOUT.toLong(), TimeUnit.SECONDS))
            assertFalse(result.isEmpty())
            val event = result.iterator().next()
            assertNotNull(event)
            return event
        }


    @Before
    fun setUp() {
        this.disruptor = Disruptor(
                EventFactory<OcsEvent> { OcsEvent() },
                RING_BUFFER_SIZE,
                Executors.defaultThreadFactory())
        val ringBuffer = disruptor?.ringBuffer ?: throw Exception("Failed to init disruptor")
        this.primeEventProducer = EventProducerImpl(ringBuffer)

        this.countDownLatch = CountDownLatch(1)
        this.result = HashSet()
        val eh = EventHandler<OcsEvent> { event, _, _ ->
            result.add(event)
            countDownLatch.countDown()
        }

        disruptor?.handleEventsWith(eh)
        disruptor?.start()
    }

    @After
    fun shutDown() {
        disruptor?.shutdown()
    }

    @Test
    fun topupDataBundleBalanceEvent() {

        // Stimulating a response
        primeEventProducer.topupDataBundleBalanceEvent(
                requestId = TOPUP_REQUEST_ID,
                bundleId = BUNDLE_ID,
                bytes = NO_OF_TOPUP_BYTES)

        // Collect an event (or fail trying).
        val event = collectedEvent

        // Verify some behavior
        assertEquals(BUNDLE_ID, event.bundleId)
        assertEquals(NO_OF_TOPUP_BYTES, event.topupContext?.topUpBytes)
        assertEquals(TOPUP_DATA_BUNDLE_BALANCE, event.messageType)
    }

    @Test
    fun creditControlRequestEvent() {
        val request = CreditControlRequestInfo.newBuilder().setMsisdn(MSISDN).addMscc(MultipleServiceCreditControl.newBuilder()
                .setRequested(ServiceUnit.newBuilder()
                        .setTotalOctets(REQUESTED_BYTES)
                        .build())
                .setUsed(ServiceUnit.newBuilder().setTotalOctets(USED_BYTES).build())
                .setRatingGroup(RATING_GROUP)
                .setServiceIdentifier(SERVICE_IDENTIFIER)
                .build()
        ).build()

        primeEventProducer.injectCreditControlRequestIntoRingbuffer(STREAM_ID, request)

        val event = collectedEvent
        assertEquals(MSISDN, event.msisdn)
        assertEquals(REQUESTED_BYTES, event.request?.msccList?.firstOrNull()?.requested?.totalOctets ?: 0L)
        assertEquals(USED_BYTES, event.request?.msccList?.firstOrNull()?.used?.totalOctets ?: 0L)
        assertEquals(RATING_GROUP, event.request?.msccList?.firstOrNull()?.ratingGroup)
        assertEquals(SERVICE_IDENTIFIER, event.request?.msccList?.firstOrNull()?.serviceIdentifier)
        assertEquals(STREAM_ID, event.ocsgwStreamId)
        assertEquals(CREDIT_CONTROL_REQUEST, event.messageType)
    }

    companion object {

        private const val NO_OF_TOPUP_BYTES = 991234L

        private const val REQUESTED_BYTES = 500L

        private const val USED_BYTES = 300L

        private const val BUNDLE_ID = "foo@bar.com"

        private const val MSISDN = "4711223344"

        private const val STREAM_ID = "mySecret stream"

        private const val RING_BUFFER_SIZE = 256

        private const val TIMEOUT = 10

        private const val RATING_GROUP = 10L

        private const val SERVICE_IDENTIFIER = 1L

        private const val TOPUP_REQUEST_ID = "req-id"
    }
}

