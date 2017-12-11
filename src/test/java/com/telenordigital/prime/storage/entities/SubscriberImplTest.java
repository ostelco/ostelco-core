package com.telenordigital.prime.storage.entities;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SubscriberImplTest {

    private static final String NO_OF_BYTES_LEFT_KEY = "noOfBytesLeft";

    private static final String MSISDN_KEY = "msisdn";

    private final SubscriberImpl fbs = new SubscriberImpl();

    @Test
    public void asMap() throws Exception {
        assertEquals(2, fbs.asMap().size());
        assertTrue(fbs.asMap().containsKey(NO_OF_BYTES_LEFT_KEY));
        assertTrue(fbs.asMap().containsKey(MSISDN_KEY));

        assertEquals(0L, fbs.asMap().get(NO_OF_BYTES_LEFT_KEY));
        assertEquals(null, fbs.asMap().get(MSISDN_KEY));
    }

    @Test
    public void getAndSetFbKey() throws Exception {
        assertEquals(null, fbs.getFbKey());
        final String fbkey = "foobar";
        fbs.setFbKey(fbkey);
        assertEquals(fbkey, fbs.getFbKey());
    }

    @Test
    public void getAndSetNoOfBytesLeft() throws Exception {
        assertEquals(0L, fbs.getNoOfBytesLeft());
        final long noOfBytesLeft = 123823838L;
        fbs.setNoOfBytesLeft(noOfBytesLeft);
        assertEquals(noOfBytesLeft, fbs.getNoOfBytesLeft());
    }

    @Test
    public void getAndSetMsisdn() throws Exception {
        assertEquals(null, fbs.getMsisdn());
        final String msisdn = "+47123456";
        fbs.setMsisdn(msisdn);
        assertEquals(msisdn, fbs.getMsisdn());
    }
}

