package org.ostelco.prime.storage.graph

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import org.joda.time.Duration
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.mockito.Mockito.mock
import org.ostelco.prime.disruptor.EventProducer
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.ocs.OcsPrimeServiceSingleton
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.firebase.initFirebaseConfigRegistry
import org.ostelco.prime.storage.graph.Products.DATA_TOPUP_3GB
import java.lang.Thread.sleep
import java.time.Instant
import java.util.*

class Neo4jStorageTest {

    private lateinit var storage: GraphStore

    @Before
    @Throws(InterruptedException::class)
    fun setUp() {
        this.storage = Neo4jStore()

        sleep(MILLIS_TO_WAIT_WHEN_STARTING_UP.toLong())
        storage.removeSubscriber(EPHERMERAL_EMAIL)
        storage.addSubscriber(Subscriber(EPHERMERAL_EMAIL), referredBy = null)
                .mapLeft { fail(it.message) }
        storage.addSubscription(EPHERMERAL_EMAIL, MSISDN)
                .mapLeft { fail(it.message) }
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
        assertTrue(storage.updateBundle(Bundle(EPHERMERAL_EMAIL, RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS)).isRight())

        storage.getBundles(EPHERMERAL_EMAIL).bimap(
                { fail(it.message) },
                { bundles ->
                    assertEquals(RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS,
                            bundles?.first { it.id == EPHERMERAL_EMAIL }?.balance)
                })

        storage.updateBundle(Bundle(EPHERMERAL_EMAIL, 0))
        storage.getBundles(EPHERMERAL_EMAIL).bimap(
                { fail(it.message) },
                { bundles ->
                    assertEquals(0L,
                            bundles?.first { it.id == EPHERMERAL_EMAIL }?.balance)
                })
    }

    @Test
    fun addRecordOfPurchaseTest() {

        storage.createProduct(DATA_TOPUP_3GB)

        val now = Instant.now().toEpochMilli()
        val purchase = PurchaseRecord(
                product = DATA_TOPUP_3GB,
                timestamp = now,
                id = UUID.randomUUID().toString(),
                msisdn = "")
        storage.addPurchaseRecord(EPHERMERAL_EMAIL, purchase)
    }

    companion object {

        private const val EPHERMERAL_EMAIL = "attherate@dotcom.com"
        private const val MSISDN = "4747116996"

        private const val MILLIS_TO_WAIT_WHEN_STARTING_UP = 3000

        private const val RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS = 92L

        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/integration-tests/resources/docker-compose.yaml")
                .waitingForService("neo4j", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("neo4j",
                        HealthChecks.toRespond2xxOverHttp(7474) { port ->
                            port.inFormat("http://\$HOST:\$EXTERNAL_PORT/browser")
                        },
                        Duration.standardSeconds(30L))
                .build()

        @JvmStatic
        @BeforeClass
        fun setup() {

            initFirebaseConfigRegistry()

            val config = Config()
            config.host = "0.0.0.0"
            config.protocol = "bolt"
            ConfigRegistry.config = config

            Neo4jClient.start()

            initDatabase()

            OcsPrimeServiceSingleton.init(mock(EventProducer::class.java))
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            Neo4jClient.stop()
        }
    }
}
