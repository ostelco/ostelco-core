package org.ostelco.prime

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import io.dropwizard.testing.DropwizardTestSupport
import io.dropwizard.testing.ResourceHelpers
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

class TestPrimeConfig {

    /**
     * Do nothing.
     * This test will just start and stop the server.
     * It will validate config file in 'src/integration-tests/resources/config.yaml'
     */
    @Test
    fun test() {
        assertNotNull(SUPPORT)
    }

    companion object {

        private val SUPPORT:DropwizardTestSupport<PrimeConfiguration> =
                DropwizardTestSupport(
                        PrimeApplication::class.java,
                        ResourceHelpers.resourceFilePath("config.yaml"))

        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/integration-tests/resources/docker-compose.yaml")
                .waitingForService("neo4j", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("neo4j",
                        HealthChecks.toRespond2xxOverHttp(7474) {
                            port -> port.inFormat("http://\$HOST:\$EXTERNAL_PORT/browser")
                        },
                        Duration.standardSeconds(20L))
                .build()

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            SUPPORT.before()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            SUPPORT.after()
        }
    }
}
