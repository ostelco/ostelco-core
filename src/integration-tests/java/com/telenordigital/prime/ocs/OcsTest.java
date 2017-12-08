package com.telenordigital.prime.ocs;

import com.telenordigital.prime.disruptor.PrimeDisruptor;
import com.telenordigital.prime.disruptor.PrimeEventProducer;
import com.telenordigital.prime.ocs.OcsServiceGrpc.OcsServiceStub;
import com.telenordigital.prime.ocs.state.OcsState;
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
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 * <p>
 * This class tests the packet gateway's perspective of talking to
 * the OCS over the gRPC-generated wire protocol.
 */
public class OcsTest {

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
    public static void setup() throws IOException {

        // Set up processing pipeline
        disruptor = new PrimeDisruptor();
        producer = new PrimeEventProducer(disruptor.getDisruptor().getRingBuffer());

        // Set up the gRPC server at a particular port with a particular
        // service, that is connected to the processing pipeline.
        final OcsService ocsService = new OcsService(producer);
        ocsServer = new OcsServer(PORT, ocsService);

        // We're just adding some bytes to it (1000 as it were since BYTES is 100)
        final OcsState ocsState = new OcsState();
        ocsState.addDataBytes(MSISDN, 10 * BYTES);

        // Events flow:
        //      Producer:(OcsService, Subscriber)
        //          -> Handler:(OcsState)
        //              -> Handler:(OcsService, Subscriber)
        disruptor.getDisruptor().handleEventsWith(ocsState).then(ocsService);

        // start disruptor and ocs services.
        disruptor.start();
        ocsServer.start();

        // Set up a channel to be used to communicate as an OCS instance, to an
        // Prime instance.
        final ManagedChannel channel = ManagedChannelBuilder
                .forTarget("0.0.0.0:" + PORT)
                .usePlaintext(true) // disable encryption for testing
                .build();

        // Initialize the stub that will be used to actually
        // communicate from the client emulating being the OCS.
        ocsServiceStub = OcsServiceGrpc.newStub(channel);
    }

    public abstract static class AbstactObserver<T> implements StreamObserver<T> {
        @Override
        public final void onError(Throwable t) {
            // Ignore errors
        }

        @Override
        public final void onCompleted() {
            LOG.info("Completed");
        }
    }

    private FetchDataBucketInfo newDefaultFetchDataInfoRecord() {
        LOG.info("Req Id: {}", REQUEST_ID);
        return FetchDataBucketInfo.newBuilder()
                .setMsisdn(MSISDN)
                .setBytes(BYTES)
                .setRequestId(REQUEST_ID)
                .build();
    }

    private ReturnUnusedDataRequest newDefaultReturnUnusedDataRequest() {
        return ReturnUnusedDataRequest.newBuilder()
                .setMsisdn(MSISDN)
                .setBytes(BYTES)
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
        final StreamObserver<FetchDataBucketInfo> fetchDataBucketRequests =
                ocsServiceStub.fetchDataBucket(
                        new AbstactObserver<FetchDataBucketInfo>() {
                            @Override
                            public void onNext(final FetchDataBucketInfo response) {
                                LOG.info("Received data bucket of {} bytes for {}",
                                        response.getBytes(), response.getMsisdn());
                                assertEquals(MSISDN, response.getMsisdn());
                                assertEquals(BYTES, response.getBytes());
                                assertEquals(REQUEST_ID, response.getRequestId());
                                cdl.countDown();
                            }
                        });

        // Simulate packet gateway requesting a new bucket of data by
        // injecting a packet of data into the "onNext" method declared
        // above.
        fetchDataBucketRequests.onNext(newDefaultFetchDataInfoRecord());

        // Wait for response (max ten seconds) and the pass the test only
        // if a response was actually generated (and cdl counted down to zero).
        cdl.await(10, TimeUnit.SECONDS);

        fetchDataBucketRequests.onCompleted();

        assertEquals(0, cdl.getCount());
    }


    /**
     * Test returning data from the OCS to the BSS.
     *
     * @throws InterruptedException
     */
    @Test
    public void testReturnUnusedData() throws InterruptedException {

        final CountDownLatch cdl = new CountDownLatch(1);

        final StreamObserver<ReturnUnusedDataRequest> returnUnusedDataRequests =
                ocsServiceStub.returnUnusedData(
                        new AbstactObserver<ReturnUnusedDataResponse>() {

                            @Override
                            public void onNext(ReturnUnusedDataResponse response) {
                                LOG.info("Returned unsed data for {}", response.getMsisdn());
                                assertEquals(MSISDN, response.getMsisdn());
                                cdl.countDown();
                            }
                        });

        // Send a new record  of some amount of data associated with an MSISDN
        // to the receiver at the other end of the stub's connection
        returnUnusedDataRequests.onNext(newDefaultReturnUnusedDataRequest());

        // Wait for response (max ten seconds) and the pass the test only
        // if a response was actually generated (and cdl counted down to zero).
        cdl.await(5, TimeUnit.SECONDS);

        returnUnusedDataRequests.onCompleted();

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
                    public void onNext(ActivateResponse response) {
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
        Thread.sleep(1000);

        // Send a report using the producer to the pipeline that will
        // inject a PrimeEvent that will top up the data bundle balance.
        producer.topupDataBundleBalanceEvent(MSISDN, 10 * BYTES);

        // Now wait, again, for the latch to reach zero, and fail the test
        // ff it hasn't.
        cdl.await(5, TimeUnit.SECONDS);
        assertEquals(0, cdl.getCount());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (ocsServer != null) {
            ocsServer.stop();
        }
        if (disruptor != null) {
            disruptor.stop();
        }
    }
}
