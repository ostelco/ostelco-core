package com.telenordigital.prime.ocs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Test the evolution of the OCS state when adding and consuming
 * bytes through topup, consuming and returning u
 */
public class OcsStateTest {

    private static final String MSISDN = "MSISDN";

    private static final int INITIAL_NUMBER_OF_BYTES_TO_ADD = 1000;

    private static final int TOPUP_NUMBER_OF_BYTES_TO_ADD = 500;

    private static final int FINAL_NUMBER_OF_BYTES =
            INITIAL_NUMBER_OF_BYTES_TO_ADD + TOPUP_NUMBER_OF_BYTES_TO_ADD;

    private static final int INITIAL_NUMBER_OF_BYTES_TO_REQUEST = 700;

    private static final int SECOND_NUMBER_OF_BYTES_TO_REQUEST = 400;


    @Test
    public void testAddDataBytes() {

        final OcsState ocsState = new OcsState();

        // Add a thousand, starting from zero. This means that the addDatBytes will
        // return the  new balande (after addition), which is 1000.
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD,
                ocsState.addDataBytes(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_ADD));

        // Just checking that the balance is still 1000.
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD, ocsState.getDataBytes(MSISDN));

        // Adding 500, should increase balance up to 1500  ;-)
        assertEquals(FINAL_NUMBER_OF_BYTES,
                ocsState.addDataBytes(MSISDN, TOPUP_NUMBER_OF_BYTES_TO_ADD));

        // And we should still have FINAL_NUMBER_OF_BYTES (1500).
        assertEquals(FINAL_NUMBER_OF_BYTES,
                ocsState.getDataBytes(MSISDN));
    }

    @Test
    public void testConsumeDataBytes() {

        final OcsState ocsState = new OcsState();

        // First store a thousand
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD,
                ocsState.addDataBytes(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_ADD));

        // Then request, and get 700
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_REQUEST,
                ocsState.consumeDataBytes(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_REQUEST));

        // Now request 400, but that's too much, so only 300 is returned, and
        // after this transaction the balance is zero.
        final int expectedPermittedReturn = 300;
        assertEquals(expectedPermittedReturn,
                ocsState.consumeDataBytes(MSISDN, SECOND_NUMBER_OF_BYTES_TO_REQUEST));

        //... so at this point even requesting a single byte will fail.
        assertEquals(0, ocsState.consumeDataBytes(MSISDN, 1));
    }


    @Test
    public void testStripLeadingPlus() {
        assertEquals("foo", OcsState.stripLeadingPlus("foo"));
        final String string;
        string = OcsState.stripLeadingPlus("+foo");
        assertEquals("foo", string);
    }
}
