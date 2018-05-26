package org.ostelco.prime.storage.firebase

import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.storage.legacy.Products.DATA_TOPUP_3GB
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.prime.storage.legacy.StorageException
import java.lang.Thread.sleep
import java.time.Instant
import java.util.*

class FbStorageTest {

    private lateinit var storage: Storage

    private lateinit var prids: MutableCollection<String>

    @Before
    @Throws(StorageException::class, InterruptedException::class)
    fun setUp() {
        initFirebaseConfigRegistry()
        this.storage = FirebaseStorage()

        sleep(MILLIS_TO_WAIT_WHEN_STARTING_UP.toLong())
        storage.removeSubscriber(EPHERMERAL_EMAIL)
        storage.addSubscriber(EPHERMERAL_EMAIL, Subscriber(EPHERMERAL_EMAIL))
        storage.addSubscription(EPHERMERAL_EMAIL, MSISDN)
        this.prids = ArrayList()
    }

    @After
    @Throws(StorageException::class)
    fun cleanUp() {
        storage.removeSubscriber(EPHERMERAL_EMAIL)
    }

    @Test
    @Throws(StorageException::class)
    fun createReadDeleteSubscriber() {
        assertNotNull(storage.getSubscriber(EPHERMERAL_EMAIL))
    }

    @Test
    @Throws(StorageException::class)
    fun setBalance() {
        storage.setBalance(MSISDN, RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS)
        Assert.assertEquals(RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS, storage.balances[MSISDN])
        storage.setBalance(MSISDN, 0)
        Assert.assertEquals(0L, storage.balances[MSISDN])
    }

    @Test
    @Throws(StorageException::class)
    fun addRecordOfPurchaseTest() {
        val now = Instant.now().toEpochMilli()
        val purchase = PurchaseRecord(
                msisdn = MSISDN,
                sku = DATA_TOPUP_3GB.sku,
                millisSinceEpoch = now)
        storage.addPurchaseRecord(EPHERMERAL_EMAIL, purchase)
    }

    companion object {

        private const val EPHERMERAL_EMAIL = "attherate@dotcom.com"
        private const val MSISDN = "4747116996"

        private const val MILLIS_TO_WAIT_WHEN_STARTING_UP = 3000

        private const val RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS = 92L
    }
}
