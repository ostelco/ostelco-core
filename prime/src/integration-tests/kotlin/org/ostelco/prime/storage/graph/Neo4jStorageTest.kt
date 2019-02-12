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
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.firebase.initFirebaseConfigRegistry
import org.ostelco.prime.storage.graph.Products.DATA_TOPUP_3GB
import java.lang.Thread.sleep
import java.time.Instant
import java.util.*

class Neo4jStorageTest {

    private lateinit var storage: GraphStore

    @Before
    fun setUp() {
        this.storage = Neo4jStore()

        sleep(MILLIS_TO_WAIT_WHEN_STARTING_UP.toLong())
        storage.removeCustomer(IDENTITY)
        storage.addCustomer(IDENTITY, Customer(email = EPHERMERAL_EMAIL, country = COUNTRY), referredBy = null)
                .mapLeft { fail(it.message) }
        storage.addSubscription(IDENTITY, MSISDN)
                .mapLeft { fail(it.message) }
    }

    @After
    fun cleanUp() {
        storage.removeCustomer(IDENTITY)
    }

    @Test
    fun createReadDeleteSubscriber() {
        assertNotNull(storage.getCustomer(IDENTITY))
    }

    @Test
    fun setBalance() {
        val bundleId = storage.getBundles(IDENTITY).fold(
                {
                    fail(it.message)
                    ""
                },
                { it.first().id })

        assertTrue(storage.updateBundle(Bundle(bundleId, RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS)).isRight())

        storage.getBundles(IDENTITY).bimap(
                { fail(it.message) },
                { bundles ->
                    assertEquals(RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS,
                            bundles.firstOrNull { it.id == bundleId }?.balance)
                })

        storage.updateBundle(Bundle(bundleId, 0))
        storage.getBundles(IDENTITY).bimap(
                { fail(it.message) },
                { bundles ->
                    assertEquals(0L,
                            bundles.firstOrNull { it.id == bundleId }?.balance)
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
        private const val COUNTRY = "NO"
        private val IDENTITY = Identity(EPHERMERAL_EMAIL, "EMAIL", "email")

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
                        Duration.standardSeconds(40L))
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
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            Neo4jClient.stop()
        }
    }
}
