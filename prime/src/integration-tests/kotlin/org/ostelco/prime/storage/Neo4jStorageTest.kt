package org.ostelco.prime.storage

import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.storage.embeddedgraph.EmbeddedNeo4jStore
import org.ostelco.prime.storage.embeddedgraph.GraphServer
import org.ostelco.prime.storage.legacy.Products.DATA_TOPUP_3GB
import java.lang.Thread.sleep
import java.time.Instant

class Neo4jStorageTest {

    private lateinit var storage: GraphStore

    @Before
    @Throws(InterruptedException::class)
    fun setUp() {
        this.storage = EmbeddedNeo4jStore()

        sleep(MILLIS_TO_WAIT_WHEN_STARTING_UP.toLong())
        storage.removeSubscriber(EPHERMERAL_EMAIL)
        assertTrue(storage.addSubscriber(Subscriber(EPHERMERAL_EMAIL)))
        assertTrue(storage.addSubscription(EPHERMERAL_EMAIL, MSISDN))
    }

    @After
    fun cleanUp() {
        storage.removeSubscriber(EPHERMERAL_EMAIL)
    }

    @Test
    fun createReadDeleteSubscriber() {
        assertNotNull(storage.getSubscriber(EPHERMERAL_EMAIL))
    }

    @Test
    fun setBalance() {
        assertTrue(storage.setBalance(MSISDN, RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS))
        Assert.assertEquals(RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS, storage.balances[MSISDN])
        storage.setBalance(MSISDN, 0)
        Assert.assertEquals(0L, storage.balances[MSISDN])
    }

    @Test
    fun addRecordOfPurchaseTest() {

        storage.createProduct(DATA_TOPUP_3GB)

        val now = Instant.now().toEpochMilli()
        val purchase = PurchaseRecord(
                msisdn = MSISDN,
                product = DATA_TOPUP_3GB,
                timestamp = now)
        storage.addPurchaseRecord(EPHERMERAL_EMAIL, purchase)
    }

    companion object {

        private const val EPHERMERAL_EMAIL = "attherate@dotcom.com"
        private const val MSISDN = "4747116996"

        private const val MILLIS_TO_WAIT_WHEN_STARTING_UP = 3000

        private const val RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS = 92L

        @JvmStatic
        @BeforeClass
        fun setup() {
            GraphServer.start()
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            GraphServer.stop()
        }
    }
}
