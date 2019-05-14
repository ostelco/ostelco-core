package org.ostelco.simcards.smdpplus

import io.dropwizard.testing.DropwizardTestSupport
import org.apache.http.client.methods.HttpGet
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class PingPongSslRoundtripTest {

    companion object {
        val SUPPORT = DropwizardTestSupport<PingPongAppConfiguration>(
                PingPongApp::class.java,
                "src/test/resources/config.yml"
        )
        /* From config file. */
        const val HTTP_PORT = 8080
        const val TLS_PORT  = 8443
    }

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
        val client = SUPPORT.getApplication<PingPongApp>().client
        val httpGet = HttpGet(String.format("http://localhost:%d/ping", HTTP_PORT))
        val response = client.execute(httpGet)
        assertThat(response.statusLine.statusCode).isEqualTo(200)
    }

    /**
     * This now works, since we disabled hostname  checking and enabled self-signed
     * certificates in the config file.  It would be nice if we could enable the
     * hostname checks in the test, but I don't know exactly how to make that
     * happen.
     *
     * https://www.baeldung.com/spring-boot-https-self-signed-certificate
     */
    @Test
    fun handleEncryptedHttp() {
        val client = SUPPORT.getApplication<PingPongApp>().client
        val httpGet = HttpGet(String.format("https://localhost:%d/ping", TLS_PORT))
        val response = client.execute(httpGet)
        assertThat(response.statusLine.statusCode).isEqualTo(200)
    }
}
