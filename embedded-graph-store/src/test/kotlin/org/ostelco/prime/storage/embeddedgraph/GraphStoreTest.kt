package org.ostelco.prime.storage.embeddedgraph

import org.junit.AfterClass
import org.junit.BeforeClass
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GraphStoreTest {

    @BeforeTest
    fun clear() {
        GraphServer.graphDb.execute("MATCH (n) DETACH DELETE n")
    }

    @Test
    fun `test add subscriber`() {
        assertTrue(GraphStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME)))
        assertEquals(
                Subscriber(email = EMAIL, name = NAME),
                GraphStoreSingleton.getSubscriber(EMAIL))
    }

    @Test
    fun `test add subscription, set and get balance`() {
        assert(GraphStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME)))

        assertTrue(GraphStoreSingleton.addSubscription(EMAIL, MSISDN))
        assertEquals(MSISDN, GraphStoreSingleton.getMsisdn(EMAIL))

        GraphStoreSingleton.setBalance(MSISDN, BALANCE)
        assertEquals(BALANCE, GraphStoreSingleton.getBalance(EMAIL))
    }

    @Test
    fun `test set and get Purchase record`() {
        assert(GraphStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME)))

        val product = createProduct("1GB_249NOK", 24900)
        val now = Instant.now().toEpochMilli()

        assertTrue(GraphStoreSingleton.createProduct(product), "Failed to create product")

        val purchaseRecord = PurchaseRecord(MSISDN, product, now)
        assertNotNull(GraphStoreSingleton.addPurchaseRecord(EMAIL, purchaseRecord), "Failed to add purchase record")

        assertEquals(listOf(purchaseRecord), GraphStoreSingleton.getPurchaseRecords(EMAIL))
    }

    @Test
    fun `test offer, segment and get products`() {
        assert(GraphStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME)))

        GraphStoreSingleton.createProduct(createProduct("1GB_249NOK", 24900))
        GraphStoreSingleton.createProduct(createProduct("2GB_299NOK", 29900))
        GraphStoreSingleton.createProduct(createProduct("3GB_349NOK", 34900))
        GraphStoreSingleton.createProduct(createProduct("5GB_399NOK", 39900))

        val segment = Segment()
        segment.id = "NEW_SEGMENT"
        segment.subscribers = listOf(EMAIL)
        GraphStoreSingleton.createSegment(segment)

        val offer = Offer()
        offer.id = "NEW_OFFER"
        offer.segments = listOf("NEW_SEGMENT")
        offer.products = listOf("3GB_349NOK")
        GraphStoreSingleton.createOffer(offer)

        val products = GraphStoreSingleton.getProducts(EMAIL)
        assertEquals(1, products.size)
        assertEquals(createProduct("3GB_349NOK", 34900), products.values.first())
    }

    companion object {
        const val EMAIL = "foo@bar.com"
        const val NAME = "Test User"
        const val MSISDN = "4712345678"
        const val BALANCE = 12345L

        @BeforeClass
        @JvmStatic
        fun start() {
            GraphServer.start()
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            GraphServer.stop()
        }
    }
}