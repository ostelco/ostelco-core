package org.ostelco.sim.dwssl

import javax.ws.rs.client.Entity.json
import org.glassfish.jersey.client.JerseyClientBuilder
import com.fasterxml.jackson.databind.cfg.ConfigOverride
import io.dropwizard.Configuration
import io.dropwizard.testing.DropwizardTestSupport
import io.dropwizard.testing.ResourceHelpers
import org.assertj.core.api.Assertions.assertThat
import org.junit.*
import javax.ws.rs.core.Response


class AcceptanceTestSslRoundtrip {

    @Before
    fun setUp() {
        SUPPORT.before()
    }

    @After
    fun tearDown() {
        SUPPORT.after()
    }

    @Test
    fun loginHandlerRedirectsAfterPost() {
        val client = JerseyClientBuilder().build()

        val response = client.target(
                String.format("http://localhost:%d/ping", 8080))
                .request()
                .get(Response::class.java)

        assertThat(response.status).isEqualTo(200)
    }

    companion object {

        val SUPPORT = DropwizardTestSupport<Configuration>(
                DwSslApp::class.java,
                "config/config.yaml"
                // ResourceHelpers.resourceFilePath("config.yaml")// ,
                // ConfigOverride.config("server.applicationConnectors[0].port", "0") // Optional, if not using a separate testing-specific configuration file, use a randomly selected port
        )
    }
}