import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.testing.DropwizardTestSupport
import org.apache.http.client.methods.HttpGet
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test


class PingPongSslRoundtripTest {

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
        assertThat(response.statusLine.statusCode).isEqualTo(403)
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
        val client = SUPPORT.getApplication<SmDpPlusApplication>().client
        val httpGet = HttpGet(String.format("https://localhost:%d/ping", 8443))
        val response = client.execute(httpGet)
        assertThat(response.statusLine.statusCode).isEqualTo(200)
    }

    companion object {

        val SUPPORT = DropwizardTestSupport<PingPongAppConfiguration>(
                SmDpPlusApplication::class.java,
                "config.yml"
        )
    }
}