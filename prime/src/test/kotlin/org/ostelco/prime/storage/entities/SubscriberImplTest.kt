package org.ostelco.prime.storage.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriberImplTest {

    companion object {
        private const val NO_OF_BYTES_LEFT_KEY = "noOfBytesLeft"
        private const val MSISDN_KEY = "msisdn"
    }

    private val fbs = SubscriberImpl()

    @Test
    fun asMap() {
        assertEquals(2, fbs.asMap().size.toLong())
        assertTrue(fbs.asMap().containsKey(NO_OF_BYTES_LEFT_KEY))
        assertTrue(fbs.asMap().containsKey(MSISDN_KEY))

        assertEquals(0L, fbs.asMap()[NO_OF_BYTES_LEFT_KEY])
        assertEquals(null, fbs.asMap()[MSISDN_KEY])
    }

    @Test
    fun getAndSetNoOfBytesLeft() {
        assertEquals(0L, fbs.noOfBytesLeft)
        val noOfBytesLeft = 123823838L
        fbs.setNoOfBytesLeft(noOfBytesLeft)
        assertEquals(noOfBytesLeft, fbs.noOfBytesLeft)
    }

    @Test
    fun getAndSetMsisdn() {
        assertEquals(null, fbs.msisdn)
        val msisdn = "+47123456"
        fbs.setMsisdn(msisdn)
        assertEquals(msisdn, fbs.msisdn)
    }
}
