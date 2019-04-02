package org.ostelco.prime.storage.graph

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.neo4j.driver.v1.AccessMode.WRITE
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.Segment
import java.time.Instant
import java.util.concurrent.CountDownLatch
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class Neo4jLoadTest {

    @BeforeTest
    fun clear() {

        Neo4jClient.driver.session(WRITE).use { session ->
            session.writeTransaction {
                it.run("MATCH (n) DETACH DELETE n")
            }
        }

        Neo4jStoreSingleton.createIndex()

        Neo4jStoreSingleton.createProduct(
                Product(sku = "100MB_FREE_ON_JOINING",
                        price = Price(0, CURRENCY),
                        properties = mapOf("noOfBytes" to "100_000_000")))

        Neo4jStoreSingleton.createProduct(
                Product(sku = "1GB_FREE_ON_REFERRED",
                        price = Price(0, CURRENCY),
                        properties = mapOf("noOfBytes" to "1_000_000_000")))

        val allSegment = Segment(id = getSegmentNameFromCountryCode(COUNTRY))
        Neo4jStoreSingleton.createSegment(allSegment)
    }

    @Ignore
    @Test
    fun `load test Neo4j`() {

        println("Provisioning test users")

        repeat(USERS) { user ->
            Neo4jStoreSingleton.addCustomer(
                    identity = Identity(id = "test-$user@ostelco.org", type = "EMAIL", provider = "email"),
                    customer = Customer(contactEmail = "test-$user@ostelco.org", nickname = NAME))
                    .mapLeft { fail(it.message) }

            Neo4jStoreSingleton.addSubscription(
                    identity = Identity(id = "test-$user@ostelco.org", type = "EMAIL", provider = "email"),
                    msisdn = "$user")
                    .mapLeft { fail(it.message) }
        }

        // balance = 100_000_000
        // reserved = 0

        // requested = 100
        // used = 10

        // Start timestamp in millisecond
        val start = Instant.now()

        val cdl = CountDownLatch(COUNT)

        runBlocking(Dispatchers.Default) {
            repeat(COUNT) { i ->
                launch {
                    Neo4jStoreSingleton.consume(msisdn = "${i % USERS}", usedBytes = USED, requestedBytes = REQUESTED) { storeResult ->
                        storeResult.fold(
                                { fail(it.message) },
                                {
                                    // println("Balance = %,d, Granted = %,d".format(it.second, it.first))
                                    cdl.countDown()
                                    assert(true)
                                })
                    }
                }
            }

            // Wait for all the responses to be returned
            println("Waiting for all responses to be returned")
        }

        cdl.await()

        // Stop timestamp in millisecond
        val stop = Instant.now()

        // Print load test results
        val diff = stop.toEpochMilli() - start.toEpochMilli()
        println("Time diff: %,d milli sec".format(diff))
        val rate = COUNT * 1000.0 / diff
        println("Rate: %,.2f req/sec".format(rate))

        Neo4jStoreSingleton.getBundles(identity = Identity(id = "test-0@ostelco.org", type = "EMAIL", provider = "email"))
                .fold(
                        { fail(it.message) },
                        {
                            assertEquals(expected = 100_000_000 - COUNT / USERS * USED - REQUESTED,
                                    actual = it.single().balance,
                                    message = "Balance does not match")
                        }
                )
    }

    companion object {

        const val COUNT = 10_000
        const val USERS = 100

        const val USED = 10L
        const val REQUESTED = 100L

        const val NAME = "Test User"
        const val CURRENCY = "NOK"
        const val COUNTRY = "NO"

        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose.yaml")
                .waitingForService("neo4j", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("neo4j",
                        HealthChecks.toRespond2xxOverHttp(7474) { port ->
                            port.inFormat("http://\$HOST:\$EXTERNAL_PORT/browser")
                        },
                        Duration.standardSeconds(40L))
                .build()

        @BeforeClass
        @JvmStatic
        fun start() {
            ConfigRegistry.config = Config(
                    host = "0.0.0.0",
                    protocol = "bolt")
            Neo4jClient.start()
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            Neo4jClient.stop()
        }
    }
}