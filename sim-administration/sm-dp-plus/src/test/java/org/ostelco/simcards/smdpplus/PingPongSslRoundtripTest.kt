package org.ostelco.simcards.smdpplus

import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.testing.DropwizardTestSupport
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate


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
        val client = SUPPORT.getApplication<SmDpPlusApplication>().client
        val httpGet = HttpGet(String.format("https://localhost:%d/ping", 8443))
        val response = client.execute(httpGet)
        assertThat(response.statusLine.statusCode).isEqualTo(200)
    }

    companion object {

        val SUPPORT = DropwizardTestSupport<SmDpPlusAppConfiguration>(
                SmDpPlusApplication::class.java,
                "config.yml"
        )
    }


    /**
     * Ho ho ho, now we have a functioning roundtrip test for self-signed certs, implemented by
     * ignoring all test.  This is not what we want in the server implementation,
     * but sufficient to test the _server_
     */
    @Test
    @Throws(IOException::class, GeneralSecurityException::class)
    fun testUsingExtremelyPermissiveHandConiguredClient() {
        val acceptingTrustStrategy = object: TrustStrategy {
            @Throws(CertificateException::class)
            override fun isTrusted(chain: Array<X509Certificate>, authType: String): Boolean = true
        }
        val sf = SSLSocketFactory(
                acceptingTrustStrategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
        val registry = SchemeRegistry()
        registry.register(Scheme("https", 8443, sf))
        val ccm = PoolingClientConnectionManager(registry)

        val httpClient = DefaultHttpClient(ccm)


        val urlOverHttps = "https://localhost:8443/ping"
        val getMethod = HttpGet(urlOverHttps)


        val response = httpClient.execute(getMethod)
        assertThat(response.statusLine.statusCode).isEqualTo(200)
    }
}