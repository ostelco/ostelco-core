package org.ostelco.prime.dsl

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.neo4j.driver.v1.AccessMode.WRITE
import org.ostelco.prime.dsl.DSL.job
import org.ostelco.prime.model.Region
import org.ostelco.prime.storage.graph.Config
import org.ostelco.prime.storage.graph.ConfigRegistry
import org.ostelco.prime.storage.graph.EntityRegistry.getEntityStore
import org.ostelco.prime.storage.graph.Neo4jClient
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.fail

class DSLTest {

    @BeforeTest
    fun clear() {

        Neo4jClient.driver.session(WRITE).use { session ->
            session.writeTransaction {
                it.run("MATCH (n) DETACH DELETE n")
            }
        }
    }

    @Test
    fun `test - create Regions`() {

        job {
            create {
                Region(id = "no", name = "Norway")
            }
            create {
                Region(id = "sg", name = "Singapore")
            }
        }

        Neo4jClient.driver.session(WRITE).use { session ->
            session.writeTransaction {
                getEntityStore(Region::class.java)
                        .get(id = "no", transaction = it)
                        .bimap(
                                { fail("Unable to read region") },
                                { region: Region ->
                                    assertEquals(
                                            Region(id = "no", name = "Norway"),
                                            region,
                                            "Region does not match")
                                })
            }
        }

    }

    companion object {

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