package com.telenordigital.prime.ocs;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.telenordigital.prime.disruptor.PrimeEvent;
import com.telenordigital.prime.disruptor.PrimeEventProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OcsServiceTest {

    private static final int RING_BUFFER_SIZE = 256;

    private static final long TIMEOUT =  10;

    private Disruptor<PrimeEvent> disruptor;

    private CountDownLatch cdl;

    private HashSet<PrimeEvent> result;

    private OcsService service;

    @Before
    public void setUp() {
        this.disruptor = new Disruptor<>(
            ()-> new PrimeEvent(),
            RING_BUFFER_SIZE,
            Executors.defaultThreadFactory() );
        final RingBuffer<PrimeEvent> ringBuffer = disruptor.getRingBuffer();
        PrimeEventProducer pep = new PrimeEventProducer(ringBuffer);

        this.cdl = new CountDownLatch(1);
        this.result = new HashSet<>();
        final EventHandler<PrimeEvent> eh = (event, sequence, endOfBatch) -> {
            result.add(event);
            cdl.countDown();
        };

        //noinspection unchecked
        disruptor.handleEventsWith(eh);
        disruptor.start();
        this.service = new OcsService(pep);
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
    public void onEvent() {
        // tbd
    }


    @Test
    public void fetchDataBucket() {
        // tbd
    }

    @Test
    public void returnUnusedData() {
        // tbd
    }

    @Test
    public void activate() {
        // tbd
    }
}