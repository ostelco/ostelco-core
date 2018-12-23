package org.ostelco.sim.dwssl

import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.testing.DropwizardTestSupport
import org.apache.http.client.methods.HttpGet
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test


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


        val client = HttpClientBuilder(SUPPORT.getEnvironment())
                .build("test client/http")!!

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