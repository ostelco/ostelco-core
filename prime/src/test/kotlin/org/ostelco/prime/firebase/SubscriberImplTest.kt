package org.ostelco.prime.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ostelco.prime.firebase.entities.asMap
import org.ostelco.prime.model.Subscriber

class SubscriberImplTest {

    private val MSISDN = "+4712345678"
    private val NO_OF_BYTES_LEFT = 123823838L

    companion object {
        private const val NO_OF_BYTES_LEFT_KEY = "noOfBytesLeft"
        private const val MSISDN_KEY = "msisdn"
    }

    private val fbs = Subscriber(MSISDN, NO_OF_BYTES_LEFT)

    @Test
    fun asMap() {
        assertEquals(2, fbs.asMap().size.toLong())
        assertTrue(fbs.asMap().containsKey(NO_OF_BYTES_LEFT_KEY))
        assertTrue(fbs.asMap().containsKey(MSISDN_KEY))
        assertEquals(MSISDN, fbs.asMap()[MSISDN_KEY])
    }

    @Test
    fun getAndSetNoOfBytesLeft() {
        val noOfBytesLeft = 123823838L
        assertEquals(noOfBytesLeft, fbs.noOfBytesLeft)
    }

    @Test
    fun getAndSetMsisdn() {
        val msisdn = "+4712345678"
        assertEquals(msisdn, fbs.msisdn)
    }
}
