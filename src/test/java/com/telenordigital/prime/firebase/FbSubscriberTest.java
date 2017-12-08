package com.telenordigital.prime.firebase;

import org.junit.Test;

import static org.junit.Assert.*;

public class FbSubscriberTest {
    @Test
    public void asMap() throws Exception {
        final FbSubscriber fbs = new FbSubscriber();
        assertEquals(2, fbs.asMap().size());
        assertTrue(fbs.asMap().containsKey("noOfBytesLeft"));
        assertTrue(fbs.asMap().containsKey("msisdn"));

        assertEquals(0L, fbs.asMap().get("noOfBytesLeft"));
        assertEquals(null, fbs.asMap().get("msisdn"));
    }

    @Test
    public void getFbKey() throws Exception {
    }

    @Test
    public void getNoOfBytesLeft() throws Exception {
    }

    @Test
    public void getMsisdn() throws Exception {
    }

    @Test
    public void setFbKey() throws Exception {
    }

    @Test
    public void setMsisdn() throws Exception {
    }

    @Test
    public void setNoOfBytesLeft() throws Exception {
    }
}