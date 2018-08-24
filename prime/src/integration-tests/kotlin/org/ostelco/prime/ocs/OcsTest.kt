package org.ostelco.prime.ocs

import com.lmax.disruptor.TimeoutException
import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.apache.commons.lang3.RandomStringUtils
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.ocs.api.ActivateRequest
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.CreditControlRequestType.INITIAL_REQUEST
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.ocs.api.OcsServiceGrpc.OcsServiceStub
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.disruptor.EventProducerImpl
import org.ostelco.prime.disruptor.OcsDisruptor
import org.ostelco.prime.logger
import org.ostelco.prime.storage.firebase.initFirebaseConfigRegistry
import org.ostelco.prime.storage.graph.Config
import org.ostelco.prime.storage.graph.ConfigRegistry
import org.ostelco.prime.storage.graph.Neo4jClient
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

    private val logger by logger()

    abstract class AbstractObserver<T> : StreamObserver<T> {

        private val logger by logger()

        override fun onError(t: Throwable) {
            // Ignore errors
        }

        override fun onCompleted() {
            logger.info("Completed")
        }
    }

    private fun newDefaultCreditControlRequestInfo(): CreditControlRequestInfo {
        logger.info("Req Id: {}", REQUEST_ID)

        val mscc = MultipleServiceCreditControl.newBuilder()
                .setRequested(ServiceUnit
                        .newBuilder()
                        .setTotalOctets(BYTES))
        return CreditControlRequestInfo.newBuilder()
                .setType(INITIAL_REQUEST)
                .setMsisdn(MSISDN)
                .setRequestId(REQUEST_ID)
                .addMscc(mscc)
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
        val requests = ocsServiceStub.creditControlRequest(
                object : AbstractObserver<CreditControlAnswerInfo>() {
                    override fun onNext(response: CreditControlAnswerInfo) {
                        logger.info("Received answer for {}",
                                response.msisdn)
                        assertEquals(MSISDN, response.msisdn)
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
        cdl.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)

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

        val streamObserver = object : AbstractObserver<ActivateResponse>() {
            override fun onNext(response: ActivateResponse) {
                if (!response.msisdn.isEmpty()) {
                    logger.info("Activate {}", response.msisdn)
                    assertEquals(MSISDN, response.msisdn)
                }
                cdl.countDown()
            }
        }

        // Get the default (singelton) instance of an activation request.
        val activateRequest = ActivateRequest.getDefaultInstance()

        // Send it over the wire, but also send  stream observer that will be
        // invoked when a reply comes back.
        ocsServiceStub.activate(activateRequest, streamObserver)

        // Wait for a second to let things get through, then move on.
        Thread.sleep(ONE_SECOND_IN_MILLISECONDS)

        // Send a report using the producer to the pipeline that will
        // inject a PrimeEvent that will top up the data bundle balance.
        producer.topupDataBundleBalanceEvent(BUNDLE_ID, NO_OF_BYTES_TO_ADD)

        // Now wait, again, for the latch to reach zero, and fail the test
        // ff it hasn't.
        cdl.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
        assertEquals(0, cdl.count)
    }

    companion object {

        /**
         * The port on which the gRPC service will be receiving incoming
         * connections.
         */
        private const val PORT = 8082

        /**
         * The phone numbe for which we're faking data consumption during this test.
         */
        private const val MSISDN = "4790300017"

        private const val BUNDLE_ID = "foo@bar.com"

        // Default chunk of byte used in various test cases
        private const val BYTES: Long = 100

        // Request ID used by OCS gateway to correlate responses with requests
        private val REQUEST_ID = RandomStringUtils.randomAlphanumeric(22)

        private const val NO_OF_BYTES_TO_ADD = 10000L

        private const val TIMEOUT_IN_SECONDS = 10L

        private const val ONE_SECOND_IN_MILLISECONDS = 1000L

        /**
         * This is the "disruptor" (processing engine) that will process
         * PrimeEvent instances and send them through a sequence of operations that
         * will update the balance of bytes for a particular subscription.
         *
         *
         * Disruptor also provides RingBuffer, which is used by Producer
         */
        private lateinit var disruptor: OcsDisruptor

        /**
         *
         */
        private lateinit var producer: EventProducerImpl

        /**
         * The gRPC service that will produce incoming events from the
         * simulated packet gateway, contains an [OcsSubscriberService] instance bound
         * to a particular port (in our case 8082).
         */
        private lateinit var ocsServer: OcsGrpcServer

        /**
         * The "sub" that will mediate access to the GRPC channel,
         * providing an API to send / receive data to/from it.
         */
        private lateinit var ocsServiceStub: OcsServiceStub

        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/integration-tests/resources/docker-compose.yaml")
                .waitingForService("neo4j", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("neo4j",
                        HealthChecks.toRespond2xxOverHttp(7474) {
                            port -> port.inFormat("http://\$HOST:\$EXTERNAL_PORT/browser")
                        },
                        Duration.standardSeconds(20L))
                .build()

        @BeforeClass
        @JvmStatic
        @Throws(IOException::class)
        fun setUp() {
            ConfigRegistry.config = Config().apply {
                this.host = "0.0.0.0"
                this.protocol = "bolt"
            }
            initFirebaseConfigRegistry()

            Neo4jClient.start()

            // Set up processing pipeline
            disruptor = OcsDisruptor()
            producer = EventProducerImpl(disruptor.disruptor.ringBuffer)

            // Set up the gRPC server at a particular port with a particular
            // service, that is connected to the processing pipeline.
            val ocsService = OcsService(producer)
            ocsServer = OcsGrpcServer(PORT, ocsService.asOcsServiceImplBase())

            val ocsState = OcsState()
            ocsState.msisdnToBundleIdMap[MSISDN] = BUNDLE_ID
            ocsState.bundleIdToMsisdnMap[BUNDLE_ID] = mutableSetOf(MSISDN)

            ocsState.addDataBundleBytesForMsisdn(MSISDN, NO_OF_BYTES_TO_ADD)

            // Events flow:
            //      Producer:(OcsService, Subscriber)
            //          -> Handler:(OcsState)
            //              -> Handler:(OcsService, Subscriber)
            disruptor.disruptor.handleEventsWith(ocsState).then(ocsService.asEventHandler())

            // start disruptor and ocs services.
            disruptor.start()
            ocsServer.start()

            // Set up a channel to be used to communicate as an OCS instance, to an
            // Prime instance.
            val channel = ManagedChannelBuilder
                    .forTarget("0.0.0.0:$PORT")
                    .usePlaintext() // disable encryption for testing
                    .build()

            // Initialize the stub that will be used to actually
            // communicate from the client emulating being the OCS.
            ocsServiceStub = OcsServiceGrpc.newStub(channel)
        }

        @AfterClass
        @JvmStatic
        @Throws(InterruptedException::class, TimeoutException::class)
        fun tearDown() {
            disruptor.stop()
            ocsServer.forceStop()
            Neo4jClient.stop()
        }
    }
}
