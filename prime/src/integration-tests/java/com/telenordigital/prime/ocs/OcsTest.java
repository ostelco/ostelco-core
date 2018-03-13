package com.telenordigital.prime.ocs;

import com.lmax.disruptor.TimeoutException;
import com.telenordigital.prime.disruptor.PrimeDisruptor;
import com.telenordigital.prime.disruptor.PrimeEventProducer;
import com.telenordigital.prime.ocs.OcsServiceGrpc.OcsServiceStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * This class tests the packet gateway's perspective of talking to
 * the OCS over the gRPC-generated wire protocol.
 */
public final class OcsTest {

    private static final Logger LOG = LoggerFactory.getLogger(OcsTest.class);

    /**
     * The port on which the gRPC service will be receiving incoming
     * connections.
     */
    private static final int PORT = 8082;

    /**
     * The phone numbe for which we're faking data consumption during this test.
     */
    private static final String MSISDN = "4790300017";

    // Default chunk of byte used in various test cases
    private static final int BYTES = 100;

    // Request ID used by OCS gateway to correlate responses with requests
    private static final String REQUEST_ID = RandomStringUtils.randomAlphanumeric(22);

    private static final int NO_OF_BYTES_TO_ADD = 10000;

    private static final int TIMEOUT_IN_SECONDS = 10;

    private static final int ONE_SECOND_IN_MILLISECONDS = 1000;

    /**
     * This is the "disruptor" (processing engine) that will process
     * PrimeEvent instances and send them through a sequence of operations that
     * will update the balance of bytes for a particular subscription.
     * <p>
     * Disruptor also provides RingBuffer, which is used by Producer
     */
    private static PrimeDisruptor disruptor;

    /**
     *
     */
    private static PrimeEventProducer producer;

    /**
     * The gRPC service that will produce incoming events from the
     * simulated packet gateway, contains an {@link OcsService} instance bound
     * to a particular port (in our case 8082).
     */
    private static OcsServer ocsServer;

    /**
     * The "sub" that will mediate access to the GRPC channel,
     * providing an API to send / receive data to/from it.
     */
    private static OcsServiceStub ocsServiceStub;

    @BeforeClass
    public static void setUp() throws IOException {

        // Set up processing pipeline
        disruptor = new PrimeDisruptor();
        producer = new PrimeEventProducer(disruptor.getDisruptor().getRingBuffer());

        // Set up the gRPC server at a particular port with a particular
        // service, that is connected to the processing pipeline.
        final OcsService ocsService = new OcsService(producer);
        ocsServer = new OcsServer(PORT, ocsService.asOcsServiceImplBase());

        final OcsState ocsState = new OcsState();
        ocsState.addDataBundleBytes(MSISDN, NO_OF_BYTES_TO_ADD);

        // Events flow:
        //      Producer:(OcsService, Subscriber)
        //          -> Handler:(OcsState)
        //              -> Handler:(OcsService, Subscriber)
        disruptor.getDisruptor().handleEventsWith(ocsState).then(ocsService.asEventHandler());

        // start disruptor and ocs services.
        disruptor.start();
        ocsServer.start();

        // Set up a channel to be used to communicate as an OCS instance, to an
        // Prime instance.
        final ManagedChannel channel = ManagedChannelBuilder.
                forTarget("0.0.0.0:" + PORT).
                usePlaintext(true). // disable encryption for testing
                build();

        // Initialize the stub that will be used to actually
        // communicate from the client emulating being the OCS.
        ocsServiceStub = OcsServiceGrpc.newStub(channel);
    }

    public abstract static class AbstactObserver<T> implements StreamObserver<T> {
        @Override
        public final void onError(final Throwable t) {
            // Ignore errors
        }

        @Override
        public final void onCompleted() {
            LOG.info("Completed");
        }
    }

    private CreditControlRequestInfo newDefaultCreditControlRequestInfo() {
        LOG.info("Req Id: {}", REQUEST_ID);
        return CreditControlRequestInfo.newBuilder()
                .setMsisdn(MSISDN)
                .setRequestId(REQUEST_ID)
                .build();
    }

    /**
     * This whole test case tests the packet gateway talking to the OCS over
     * the gRPC interface.
     */
    @Test
    public void testFetchDataRequest() throws InterruptedException {

        // If this latch reaches zero, then things went well.
        final CountDownLatch cdl = new CountDownLatch(1);

        // Simulate being the OCS receiving a packet containing
        // information about a data bucket containing a number
        // of bytes for some MSISDN.
        final StreamObserver<CreditControlRequestInfo> requests =
                ocsServiceStub.creditControlRequest(
                        new AbstactObserver<CreditControlAnswerInfo>() {
                            @Override
                            public void onNext(final CreditControlAnswerInfo response) {
                                LOG.info("Received data bucket of {} bytes for {}",
                                        response.getMsccOrBuilderList().get(0).getGranted().getTotalOctets(),
                                        response.getMsisdn());
                                assertEquals(MSISDN, response.getMsisdn());
                                assertEquals(BYTES, response.getMsccOrBuilderList().get(0).getGranted().getTotalOctets());
                                assertEquals(REQUEST_ID, response.getRequestId());
                                cdl.countDown();
                            }
                        });

        // Simulate packet gateway requesting a new bucket of data by
        // injecting a packet of data into the "onNext" method declared
        // above.
        requests.onNext(newDefaultCreditControlRequestInfo());

        // Wait for response (max ten seconds) and the pass the test only
        // if a response was actually generated (and cdl counted down to zero).
        cdl.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

        requests.onCompleted();

        assertEquals(0, cdl.getCount());
    }

    /**
     * Simulate sending a request to activate a subscription.
     *
     * @throws InterruptedException
     */
    @Test
    public void testActivateMsisdn() throws InterruptedException {

        final CountDownLatch cdl = new CountDownLatch(2);

        final StreamObserver<ActivateResponse> streamObserver =
            new AbstactObserver<ActivateResponse>() {
                @Override
                public void onNext(final ActivateResponse response) {
                    if (!response.getMsisdn().isEmpty()) {
                        LOG.info("Activate {}", response.getMsisdn());
                        assertEquals(MSISDN, response.getMsisdn());
                    }
                    cdl.countDown();
                }
            };

        // Get the default (singelton) instance of an activation request.
        final ActivateRequest activateRequest = ActivateRequest.getDefaultInstance();

        // Send it over the wire, but also send  stream observer that will be
        // invoked when a reply comes back.
        ocsServiceStub.activate(activateRequest, streamObserver);

        // Wait for a second to let things get through, then move on.
        Thread.sleep(ONE_SECOND_IN_MILLISECONDS);

        // Send a report using the producer to the pipeline that will
        // inject a PrimeEvent that will top up the data bundle balance.
        producer.topupDataBundleBalanceEvent(MSISDN, NO_OF_BYTES_TO_ADD);

        // Now wait, again, for the latch to reach zero, and fail the test
        // ff it hasn't.
        cdl.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        assertEquals(0, cdl.getCount());
    }

    @AfterClass
    public static void tearDown() throws InterruptedException, TimeoutException {
        if (ocsServer != null) {
            ocsServer.stop();
        }
        if (disruptor != null) {
            disruptor.stop();
        }
    }
}
