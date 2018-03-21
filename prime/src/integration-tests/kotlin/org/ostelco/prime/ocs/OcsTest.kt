package org.ostelco.prime.ocs

import com.lmax.disruptor.TimeoutException
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.apache.commons.lang3.RandomStringUtils
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.ostelco.ocs.api.ActivateRequest
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.ocs.api.OcsServiceGrpc.OcsServiceStub
import org.ostelco.prime.disruptor.PrimeDisruptor
import org.ostelco.prime.disruptor.PrimeEventProducer
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *
 *
 * This class tests the packet gateway's perspective of talking to
 * the OCS over the gRPC-generated wire protocol.
 */
class OcsTest {

    abstract class AbstactObserver<T> : StreamObserver<T> {
        override fun onError(t: Throwable) {
            // Ignore errors
        }

        override fun onCompleted() {
            LOG.info("Completed")
        }
    }

    private fun newDefaultCreditControlRequestInfo(): CreditControlRequestInfo {
        LOG.info("Req Id: {}", REQUEST_ID)
        return CreditControlRequestInfo.newBuilder()
                .setMsisdn(MSISDN)
                .setRequestId(REQUEST_ID)
                .build()
    }

    /**
     * This whole test case tests the packet gateway talking to the OCS over
     * the gRPC interface.
     */
    @Test
    @Throws(InterruptedException::class)
    fun testFetchDataRequest() {

        // If this latch reaches zero, then things went well.
        val cdl = CountDownLatch(1)

        // Simulate being the OCS receiving a packet containing
        // information about a data bucket containing a number
        // of bytes for some MSISDN.
        val requests = ocsServiceStub!!.creditControlRequest(
                object : AbstactObserver<CreditControlAnswerInfo>() {
                    override fun onNext(response: CreditControlAnswerInfo) {
                        LOG.info("Received data bucket of {} bytes for {}",
                                response.msccOrBuilderList[0].granted.totalOctets,
                                response.msisdn)
                        assertEquals(MSISDN, response.msisdn)
                        assertEquals(BYTES.toLong(), response.msccOrBuilderList[0].granted.totalOctets)
                        assertEquals(REQUEST_ID, response.requestId)
                        cdl.countDown()
                    }
                })

        // Simulate packet gateway requesting a new bucket of data by
        // injecting a packet of data into the "onNext" method declared
        // above.
        requests.onNext(newDefaultCreditControlRequestInfo())

        // Wait for response (max ten seconds) and the pass the test only
        // if a response was actually generated (and cdl counted down to zero).
        cdl.await(TIMEOUT_IN_SECONDS.toLong(), TimeUnit.SECONDS)

        requests.onCompleted()

        assertEquals(0, cdl.count)
    }

    /**
     * Simulate sending a request to activate a subscription.
     *
     * @throws InterruptedException
     */
    @Test
    @Throws(InterruptedException::class)
    fun testActivateMsisdn() {

        val cdl = CountDownLatch(2)

        val streamObserver = object : AbstactObserver<ActivateResponse>() {
            override fun onNext(response: ActivateResponse) {
                if (!response.msisdn.isEmpty()) {
                    LOG.info("Activate {}", response.msisdn)
                    assertEquals(MSISDN, response.msisdn)
                }
                cdl.countDown()
            }
        }

        // Get the default (singelton) instance of an activation request.
        val activateRequest = ActivateRequest.getDefaultInstance()

        // Send it over the wire, but also send  stream observer that will be
        // invoked when a reply comes back.
        ocsServiceStub!!.activate(activateRequest, streamObserver)

        // Wait for a second to let things get through, then move on.
        Thread.sleep(ONE_SECOND_IN_MILLISECONDS.toLong())

        // Send a report using the producer to the pipeline that will
        // inject a PrimeEvent that will top up the data bundle balance.
        producer!!.topupDataBundleBalanceEvent(MSISDN, NO_OF_BYTES_TO_ADD.toLong())

        // Now wait, again, for the latch to reach zero, and fail the test
        // ff it hasn't.
        cdl.await(TIMEOUT_IN_SECONDS.toLong(), TimeUnit.SECONDS)
        assertEquals(0, cdl.count)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(OcsTest::class.java)

        /**
         * The port on which the gRPC service will be receiving incoming
         * connections.
         */
        private const val PORT = 8082

        /**
         * The phone numbe for which we're faking data consumption during this test.
         */
        private const val MSISDN = "4790300017"

        // Default chunk of byte used in various test cases
        private const val BYTES = 100

        // Request ID used by OCS gateway to correlate responses with requests
        private val REQUEST_ID = RandomStringUtils.randomAlphanumeric(22)

        private const val NO_OF_BYTES_TO_ADD = 10000

        private const val TIMEOUT_IN_SECONDS = 10

        private const val ONE_SECOND_IN_MILLISECONDS = 1000

        /**
         * This is the "disruptor" (processing engine) that will process
         * PrimeEvent instances and send them through a sequence of operations that
         * will update the balance of bytes for a particular subscription.
         *
         *
         * Disruptor also provides RingBuffer, which is used by Producer
         */
        private var disruptor: PrimeDisruptor? = null

        /**
         *
         */
        private var producer: PrimeEventProducer? = null

        /**
         * The gRPC service that will produce incoming events from the
         * simulated packet gateway, contains an [OcsService] instance bound
         * to a particular port (in our case 8082).
         */
        private var ocsServer: OcsServer? = null

        /**
         * The "sub" that will mediate access to the GRPC channel,
         * providing an API to send / receive data to/from it.
         */
        private var ocsServiceStub: OcsServiceStub? = null

        @BeforeClass
        @Throws(IOException::class)
        fun setUp() {

            // Set up processing pipeline
            disruptor = PrimeDisruptor()
            producer = PrimeEventProducer(disruptor!!.disruptor.ringBuffer)

            // Set up the gRPC server at a particular port with a particular
            // service, that is connected to the processing pipeline.
            val ocsService = OcsService(producer!!)
            ocsServer = OcsServer(PORT, ocsService.asOcsServiceImplBase())

            val ocsState = OcsState()
            ocsState.addDataBundleBytes(MSISDN, NO_OF_BYTES_TO_ADD.toLong())

            // Events flow:
            //      Producer:(OcsService, Subscriber)
            //          -> Handler:(OcsState)
            //              -> Handler:(OcsService, Subscriber)
            disruptor!!.disruptor.handleEventsWith(ocsState).then(ocsService.asEventHandler())

            // start disruptor and ocs services.
            disruptor!!.start()
            ocsServer!!.start()

            // Set up a channel to be used to communicate as an OCS instance, to an
            // Prime instance.
            val channel = ManagedChannelBuilder.forTarget("0.0.0.0:" + PORT).usePlaintext(true). // disable encryption for testing
                    build()

            // Initialize the stub that will be used to actually
            // communicate from the client emulating being the OCS.
            ocsServiceStub = OcsServiceGrpc.newStub(channel)
        }

        @AfterClass
        @Throws(InterruptedException::class, TimeoutException::class)
        fun tearDown() {
            if (ocsServer != null) {
                ocsServer!!.stop()
            }
            if (disruptor != null) {
                disruptor!!.stop()
            }
        }
    }
}
