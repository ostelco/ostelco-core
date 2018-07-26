package org.ostelco.prime.storage.graph

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.neo4j.driver.v1.AccessMode.WRITE
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

        Neo4jClient.driver.session(WRITE).use {
            it.writeTransaction {
                it.run("MATCH (n) DETACH DELETE n")
            }
        }
    }

    @Test
    fun `test add subscriber`() {
        assertTrue(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME)))
        assertEquals(
                Subscriber(email = EMAIL, name = NAME),
                Neo4jStoreSingleton.getSubscriber(EMAIL))
    }

    @Test
    fun `test add subscription, set and get balance`() {
        assert(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME)))

        assertTrue(Neo4jStoreSingleton.addSubscription(EMAIL, MSISDN))
        assertEquals(MSISDN, Neo4jStoreSingleton.getMsisdn(EMAIL))

        Neo4jStoreSingleton.setBalance(MSISDN, BALANCE)
        assertEquals(BALANCE, Neo4jStoreSingleton.getBalance(EMAIL))
    }

    @Test
    fun `test set and get Purchase record`() {
        assert(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME)))

        val product = createProduct("1GB_249NOK", 24900)
        val now = Instant.now().toEpochMilli()

        assertTrue(Neo4jStoreSingleton.createProduct(product), "Failed to create product")

        val purchaseRecord = PurchaseRecord(MSISDN, product, now)
        assertNotNull(Neo4jStoreSingleton.addPurchaseRecord(EMAIL, purchaseRecord), "Failed to add purchase record")

        assertEquals(listOf(purchaseRecord), Neo4jStoreSingleton.getPurchaseRecords(EMAIL))
    }

    @Test
    fun `test offer, segment and get products`() {
        assert(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME)))

        Neo4jStoreSingleton.createProduct(createProduct("1GB_249NOK", 24900))
        Neo4jStoreSingleton.createProduct(createProduct("2GB_299NOK", 29900))
        Neo4jStoreSingleton.createProduct(createProduct("3GB_349NOK", 34900))
        Neo4jStoreSingleton.createProduct(createProduct("5GB_399NOK", 39900))

        val segment = Segment()
        segment.id = "NEW_SEGMENT"
        segment.subscribers = listOf(EMAIL)
        Neo4jStoreSingleton.createSegment(segment)

        val offer = Offer()
        offer.id = "NEW_OFFER"
        offer.segments = listOf("NEW_SEGMENT")
        offer.products = listOf("3GB_349NOK")
        Neo4jStoreSingleton.createOffer(offer)

        val products = Neo4jStoreSingleton.getProducts(EMAIL)
        assertEquals(1, products.size)
        assertEquals(createProduct("3GB_349NOK", 34900), products.values.first())
    }

    companion object {
        const val EMAIL = "foo@bar.com"
        const val NAME = "Test User"
        const val MSISDN = "4712345678"
        const val BALANCE = 12345L

        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose.yaml")
                .waitingForService("neo4j", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("neo4j",
                        HealthChecks.toRespond2xxOverHttp(7474) {
                            port -> port.inFormat("http://\$HOST:\$EXTERNAL_PORT/browser")
                        },
                        Duration.standardSeconds(20L))
                .build()

        @BeforeClass
        @JvmStatic
        fun start() {
            ConfigRegistry.config = Config()
            ConfigRegistry.config.host = "0.0.0.0"
            Neo4jClient.start()
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            Neo4jClient.stop()
        }
    }
}