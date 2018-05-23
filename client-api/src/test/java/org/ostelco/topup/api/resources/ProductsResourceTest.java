package org.ostelco.topup.api.resources;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.collection.HashMap;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.ostelco.prime.client.api.model.Price;
import org.ostelco.prime.client.api.model.Product;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.auth.OAuthAuthenticator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Products API tests.
 */
public class ProductsResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);

    private static final String key = "secret";
    private final String issuer = "http://ostelco.org/";
    private final String email = "mw@internet.org";
    private final Map<String, Object> claims = HashMap.of(issuer + "email", (Object) email)
            .toJavaMap();
    private final String accessToken = Jwts.builder()
            .setClaims(claims)
            .setIssuer(issuer)
            .signWith(SignatureAlgorithm.HS512, key)
            .compact();
    private final List<Product> products = io.vavr.collection.List.of(
            new Product("1", new Price(10, "NOK")),
            new Product("2", new Price(5, "NOK")),
            new Product("3", new Price(20, "NOK")))
            .toJavaList();
    private final String userInfo = Base64.getEncoder()
            .encodeToString((new String("{\n" +
                    "     \"issuer\": \"someone\",\n" +
                    "     \"email\": \"mw@internet.org\"\n" +
                    "}\n"))
                    .getBytes());

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
            .addResource(new AuthDynamicFeature(
                    new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                            .setAuthenticator(new OAuthAuthenticator(key))
                            .setPrefix("Bearer")
                            .buildAuthFilter()))
            .addResource(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class))
            .addResource(new ProductsResource(DAO))
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .build();

    @Test
    public void getProducts() throws Exception {
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);

        when(DAO.getProducts(arg.capture())).thenReturn(Either.right(products));

        Response resp = RULE.target("/products")
                .request()
                .header("Authorization", String.format("Bearer %s", accessToken))
                .header("X-Endpoint-API-UserInfo", userInfo)
                .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);

        // assertThat and assertEquals is not working
        assertTrue(products.equals(resp.readEntity(new GenericType<List<Product>>() {
        })));
        assertThat(arg.getValue()).isEqualTo(email);
    }

    @Test
    public void purchaseProduct() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);

        final String sku = products.get(0).getSku();

        when(DAO.purchaseProduct(arg1.capture(), arg2.capture())).thenReturn(Option.none());

        Response resp = RULE.target(String.format("/products/%s", sku))
                .request()
                .header("Authorization", String.format("Bearer %s", accessToken))
                .header("X-Endpoint-API-UserInfo", userInfo)
                .post(Entity.text(""));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat(arg2.getValue()).isEqualTo(sku);
    }
}
