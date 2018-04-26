package org.ostelco.prime.firebase

import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.events.EventListeners
import org.ostelco.prime.ocs.OcsState
import org.ostelco.prime.storage.Products.DATA_TOPUP_3GB
import org.ostelco.prime.storage.PurchaseRequestHandler
import org.ostelco.prime.storage.Storage
import org.ostelco.prime.storage.StorageException
import org.ostelco.prime.storage.entities.PurchaseRequest
import org.ostelco.prime.storage.entities.PurchaseRequestImpl
import java.lang.Thread.sleep
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FbStorageTest {

    private var fbStorage: FbStorage? = null

    private var storage: Storage? = null

    private var prids: MutableCollection<String>? = null

    @Before
    @Throws(StorageException::class, InterruptedException::class)
    fun setUp() {
        this.fbStorage = FbStorage(
                "pantel-tests",
                "src/integration-tests/resources/pantel-tests.json",
                EventListeners(OcsState()))
        this.storage = fbStorage
        sleep(MILLIS_TO_WAIT_WHEN_STARTING_UP.toLong())
        storage!!.removeSubscriberByMsisdn(EPHERMERAL_MSISDN)
        storage!!.insertNewSubscriber(EPHERMERAL_MSISDN)
        this.prids = ArrayList()
    }

    @After
    @Throws(StorageException::class)
    fun cleanUp() {
        storage!!.removeSubscriberByMsisdn(EPHERMERAL_MSISDN)
        for (prid in prids!!) {
            storage!!.removePurchaseRequestById(prid)
        }
    }

    @Test
    @Throws(StorageException::class)
    fun getStorageByMsisdnTest() {
        val subscriberByMsisdn = storage!!.getSubscriberFromMsisdn(EPHERMERAL_MSISDN)
        assertNotEquals(null, subscriberByMsisdn)
        assertEquals(EPHERMERAL_MSISDN, subscriberByMsisdn!!.msisdn)
    }

    @Test
    @Throws(StorageException::class)
    fun insertNewSubscriberTest() {
        Assert.assertNotEquals(null, storage!!.getSubscriberFromMsisdn(EPHERMERAL_MSISDN))
    }

    @Test
    @Throws(StorageException::class)
    fun setRemainingByMsisdnTest() {
        storage!!.setRemainingByMsisdn(
                EPHERMERAL_MSISDN,
                RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS)
        Assert.assertEquals(RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS,
                storage!!.getSubscriberFromMsisdn(EPHERMERAL_MSISDN)!!.noOfBytesLeft)
        storage!!.setRemainingByMsisdn(EPHERMERAL_MSISDN, 0)
        Assert.assertEquals(0L,
                storage!!.getSubscriberFromMsisdn(EPHERMERAL_MSISDN)!!.noOfBytesLeft)
    }

    @Test
    @Throws(StorageException::class)
    fun updateDisplayDatastructureTest() {
        storage!!.setRemainingByMsisdn(EPHERMERAL_MSISDN,
                RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS)
        storage!!.updateDisplayDatastructure(EPHERMERAL_MSISDN)
        // XXX  Some verification missing, but it looks like the right thing
    }

    @Test
    @Throws(StorageException::class)
    fun addRecordOfPurchaseByMsisdnTest() {
        val now = Instant.now().toEpochMilli()
        val id = storage!!.addRecordOfPurchaseByMsisdn(
                EPHERMERAL_MSISDN,
                DATA_TOPUP_3GB.sku,
                now)
        storage!!.removeRecordOfPurchaseById(id)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testWriteThenReactToUpdateRequest() {

        val latch = CountDownLatch(2)

        storage!!.addPurchaseRequestHandler(
                object : PurchaseRequestHandler {
                    override fun onPurchaseRequest(req: PurchaseRequest) {
                        assertNotEquals(null, req)
                        assertEquals(PAYMENT_TOKEN, req.paymentToken)
                        assertEquals(DATA_TOPUP_3GB.sku, req.sku)
                        latch.countDown()
                    }
                })

        val cr = PurchaseRequestImpl(DATA_TOPUP_3GB, PAYMENT_TOKEN, EPHERMERAL_MSISDN)
        val id = fbStorage!!.injectPurchaseRequest(cr)
        val id2 = fbStorage!!.injectPurchaseRequest(cr)
        prids!!.add(id)
        prids!!.add(id2)

        if (!latch.await(TIMEOUT_IN_SECONDS.toLong(), TimeUnit.SECONDS)) {
            fail("Read/react failed")
        }

        storage!!.removePurchaseRequestById(id)
        storage!!.removePurchaseRequestById(id2)
    }

    companion object {

        private const val PAYMENT_TOKEN = "thisIsAPaymentToken"

        private const val EPHERMERAL_MSISDN = "+4747116996"

        private const val MILLIS_TO_WAIT_WHEN_STARTING_UP = 3000

        private const val RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS = 92L

        private const val TIMEOUT_IN_SECONDS = 10
    }
}
