package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PrimeEventProducerTest {


    private static final long NO_OF_TOPUP_BYTES = 991234L;

    private  static final String MSISDN = "+4711223344";

    private  PrimeEventProducer pep;



    private RingBuffer<PrimeEvent> ringBuffer;

    private PrimeDisruptor disruptor;


    @Before
    public void setUp() {
        this.disruptor = new PrimeDisruptor();
        this.pep = new PrimeEventProducer(disruptor.getDisruptor().getRingBuffer());
        disruptor.getDisruptor();
        disruptor.start();
    }

    @After
    public void shutDown() throws TimeoutException {
       disruptor.stop();
    }

    @Test
    public void topupDataBundleBalanceEvent() throws Exception {
        pep.topupDataBundleBalanceEvent(MSISDN, NO_OF_TOPUP_BYTES);
    }

    @Test
    public void returnUnusedDataBucketEvent() throws Exception {
        final String msisdn = "22222";
        final long bytes = 999L;
        final String streamId = "xxxx";

        pep.returnUnusedDataBucketEvent(msisdn, bytes, streamId);

        // XXX Add some verification for this test to make sense
    }

    @Test
    public void fetchDataBucketEvent() throws Exception {
        // TBD
        // pep.fetchDataBucketEvent(request, streamid);
    }

}