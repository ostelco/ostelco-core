package org.ostelco.topup.api.resources;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.collection.HashMap;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ostelco.prime.client.api.model.Profile;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.db.SubscriberDAOInMemoryImpl;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Profile API tests.
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProfileResourceInMemoryTest {

    private static final SubscriberDAO DAO = new SubscriberDAOInMemoryImpl();

    private final String email = "boaty@internet.org";
    private static final String key = "secret";
    private final String name = "Boaty McBoatface";
    private final String address = "Storvej 10";
    private final String postCode = "132 23";
    private final String city = "Oslo";
    private final String issuer = "http://ostelco.org/";
    private final Map<String, Object> claims = HashMap.of(issuer + "email", (Object) email)
            .toJavaMap();
    private final String accessToken = Jwts.builder()
            .setClaims(claims)
            .setIssuer(issuer)
            .signWith(SignatureAlgorithm.HS512, key)
            .compact();

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
        .addResource(new AuthDynamicFeature(
                        new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(new OAuthAuthenticator(key))
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
        .addResource(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class))
        .addResource(new ProfileResource(DAO))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .build();

    @Test
    public void T01_getProfile() throws Exception {
        Response resp = RULE.target("/profile")
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void T02_createProfile() throws Exception {
        Response resp = RULE.target("/profile")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", String.format("Bearer %s", accessToken))
            .post(Entity.json("{\n" +
                              "    \"name\": \"" + name + "\",\n" +
                              "    \"address\": \"" + address + "\",\n" +
                              "    \"postCode\": \"" + postCode + "\",\n" +
                              "    \"city\": \"" + city + "\",\n" +
                              "    \"email\": \"" + email + "\"\n" +
                              "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(resp.getMediaType()).isNull();
    }

    @Test
    public void T03_getProfile() throws Exception {
        Response resp = RULE.target("/profile")
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.readEntity(Profile.class)).isEqualTo(getCheckProfile());
    }

    @Test
    public void T04_updateProfile() throws Exception {
        String newAddress = "Solhøyden 10";
        String newPostCode = "555";

        Response resp = RULE.target("/profile")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", String.format("Bearer %s", accessToken))
            .put(Entity.json("{\n" +
                             "    \"name\": \"" + name + "\",\n" +
                             "    \"address\": \"" + newAddress + "\",\n" +
                             "    \"postCode\": \"" + newPostCode + "\",\n" +
                             "    \"city\": \"" + city + "\",\n" +
                             "    \"email\": \"" + email + "\"\n" +
                             "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType()).isNull();
    }

    @Test
    public void T05_getProfile() throws Exception {
        String newAddress = "Solhøyden 10";
        String newPostCode = "555";

        Response resp = RULE.target("/profile")
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.readEntity(Profile.class)).isEqualTo(getCheckProfile(name, newAddress,
                        newPostCode, city, email));
    }

    private Profile getCheckProfile() {
        return getCheckProfile(name, address, postCode, city, email);
    }

    private Profile getCheckProfile(final String name, final String address, final String postCode,
            final String city, final String email) {
        Profile profile = new Profile(email);
        profile.setName(name);
        profile.setAddress(address);
        profile.setPostCode(postCode);
        profile.setCity(city);
        return profile;
    }
}
