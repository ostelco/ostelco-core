package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.telenordigital.prime.ocs.CreditControlRequestInfo;
import com.telenordigital.prime.ocs.MultipleServiceCreditControl;
import com.telenordigital.prime.ocs.ReguestedServiceUnit;
import com.telenordigital.prime.ocs.UsedServiceUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.telenordigital.prime.disruptor.PrimeEventMessageType.CREDIT_CONTROL_REQUEST;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PrimeEventProducerTest {

    private static final long NO_OF_TOPUP_BYTES = 991234L;

    private static final String MSISDN = "+4711223344";

    private static final String STREAM_ID = "mySecret stream";

    private static final int RING_BUFFER_SIZE = 256;

    private static final int TIMEOUT = 10;

    private  PrimeEventProducer pep;

    private Disruptor<PrimeEvent> disruptor;

    private CountDownLatch cdl;

    private Set<PrimeEvent> result;


    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        this.disruptor = new Disruptor<PrimeEvent>(
            ()-> new PrimeEvent(),
            RING_BUFFER_SIZE,
            Executors.defaultThreadFactory() );
        final RingBuffer<PrimeEvent> ringBuffer = disruptor.getRingBuffer();
        this.pep = new PrimeEventProducer(ringBuffer);

        this.cdl = new CountDownLatch(1);
        this.result = new HashSet<>();
        final EventHandler<PrimeEvent> eh = (event, sequence, endOfBatch) -> {
            result.add(event);
            cdl.countDown();
        };

        //noinspection unchecked
        disruptor.handleEventsWith(eh);
        disruptor.start();
    }

    private PrimeEvent getCollectedEvent()  throws InterruptedException {
        // Wait  wait a short while for the thing to process.
        assertTrue(cdl.await(TIMEOUT, TimeUnit.SECONDS));
        assertFalse(result.isEmpty());
        final PrimeEvent event = result.iterator().next();
        assertNotNull(event);
        return event;
    }

    @After
    public void shutDown() {
        disruptor.shutdown();
    }

    @Test
    public void topupDataBundleBalanceEvent() throws Exception {

        // Stimulating a response
        pep.topupDataBundleBalanceEvent(MSISDN, NO_OF_TOPUP_BYTES);

        // Collect an event (or fail trying).
        final PrimeEvent event = getCollectedEvent();

        // Verify some behavior
        assertEquals(MSISDN, event.getMsisdn());
        assertEquals(NO_OF_TOPUP_BYTES, event.getRequestedBucketBytes());
        assertEquals(TOPUP_DATA_BUNDLE_BALANCE, event.getMessageType());
    }

    @Test
    public void creditControlRequestEvent() throws Exception {
        final CreditControlRequestInfo request =
                CreditControlRequestInfo.
                        newBuilder().
                        setMsisdn(MSISDN).
                        addMscc(MultipleServiceCreditControl.newBuilder()
                                .setRequested(ReguestedServiceUnit.newBuilder()
                                        .setTotalOctets(NO_OF_TOPUP_BYTES)
                                        .build())
                                .setUsed(UsedServiceUnit.newBuilder().setTotalOctets(0L).build())
                                .setRatingGroup(10)
                                .setServiceIdentifier(1)
                                .build()
                        ).build();

        pep.injectCreditControlRequestIntoRingbuffer(request, STREAM_ID);

        final PrimeEvent event = getCollectedEvent();
        assertEquals(MSISDN, event.getMsisdn());
        assertEquals(NO_OF_TOPUP_BYTES, event.getRequestedBucketBytes());
        assertEquals(0L, event.getUsedBucketBytes());
        assertEquals(10, event.getRatingGroup());
        assertEquals(1, event.getServiceIdentifier());
        assertEquals(STREAM_ID, event.getOcsgwStreamId());
        assertEquals(CREDIT_CONTROL_REQUEST, event.getMessageType());
    }
}

