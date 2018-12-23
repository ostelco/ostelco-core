package org.ostelco.sim.dwssl

import javax.ws.rs.client.Entity.json
import com.fasterxml.jackson.databind.cfg.ConfigOverride
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.testing.DropwizardTestSupport
import io.dropwizard.testing.ResourceHelpers
import org.assertj.core.api.Assertions.assertThat
import org.junit.*
import java.net.http.HttpRequest
import javax.ws.rs.core.Response
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet




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
    fun handleNonEncryptedHttp() {
        val client = HttpClientBuilder(SUPPORT.getEnvironment()).build("test client/http")

        val httpGet = HttpGet(String.format("http://localhost:%d/ping", 8080))
        val response = client.execute(httpGet)
        assertThat(response.statusLine.statusCode).isEqualTo(200)
    }


    @Test
    fun handleEncryptedHttp() {
        val client = HttpClientBuilder(SUPPORT.getEnvironment()).build("test client/http")

        val httpGet = HttpGet(String.format("https://localhost:%d/ping", 8443))
        val response = client.execute(httpGet)
        assertThat(response.statusLine.statusCode).isEqualTo(200)
    }

    companion object {

        val SUPPORT = DropwizardTestSupport<DweSslAppConfig>(
                DwSslApp::class.java,
                "config/config.yaml"
                // ResourceHelpers.resourceFilePath("config.yaml")// ,
                // ConfigOverride.config("server.applicationConnectors[0].port", "0") // Optional, if not using a separate testing-specific configuration file, use a randomly selected port
        )
    }
}