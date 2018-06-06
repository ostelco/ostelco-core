package org.ostelco.prime.client.api.auth

import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import io.vavr.collection.Array
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.prime.client.api.auth.helpers.TestApp
import org.ostelco.prime.client.api.util.AccessToken
import javax.ws.rs.client.Client
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Tests OAuth2 callback to '.../userinfo" endpoint.
 *
 */
class GetUserInfoTest {

    private val email = "boaty@internet.org"

    private val audience = Array.of("http://kmmtest",
            "http://localhost:${RULE.localPort}/userinfo")
            .toJavaList()

    @Test
    @Throws(Exception::class)
    fun getProfileNotFound() {

        val response = client!!.target(
                "http://localhost:${RULE.localPort}/profile")
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email, audience)}")
                .get(Response::class.java)

        assertThat(response.status).isEqualTo(Response.Status.NOT_FOUND.statusCode)
        assertThat(response.mediaType.toString()).startsWith(MediaType.APPLICATION_JSON)
    }

    companion object {
        private val key = "secret"

        private var client: Client? = null

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
