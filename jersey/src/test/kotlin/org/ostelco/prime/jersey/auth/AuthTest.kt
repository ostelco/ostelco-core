package org.ostelco.prime.jersey.auth

import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.glassfish.jersey.client.ClientProperties
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.prime.jersey.auth.helpers.AccessToken
import org.ostelco.prime.jersey.auth.helpers.TestApp
import javax.ws.rs.client.Client
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Tests OAuth2 callback to '.../userinfo" endpoint.
 *
 */
class AuthTest {

    private val email = "boaty@internet.org"

    private val audience = listOf("http://kmmtest",
            "http://localhost:${RULE.localPort}/userinfo")

    @Test
    fun getNotFound() {


        // XXX Race condition makes test fail sometimes.
        // This is an abominiation :-) But it's whats necessary to consistently pass this
        // test on my workstation.

        waitForServer()


        val response = client.target(
                "http://localhost:${RULE.localPort}/foo")
                .request()
                .property(ClientProperties.CONNECT_TIMEOUT, 30000)
                .property(ClientProperties.READ_TIMEOUT, 30000)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email, audience)}")
                .get(Response::class.java)

        assertThat(response.status).isEqualTo(Response.Status.NOT_FOUND.statusCode)
        assertThat(response.mediaType.toString()).startsWith(MediaType.APPLICATION_JSON)
    }

    private fun waitForServer() {
        var counter = 40  // Max wait time, ten seconds.
        while (counter > 0) {
            try {
                val r = client.target(
                        "http://localhost:${RULE.adminPort}/healthcheck")
                        .request()
                        .get(Response::class.java)
                if (r.status == 200) {
                    println("Connected")
                    break
                }
            } catch (t: Throwable) {
                println("Caught throwable  $t")
            }
            counter -= 1
            Thread.sleep(250)
        }

        if (counter == 0) {
            fail("Couldn't connect to RULE server")
        }
    }

    companion object {
        private val key = "secret"

        private lateinit var client: Client

        @JvmField
        @ClassRule
        val RULE = DropwizardAppRule(TestApp::class.java, ResourceHelpers.resourceFilePath("test.yaml"),
                ConfigOverride.config("secret", key))

        @BeforeClass
        @JvmStatic
        fun setUpClient() {
            client = JerseyClientBuilder(RULE.environment).build("test client")
        }
    }
}
