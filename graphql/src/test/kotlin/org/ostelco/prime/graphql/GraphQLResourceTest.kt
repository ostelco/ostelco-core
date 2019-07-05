package org.ostelco.prime.graphql

import graphql.introspection.IntrospectionQuery
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
import org.ostelco.prime.model.Identity
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
                .thenReturn(Optional.of(AccessTokenPrincipal(Identity(email, type = "EMAIL", provider = "email"))))
    }

    @Test
    fun `test handlePost`() {
        val resp = RULE.target("/graphql")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .post(Entity.json(GraphQLRequest(query = """{ context { customer { nickname, contactEmail } } }""")))
                .readEntity(GraphQlResponse::class.java)

        Assert.assertEquals(email, resp.data?.context?.customer?.contactEmail)
    }

    @Test
    fun `test handleGet`() {
        val resp = RULE.target("/graphql")
                .queryParam("query", URLEncoder.encode("""{context{customer{nickname,contactEmail}}}""", StandardCharsets.UTF_8))
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .get(GraphQlResponse::class.java)

        Assert.assertEquals(email, resp.data?.context?.customer?.contactEmail)
    }

    @Test
    fun `test introspection`() {
        val resp = RULE.target("/graphql")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer ${AccessToken.withEmail(email)}")
                .post(
                        Entity.json(
                                GraphQLRequest(query = IntrospectionQuery.INTROSPECTION_QUERY)
                        )
                )
                .readEntity(String::class.java)

        assert(resp.isNotBlank())
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