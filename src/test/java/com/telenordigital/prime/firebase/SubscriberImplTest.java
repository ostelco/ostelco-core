package com.telenordigital.prime.firebase;

import org.junit.Test;

import static org.junit.Assert.*;

public class SubscriberImplTest {

    private final SubscriberImpl fbs = new SubscriberImpl();

    @Test
    public void asMap() throws Exception {
        assertEquals(2, fbs.asMap().size());
        assertTrue(fbs.asMap().containsKey("noOfBytesLeft"));
        assertTrue(fbs.asMap().containsKey("msisdn"));

        assertEquals(0L, fbs.asMap().get("noOfBytesLeft"));
        assertEquals(null, fbs.asMap().get("msisdn"));
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