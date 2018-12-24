package org.ostelco.sim.dwssl

import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.testing.DropwizardTestSupport
import org.apache.http.client.methods.HttpGet
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import java.security.GeneralSecurityException
import java.io.IOException
import javax.ws.rs.GET
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.ClientProtocolException
import org.junit.Ignore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.ws.rs.HttpMethod


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


    /**
     * We want something along these lines to work eventually, but for now it fails, the
     * certs are not in order, the cert/trust stores are perhaps not correctly configured
     * or the files they point to don't contain the correct information.
     *
     * https://www.baeldung.com/spring-boot-https-self-signed-certificate
     */
    @Ignore
    @Test
    fun handleEncryptedHttp() {


        val client = SUPPORT.getApplication<DwSslApp>().client
        /* val client = HttpClientBuilder(SUPPORT.getEnvironment())
                .build("test client/http")!! */

        // Current error: javax.net.ssl.SSLPeerUnverifiedException: Certificate for <localhost> doesn't match any of the subject alternative names: []
        //  .... perhaps adding some domains in the certificate generation will do the trick?

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


    /**
     * Ho ho ho, now we have a functioning roundtrip test for self-signed certs, implemented by
     * ignoring all test.  This is not what we want in the server implementation,
     * but sufficient to test the _server_
     */
    @Test
    @Throws(IOException::class, GeneralSecurityException::class)
    fun givenAcceptingAllCertificates_whenHttpsUrlIsConsumed_thenException() {
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