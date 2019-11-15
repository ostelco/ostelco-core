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
import org.ostelco.prime.dsl.DSL.job
import org.ostelco.prime.kts.engine.KtsServiceFactory
import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.model.Region
import org.ostelco.prime.storage.graph.model.Segment
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.system.measureTimeMillis
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

        job {
            create {
                Product(sku = "2GB_FREE_ON_JOINING",
                        price = Price(0, ""),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "${2.GiB()}"
                        )
                )
            }
            create {
                Product(sku = "1GB_FREE_ON_REFERRED",
                        price = Price(0, ""),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_000_000_000"
                        )
                )
            }
            create {
                Segment(id = "country-$COUNTRY_CODE}")
            }
            create {
                Region(id  = COUNTRY_CODE, name = "Norway")
            }
        }
    }

    @Ignore
    @Test
    fun `load test Neo4j`() {

        println("Provisioning test users")

        repeat(USERS) { user ->
            Neo4jStoreSingleton.addCustomer(
                    identity = Identity(id = "test-$user", type = "EMAIL", provider = "email"),
                    customer = Customer(contactEmail = "test-$user@ostelco.org", nickname = NAME))
                    .mapLeft { fail(it.message) }

            Neo4jStoreSingleton.addSubscription(
                    identity = Identity(id = "test-$user", type = "EMAIL", provider = "email"),
                    msisdn = "$user",
                    iccId = UUID.randomUUID().toString(),
                    regionCode = "no",
                    alias = "")
                    .mapLeft { fail(it.message) }
        }

        // balance = 100_000_000
        // reserved = 0

        // requested = 100
        // used = 10

        val durationInMillis = measureTimeMillis {

            val cdl = CountDownLatch(COUNT)

            runBlocking(Dispatchers.Default) {
                repeat(COUNT) { i ->
                    launch {
                        Neo4jStoreSingleton.consume(msisdn = "${i % USERS}", usedBytes = USED, requestedBytes = REQUESTED) { storeResult ->
                            storeResult.bimap(
                                    { fail(it.message) },
                                    {
                                        cdl.countDown()
                                        // println("Balance = %,d, Granted = %,d".format(it.balance, it.granted))
                                    })
                        }
                    }
                }

                // Wait for all the responses to be returned
                println("Waiting for all responses to be returned")
            }

            cdl.await()
        }

        // Print load test results
        println("Time duration: %,d milli sec".format(durationInMillis))
        val rate = COUNT * 1000.0 / durationInMillis
        println("Rate: %,.2f req/sec".format(rate))

        Neo4jStoreSingleton.getBundles(identity = Identity(id = "test-0", type = "EMAIL", provider = "email"))
                .fold(
                        { fail(it.message) },
                        {
                            assertEquals(expected = 2.GiB() - COUNT / USERS * USED - REQUESTED,
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
        const val COUNTRY_CODE = "no"

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
                    protocol = "bolt",
                    onNewCustomerAction = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.OnNewCustomerAction",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/OnNewCustomerAction.kts"
                            )
                    ),
                    allowedRegionsService = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.AllowedRegionsService",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/AllowedRegionsService.kts"
                            )
                    ),
                    onKycApprovedAction = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.OnKycApprovedAction",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/OnKycApprovedAction.kts"
                            )
                    ),
                    onRegionApprovedAction = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.OnRegionApprovedAction",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/OnRegionApprovedAction.kts"
                            )
                    ),
                    hssNameLookupService = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.HssNameLookupService",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/HssNameLookupService.kts"
                            )
                    )
            )
            Neo4jClient.start()
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            Neo4jClient.stop()
        }
    }
}

fun Int.GiB() = this.toLong() * 1024 * 1024 * 1024
