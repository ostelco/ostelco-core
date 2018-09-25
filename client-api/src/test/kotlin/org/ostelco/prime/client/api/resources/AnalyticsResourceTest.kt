package org.ostelco.prime.client.api.resources

import arrow.core.Either
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit.ResourceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.auth.OAuthAuthenticator
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.client.api.util.AccessToken
import java.io.IOException
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Analytics API tests.
 *
 */
class AnalyticsResourceTest {

    private val MAPPER = ObjectMapper()

    private val email = "mw@internet.org"

    @Before
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email)))
    }

    @Test
    fun reportAnalytics() {
        val arg1 = argumentCaptor<String>()
        val arg2 = argumentCaptor<String>()

        `when`(DAO.reportAnalytics(arg1.capture(), arg2.capture())).thenReturn(Either.right(Unit))

        val events = """
            [
              {
                "eventType": "PURCHASES_A_PRODUCT",
                "sku": "1",
                "time": "1524734549"
              },
              {
                "eventType": "EXITS_APPLICATION",
                "time": "1524742549"
              }
            ]""".trimIndent()

        assertThat(isValidJson(events)).isTrue()

        val resp = RULE.target("/analytics")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .post(Entity.json(events))

        assertThat(resp.status).isEqualTo(Response.Status.CREATED.statusCode)
        assertThat(resp.mediaType).isNull()
        assertThat(arg1.firstValue).isEqualTo(email)
        assertThat(isValidJson(events)).isTrue()
        assertThat(isValidJson(arg2.firstValue)).isTrue()
    }

    /* https://stackoverflow.com/questions/10226897/how-to-validate-json-with-jackson-json */
    private fun isValidJson(json: String): Boolean {
        try {
            val parser = MAPPER.factory
                    .createParser(json)
            while (parser.nextToken() != null) {
            }
            return true
        } catch (e: JsonParseException) {
            /* Ignored. */
        } catch (e: IOException) {
            /* Ignored. */
        }

        return false
    }

    companion object {

        val DAO: SubscriberDAO = mock(SubscriberDAO::class.java)
        val AUTHENTICATOR: OAuthAuthenticator = mock(OAuthAuthenticator::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule.builder()
                .addResource(AuthDynamicFeature(
                        OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(AnalyticsResource(DAO))
                .setTestContainerFactory(GrizzlyWebTestContainerFactory())
                .build()
    }
}
