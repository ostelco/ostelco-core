package org.ostelco.prime.client.api.resources

import arrow.core.Either
import com.nhaarman.mockito_kotlin.argumentCaptor
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit.ResourceTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.client.api.util.AccessToken
import org.ostelco.prime.model.Identity
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Consents API tests.
 *
 */
class ConsentsResourceTest {

    private val email = "mw@internet.org"

    private val consents = listOf(
            Consent("1", "blabla", false),
            Consent("2", "blabla", true))

    @Before
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email, "email")))
    }

    @Test
    fun getConsents() {
        val arg = argumentCaptor<Identity>()

        `when`(DAO.getConsents(arg.capture())).thenReturn(Either.right(consents))

        val resp = RULE.target("/consents")
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .get(Response::class.java)

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(resp.mediaType.toString()).isEqualTo(MediaType.APPLICATION_JSON)
        assertThat(resp.readEntity(object : GenericType<List<Consent>>() {

        })).isEqualTo(consents)
        assertThat(arg.firstValue).isEqualTo(Identity(email, "EMAIL", "email"))
    }

    @Test
    fun acceptConsent() {
        val arg1 = argumentCaptor<Identity>()
        val arg2 = argumentCaptor<String>()

        val consentId = consents[0].consentId

        `when`(DAO.acceptConsent(arg1.capture(), arg2.capture())).thenReturn(Either.right(consents[0]))
        `when`(DAO.rejectConsent(arg1.capture(), arg2.capture())).thenReturn(Either.left(
                NotFoundError("No consents found", ApiErrorCode.FAILED_TO_FETCH_CONSENT)))

        val resp = RULE.target("/consents/$consentId")
                .queryParam("accepted", true)
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .put(Entity.text(""))

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(arg1.firstValue).isEqualTo(Identity(email, "EMAIL", "email"))
        assertThat(arg2.firstValue).isEqualTo(consentId)
    }

    @Test
    fun rejectConsent() {
        val arg1 = argumentCaptor<Identity>()
        val arg2 = argumentCaptor<String>()

        val consentId = consents[0].consentId

        `when`(DAO.acceptConsent(arg1.capture(), arg2.capture())).thenReturn(Either.left(
                NotFoundError("No consents found", ApiErrorCode.FAILED_TO_FETCH_CONSENT)))
        `when`(DAO.rejectConsent(arg1.capture(), arg2.capture())).thenReturn(Either.right(consents[0]))

        val resp = RULE.target("/consents/$consentId")
                .queryParam("accepted", false)
                .request()
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .put(Entity.text(""))

        assertThat(resp.status).isEqualTo(Response.Status.OK.statusCode)
        assertThat(arg1.firstValue).isEqualTo(Identity(email, "EMAIL", "email"))
        assertThat(arg2.firstValue).isEqualTo(consentId)
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
                .addResource(ConsentsResource(DAO))
                .build()
    }
}
