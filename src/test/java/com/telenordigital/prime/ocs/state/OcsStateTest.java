package com.telenordigital.prime.ocs.state;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 *
 * Test the evolution of the OCS state when adding and consuming
 * bytes through topup, consuming and returning u
 */
public class OcsStateTest {

    private static final String MSISDN = "MSISDN";

    @Test
    public void testAddDataBytes() throws Exception {

        final OcsState ocsState = new OcsState();

        // Add a thousand, starting from zero. This means that the addDatBytes will
        // return the  new balande (after addition), which is 1000.
        assertEquals(1000, ocsState.addDataBytes(MSISDN, 1000));

        // Just checking that the balance is still 1000.
        assertEquals(1000, ocsState.getDataBytes(MSISDN));

        // Adding 500, should increase balance up to 50 ;-)
        assertEquals(1500, ocsState.addDataBytes(MSISDN, 500));

        // And we should still hve 1500.
       assertEquals(1500, ocsState.getDataBytes(MSISDN));
    }

    @Test
    public void testConsumeDataBytes() throws Exception {

        final OcsState ocsState = new OcsState();

        // First store a thousand
        assertEquals(1000, ocsState.addDataBytes(MSISDN, 1000));

        // Then request, and get 700
        assertEquals(700, ocsState.consumeDataBytes(MSISDN, 700));

        // Now request 400, but that's too much, so only 300 is returned, and
        // after this transaction the balance is zero.
        assertEquals(300, ocsState.consumeDataBytes(MSISDN, 400));

        //... so at this point even requesting a single byte will fail.
        assertEquals(0, ocsState.consumeDataBytes(MSISDN, 1));
    }
}
