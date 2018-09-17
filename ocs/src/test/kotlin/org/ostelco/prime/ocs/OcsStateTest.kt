package org.ostelco.prime.ocs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 *
 *
 * Test the evolution of the OCS state when adding and consuming
 * bytes through topup, consuming and returning u
 */
class OcsStateTest {

    @Test
    fun testAddDataBytes() {

        val ocsState = OcsState(loadSubscriberInfo = false)

        ocsState.msisdnToBundleIdMap[MSISDN] = BUNDLE_ID

        // Add a thousand, starting from zero. This means that the addDataBytes will
        // return the  new balance (after addition), which is 1000.
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD,
                ocsState.addDataBundleBytesForMsisdn(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_ADD))

        // Just checking that the balance is still 1000.
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD, ocsState.getDataBundleBytes(MSISDN))

        // Adding 500, should increase balance up to 1500  ;-)
        assertEquals(FINAL_NUMBER_OF_BYTES,
                ocsState.addDataBundleBytesForMsisdn(MSISDN, TOPUP_NUMBER_OF_BYTES_TO_ADD))

        // And we should still have FINAL_NUMBER_OF_BYTES (1500).
        assertEquals(FINAL_NUMBER_OF_BYTES, ocsState.getDataBundleBytes(MSISDN))
    }

    @Test
    fun testConsumeDataBytes() {

        val ocsState = OcsState(loadSubscriberInfo = false)

        ocsState.msisdnToBundleIdMap[MSISDN] = BUNDLE_ID

        // First store a thousand
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD,
                ocsState.addDataBundleBytesForMsisdn(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_ADD))

        // Then reserve, and get 700
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_REQUEST,
                ocsState.reserveDataBytes(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_REQUEST))

        // Then consume 700 from the reserved
        assertEquals(REMAINING_BYTES,
                ocsState.consumeDataBytes(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_REQUEST))

        // Now request 400, but that's too much, so only 300 is returned, and
        // after this transaction the balance is zero.
        assertEquals(REMAINING_BYTES,
                ocsState.reserveDataBytes(MSISDN, SECOND_NUMBER_OF_BYTES_TO_REQUEST))

        // Now consume a bit more then resumed (P-GW is allowed to overconsume small amount)
        // Balance should now be 0
        assertEquals(0,
                ocsState.consumeDataBytes(MSISDN, SECOND_NUMBER_OF_BYTES_TO_REQUEST + 45))

        //... so at this point even reserving a single byte will fail.
        assertEquals(0, ocsState.reserveDataBytes(MSISDN, 1))
    }


    @Test
    fun testOverConsumtionDataBytes() {

        val ocsState = OcsState(loadSubscriberInfo = false)

        ocsState.msisdnToBundleIdMap[MSISDN] = BUNDLE_ID

        // First store a thousand
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD,
                ocsState.addDataBundleBytesForMsisdn(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_ADD))

        // Then reserve, and get 700
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_REQUEST,
                ocsState.reserveDataBytes(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_REQUEST))

        // Then consume 750 from the reserved
        assertEquals(OVER_CONSUME_REMAINING_BYTES,
                ocsState.consumeDataBytes(MSISDN, OVER_CONSUME_BUCKET))

        // Now request 400, but that's too much, so only 250 is returned, and
        // after this transaction the balance is zero.
        assertEquals(OVER_CONSUME_REMAINING_BYTES,
                ocsState.reserveDataBytes(MSISDN, SECOND_NUMBER_OF_BYTES_TO_REQUEST))

        //... so at this point even reserving a single byte will fail.
        assertEquals(0, ocsState.reserveDataBytes(MSISDN, 1))
    }


    @Test
    fun testReleaseDataBytes() {

        val ocsState = OcsState(loadSubscriberInfo = false)

        ocsState.msisdnToBundleIdMap[MSISDN] = BUNDLE_ID

        // First store a thousand
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD,
                ocsState.addDataBundleBytesForMsisdn(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_ADD))

        // Then reserve, and get 700
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_REQUEST,
                ocsState.reserveDataBytes(MSISDN, INITIAL_NUMBER_OF_BYTES_TO_REQUEST))

        // Checking that the balance is 300.
        assertEquals(REMAINING_BYTES, ocsState.getDataBundleBytes(MSISDN))

        // Then release the reserved bucket, and get 700 as the released size
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_REQUEST, ocsState.releaseReservedBucket(MSISDN))

        // Checking that the balance is back to 1000.
        assertEquals(INITIAL_NUMBER_OF_BYTES_TO_ADD, ocsState.getDataBundleBytes(MSISDN))
    }

    companion object {

        private const val BUNDLE_ID = "foo@bar.com"
        private const val MSISDN = "MSISDN"

        private const val INITIAL_NUMBER_OF_BYTES_TO_ADD = 1000L

        private const val TOPUP_NUMBER_OF_BYTES_TO_ADD = 500L

        private const val FINAL_NUMBER_OF_BYTES = INITIAL_NUMBER_OF_BYTES_TO_ADD + TOPUP_NUMBER_OF_BYTES_TO_ADD

        private const val INITIAL_NUMBER_OF_BYTES_TO_REQUEST = 700L
        private const val OVER_CONSUME_BUCKET = 750L

        private const val REMAINING_BYTES = 300L
        private const val OVER_CONSUME_REMAINING_BYTES = 250L

        private const val SECOND_NUMBER_OF_BYTES_TO_REQUEST = 400L
    }
}
