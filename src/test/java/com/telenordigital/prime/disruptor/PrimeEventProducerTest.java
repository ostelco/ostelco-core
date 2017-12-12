package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.telenordigital.prime.disruptor.PrimeEventMessageType.RETURN_UNUSED_DATA_BUCKET;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PrimeEventProducerTest {


    private static final long NO_OF_TOPUP_BYTES = 991234L;

    private  static final String MSISDN = "+4711223344";

    private  PrimeEventProducer pep;

    private Disruptor<PrimeEvent> disruptor;


    private RingBuffer<PrimeEvent> ringBuffer;

    private EventFactory<PrimeEvent> eventFactory;

    private Executor executor;

    private CountDownLatch cdl;
    private Set<PrimeEvent> result;


    @Before
    public void setUp() {
        this.eventFactory = new EventFactory<PrimeEvent>() {
            @Override
            public PrimeEvent newInstance() {
                return new PrimeEvent();
            }
        };

        this.executor = Executors.newCachedThreadPool();
        this.disruptor = new Disruptor<PrimeEvent>(eventFactory, 256,   Executors.defaultThreadFactory() );
        this.ringBuffer = disruptor.getRingBuffer();
        this.pep = new PrimeEventProducer(ringBuffer);

        this.cdl = new CountDownLatch(1);
        this.result = new HashSet<>();
        final EventHandler<PrimeEvent> eh = new EventHandler<PrimeEvent>() {
            @Override
            public void onEvent(final PrimeEvent event, long sequence, boolean endOfBatch) throws Exception {
                result.add(event);
                cdl.countDown();
            }
        };

        disruptor.handleEventsWith(eh);
        disruptor.start();
    }

    private PrimeEvent getCollectedEvent()  throws InterruptedException {
        // Wait  wait a short while for the thing to process.
        assertTrue(cdl.await(10, TimeUnit.SECONDS));
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
        assertEquals(NO_OF_TOPUP_BYTES, event.getBucketBytes());
        assertEquals(TOPUP_DATA_BUNDLE_BALANCE, event.getMessageType());

    }

    @Test
    public void returnUnusedDataBucketEvent() throws Exception {
        final String streamId = "xxxx";

        pep.returnUnusedDataBucketEvent(MSISDN, NO_OF_TOPUP_BYTES, streamId);

        // Collect an event (or fail trying).
        final PrimeEvent event = getCollectedEvent();
        assertEquals(MSISDN, event.getMsisdn());
        assertEquals(NO_OF_TOPUP_BYTES, event.getBucketBytes());
        assertEquals(streamId, event.getOcsgwStreamId());
        assertEquals(RETURN_UNUSED_DATA_BUCKET, event.getMessageType());
    }

    @Test
    public void fetchDataBucketEvent() throws Exception {
        // TBD
        // pep.fetchDataBucketEvent(request, streamid);
    }

}