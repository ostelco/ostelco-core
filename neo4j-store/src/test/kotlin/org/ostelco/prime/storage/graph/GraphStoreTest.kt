package org.ostelco.prime.storage.graph

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.mockito.Mockito
import org.neo4j.driver.v1.AccessMode.WRITE
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.ocs.OcsAdminService
import org.ostelco.prime.storage.graph.GraphStoreTest.Companion.OCS_MOCK
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockOcsAdminService : OcsAdminService by OCS_MOCK

class GraphStoreTest {

    @BeforeTest
    fun clear() {

        Neo4jClient.driver.session(WRITE).use { session ->
            session.writeTransaction {
                it.run("MATCH (n) DETACH DELETE n")
            }
        }

        Neo4jStoreSingleton.createProduct(
                Product(sku = "100MB_FREE_ON_JOINING",
                        price = Price(0, "NOK"),
                        properties = mapOf("noOfBytes" to "100_000_000")))

        Neo4jStoreSingleton.createProduct(
                Product(sku = "1GB_FREE_ON_REFERRED",
                        price = Price(0, "NOK"),
                        properties = mapOf("noOfBytes" to "1_000_000_000")))

        val allSegment = Segment()
        allSegment.id = "all"
        Neo4jStoreSingleton.createSegment(allSegment)
    }

    @Test
    fun `test add subscriber`() {

        assertTrue(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = null).isEmpty())
        assertEquals(
                Subscriber(email = EMAIL, name = NAME, referralId = EMAIL),
                Neo4jStoreSingleton.getSubscriber(EMAIL).toOption().orNull())

        // TODO vihang: fix argument captor for neo4j-store tests
//        val bundleArgCaptor: ArgumentCaptor<Bundle> = ArgumentCaptor.forClass(Bundle::class.java)
//        verify(OCS_MOCK, times(1)).addBundle(bundleArgCaptor.capture())
//        assertEquals(Bundle(id = EMAIL, balance = 100_000_000), bundleArgCaptor.value)
    }

    @Test
    fun `test add subscription`() {

        assert(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = null).isEmpty())

        assertTrue(Neo4jStoreSingleton.addSubscription(EMAIL, MSISDN).isEmpty())
        assertEquals(MSISDN, Neo4jStoreSingleton.getMsisdn(EMAIL))
        assertEquals(listOf(Subscription(MSISDN)), Neo4jStoreSingleton.getSubscriptions(EMAIL))

        // TODO vihang: fix argument captor for neo4j-store tests
//        val msisdnArgCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
//        val bundleIdArgCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
//        verify(OCS_MOCK).addMsisdnToBundleMapping(msisdnArgCaptor.capture(), bundleIdArgCaptor.capture())
//        assertEquals(MSISDN, msisdnArgCaptor.value)
//        assertEquals(EMAIL, bundleIdArgCaptor.value)
    }

    @Test
    fun `test set and get Purchase record`() {
        assert(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = null).isEmpty())

        val product = createProduct("1GB_249NOK", 24900)
        val now = Instant.now().toEpochMilli()

        assertTrue(Neo4jStoreSingleton.createProduct(product).isEmpty(), "Failed to create product")

        val purchaseRecord = PurchaseRecord(product = product, timestamp = now)
        assertNotNull(Neo4jStoreSingleton.addPurchaseRecord(EMAIL, purchaseRecord), "Failed to add purchase record")

        assertTrue(Neo4jStoreSingleton.getPurchaseRecords(EMAIL).contains(purchaseRecord))
    }

    @Test
    fun `test offer, segment and get products`() {
        assert(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = null).isEmpty())

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
        val OCS_MOCK = Mockito.mock(OcsAdminService::class.java)

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