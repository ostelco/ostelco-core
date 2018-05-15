package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.collection.HashMap;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Profile API tests.
 *
 */
public class ProfileResourceTest {

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);

    private final String email = "boaty@internet.org";
    private static final String key = "secret";
    private final String name = "Boaty McBoatface";
    private final String subscriptionId = "007";
    private final String issuer = "http://ostelco.org/";
    private final Map<String, Object> claims = HashMap.of(issuer + "email", (Object) email)
            .toJavaMap();
    private final String accessToken = Jwts.builder()
            .setClaims(claims)
            .setIssuer(issuer)
            .setSubject(subscriptionId)
            .signWith(SignatureAlgorithm.HS512, key)
            .compact();
    private final Profile profile = new Profile(name, email);

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
    public void getProfile() throws Exception {
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);

        when(DAO.getProfile(arg.capture())).thenReturn(Either.right(profile));

        Response resp = RULE.target("/profile")
            .request()
            .header("Authorization", String.format("Bearer %s", accessToken))
            .get(Response.class);

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);

        /* Requires that:
               lombok.anyConstructor.addConstructorProperties=true
           is added to the lombok config file ('lombok.config').
           Ref.: lombok changelog for ver. 1.16.20. */
        assertThat(resp.readEntity(Profile.class)).isEqualTo(profile);
        assertThat(arg.getValue()).isEqualTo(email);
    }

    @Test
    public void updateProfile() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Profile> arg2 = ArgumentCaptor.forClass(Profile.class);

        when(DAO.updateProfile(arg1.capture(), arg2.capture()))
            .thenReturn(Option.none());

        Response resp = RULE.target("/profile")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", String.format("Bearer %s", accessToken))
            .put(Entity.json("{\n" +
                             "    \"name\": \"" + name + "\",\n" +
                             "    \"email\": \"" + email + "\"\n" +
                             "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType()).isNull();
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat((arg2.getValue()).getEmail()).isEqualTo(email);
        assertThat((arg2.getValue()).getName()).isEqualTo(name);
    }

    @Test
    public void updateWithIncompleteProfile() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Profile> arg2 = ArgumentCaptor.forClass(Profile.class);

        when(DAO.updateProfile(arg1.capture(), arg2.capture()))
            .thenReturn(Option.of(new Error("No profile found")));

        Response resp = RULE.target("/profile")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", String.format("Bearer %s", accessToken))
            .put(Entity.json("{\n" +
                             "    \"name\": \"" + name + "\"\n" +
                             "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(arg1.getValue()).isEqualTo(email);
    }
}
