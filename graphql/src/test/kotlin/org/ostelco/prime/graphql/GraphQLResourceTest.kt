package org.ostelco.prime.graphql

import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.Assert
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.auth.OAuthAuthenticator
import org.ostelco.prime.graphql.util.AccessToken
import org.ostelco.prime.jsonmapper.objectMapper
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

class GraphQLResourceTest {

    private val email = "graphql@test.com"

    @Before
    fun setUp() {
        `when`(AUTHENTICATOR.authenticate(ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(AccessTokenPrincipal(email, provider = "email")))
    }

    @Test
    fun `test handlePost`() {
        val resp = RULE.target("/graphql")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .post(Entity.json(GraphQLRequest(query = """{ customer(id: "invalid@test.com") { profile { email } } }""")))
                .readEntity(GraphQlResponse::class.java)

        Assert.assertEquals(email, resp.data?.customer?.profile?.email)
    }

    @Test
    fun `test handleGet`() {
        val resp = RULE.target("/graphql")
                .queryParam("query", URLEncoder.encode("""{customer(id:"invalid@test.com"){profile{email}}}""", StandardCharsets.UTF_8.name()))
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .get(GraphQlResponse::class.java)

        Assert.assertEquals(email, resp.data?.customer?.profile?.email)
    }

    companion object {

        val AUTHENTICATOR = mock(OAuthAuthenticator::class.java)

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
                .addResource(GraphQLResource(QueryHandler(File("src/test/resources/customer.graphqls"))))
                .build()
    }
}