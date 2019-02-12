package org.ostelco.prime.client.api.resources

import arrow.core.Either
import com.nhaarman.mockito_kotlin.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit.ResourceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.client.api.util.AccessToken
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.*
import java.time.Instant
import java.util.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Subscription API tests.
 *
 */
class SubscriptionResourceTest {

    private val email = "mw@internet.org"

    private val purchaseRecords = listOf(
            PurchaseRecord(
                    product = Product(sku = "1", price = Price(10, "NOK")),
                    timestamp = Instant.now().toEpochMilli(),
                    id = UUID.randomUUID().toString(),
                    msisdn = ""))

    @Before
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email, "email")))
    }

    @Ignore // XXX Fix this asap!
    @Test
    fun getActivePseudonyms() {
        val arg = argumentCaptor<Identity>()

        val msisdn = "4790300001"
        val pseudonym = PseudonymEntity(msisdn, "random", 0, 1)
        val activePseudonyms = ActivePseudonyms(pseudonym, pseudonym)

        `when`(DAO.getActivePseudonymForSubscriber(arg.capture()))
                .thenReturn(Either.right(activePseudonyms))

        val responseJsonString = objectMapper.writeValueAsString(activePseudonyms)

        val resp = RULE.target("/subscription/activePseudonyms")
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .get(Response::class.java)

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)
        assertTrue(resp.hasEntity())
        assertTrue(responseJsonString == resp.readEntity(String::class.java))
    }

    companion object {

        val DAO: SubscriberDAO = mock(SubscriberDAO::class.java)
        val AUTHENTICATOR: OAuthAuthenticator = mock(OAuthAuthenticator::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule.builder()
                .setMapper(objectMapper)
                .addResource(AuthDynamicFeature(
                        OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                                .setAuthenticator(AUTHENTICATOR)
                                .setPrefix("Bearer")
                                .buildAuthFilter()))
                .addResource(AuthValueFactoryProvider.Binder(AccessTokenPrincipal::class.java))
                .addResource(CustomerResource(DAO))
                .build()
    }
}
