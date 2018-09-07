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
import java.time.Instant
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class MockOcsAdminService : OcsAdminService by Mockito.mock(OcsAdminService::class.java)

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

        val allSegment = Segment(id = "all")
        Neo4jStoreSingleton.createSegment(allSegment)
    }

    @Test
    fun `add subscriber`() {

        Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = null)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getSubscriber(EMAIL).bimap(
                { fail(it.message) },
                { assertEquals(Subscriber(email = EMAIL, name = NAME, referralId = EMAIL), it) })

        // TODO vihang: fix argument captor for neo4j-store tests
//        val bundleArgCaptor: ArgumentCaptor<Bundle> = ArgumentCaptor.forClass(Bundle::class.java)
//        verify(OCS_MOCK, times(1)).addBundle(bundleArgCaptor.capture())
//        assertEquals(Bundle(id = EMAIL, balance = 100_000_000), bundleArgCaptor.value)
    }

    @Test
    fun `fail to add subscriber with invalid referred by`() {

        Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = "blah")
                .fold({
                    assertEquals(
                            expected = "Failed to create REFERRED - blah -> foo@bar.com",
                            actual = it.message)
                },
                        { fail("Created subscriber in spite of invalid 'referred by'") })
    }

    @Test
    fun `add subscription`() {

        Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = null)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.addSubscription(EMAIL, MSISDN)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getMsisdn(EMAIL).bimap(
                { fail(it.message) },
                { assertEquals(MSISDN, it) })

        Neo4jStoreSingleton.getSubscriptions(EMAIL).bimap(
                { fail(it.message) },
                { assertEquals(listOf(Subscription(MSISDN)), it) })

        // TODO vihang: fix argument captor for neo4j-store tests
//        val msisdnArgCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
//        val bundleIdArgCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
//        verify(OCS_MOCK).addMsisdnToBundleMapping(msisdnArgCaptor.capture(), bundleIdArgCaptor.capture())
//        assertEquals(MSISDN, msisdnArgCaptor.value)
//        assertEquals(EMAIL, bundleIdArgCaptor.value)
    }

    @Test
    fun `set and get Purchase record`() {
        assert(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = null).isRight())

        val product = createProduct("1GB_249NOK", 24900)
        val now = Instant.now().toEpochMilli()

        Neo4jStoreSingleton.createProduct(product)
                .mapLeft { fail(it.message) }

        val purchaseRecord = PurchaseRecord(product = product, timestamp = now, id = UUID.randomUUID().toString(), msisdn = "")
        Neo4jStoreSingleton.addPurchaseRecord(EMAIL, purchaseRecord).bimap(
                { fail(it.message) },
                { assertNotNull(it) }
        )

        Neo4jStoreSingleton.getPurchaseRecords(EMAIL).bimap(
                { fail(it.message) },
                { assertTrue(it.contains(purchaseRecord)) }
        )
    }

    @Test
    fun `create products, offer, segment and then get products for a subscriber`() {
        assert(Neo4jStoreSingleton.addSubscriber(Subscriber(email = EMAIL, name = NAME), referredBy = null).isRight())

        Neo4jStoreSingleton.createProduct(createProduct("1GB_249NOK", 24900))
        Neo4jStoreSingleton.createProduct(createProduct("2GB_299NOK", 29900))
        Neo4jStoreSingleton.createProduct(createProduct("3GB_349NOK", 34900))
        Neo4jStoreSingleton.createProduct(createProduct("5GB_399NOK", 39900))

        val segment = Segment(
                id = "NEW_SEGMENT",
                subscribers = listOf(EMAIL))
        Neo4jStoreSingleton.createSegment(segment)

        val offer = Offer(
                id = "NEW_OFFER",
                segments = listOf("NEW_SEGMENT"),
                products = listOf("3GB_349NOK"))
        Neo4jStoreSingleton.createOffer(offer)

        Neo4jStoreSingleton.getProducts(EMAIL).bimap(
                { fail(it.message) },
                { products ->
                    assertEquals(1, products.size)
                    assertEquals(createProduct("3GB_349NOK", 34900), products.values.first())
                })

        Neo4jStoreSingleton.getProduct(EMAIL, "2GB_299NOK").bimap(
                { assertEquals("Product - 2GB_299NOK not found.", it.message) },
                { fail("Expected get product to fail since it is not linked to any subscriber --> segment --> offer") })
    }

    companion object {
        const val EMAIL = "foo@bar.com"
        const val NAME = "Test User"
        const val MSISDN = "4712345678"

        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose.yaml")
                .waitingForService("neo4j", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("neo4j",
                        HealthChecks.toRespond2xxOverHttp(7474) { port ->
                            port.inFormat("http://\$HOST:\$EXTERNAL_PORT/browser")
                        },
                        Duration.standardSeconds(20L))
                .build()

        @BeforeClass
        @JvmStatic
        fun start() {
            ConfigRegistry.config = Config()
            ConfigRegistry.config.host = "0.0.0.0"
            ConfigRegistry.config.protocol = "bolt"
            Neo4jClient.start()
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            Neo4jClient.stop()
        }
    }
}