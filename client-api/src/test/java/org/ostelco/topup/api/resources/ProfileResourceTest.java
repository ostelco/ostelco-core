package org.ostelco.topup.api.resources;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.ostelco.prime.client.api.model.Profile;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.util.AccessToken;
import org.ostelco.topup.api.util.AuthDynamicFeatureFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    private final String address = "Storvej 10";
    private final String postCode = "132 23";
    private final String city = "Oslo";

    private final Profile profile = new Profile(email);

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
        .addResource(AuthDynamicFeatureFactory.createInstance(key))
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
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
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
    public void createProfile() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Profile> arg2 = ArgumentCaptor.forClass(Profile.class);

        when(DAO.createProfile(arg1.capture(), arg2.capture()))
            .thenReturn(Option.none());

        Response resp = RULE.target("/profile")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
            .post(Entity.json("{\n" +
                              "    \"name\": \"" + name + "\",\n" +
                              "    \"address\": \"" + address + "\",\n" +
                              "    \"postCode\": \"" + postCode + "\",\n" +
                              "    \"city\": \"" + city + "\",\n" +
                              "    \"email\": \"" + email + "\"\n" +
                              "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(resp.getMediaType()).isNull();
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat((arg2.getValue()).getEmail()).isEqualTo(email);
        assertThat((arg2.getValue()).getName()).isEqualTo(name);
        assertThat((arg2.getValue()).getAddress()).isEqualTo(address);
        assertThat((arg2.getValue()).getPostCode()).isEqualTo(postCode);
        assertThat((arg2.getValue()).getCity()).isEqualTo(city);
    }

    @Test
    public void updateProfile() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Profile> arg2 = ArgumentCaptor.forClass(Profile.class);

        String newAddress = "Storvej 10";
        String newPostCode = "132 23";

        when(DAO.updateProfile(arg1.capture(), arg2.capture()))
            .thenReturn(Option.none());

        Response resp = RULE.target("/profile")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
            .put(Entity.json("{\n" +
                             "    \"name\": \"" + name + "\",\n" +
                             "    \"address\": \"" + newAddress + "\",\n" +
                             "    \"postCode\": \"" + newPostCode + "\",\n" +
                             "    \"city\": \"" + city + "\",\n" +
                             "    \"email\": \"" + email + "\"\n" +
                             "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resp.getMediaType()).isNull();
        assertThat(arg1.getValue()).isEqualTo(email);
        assertThat((arg2.getValue()).getEmail()).isEqualTo(email);
        assertThat((arg2.getValue()).getName()).isEqualTo(name);
        assertThat((arg2.getValue()).getAddress()).isEqualTo(newAddress);
        assertThat((arg2.getValue()).getPostCode()).isEqualTo(newPostCode);
        assertThat((arg2.getValue()).getCity()).isEqualTo(city);
    }

    @Test
    public void updateWithIncompleteProfile() throws Exception {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Profile> arg2 = ArgumentCaptor.forClass(Profile.class);

        when(DAO.updateProfile(arg1.capture(), arg2.capture()))
            .thenReturn(Option.of(new Error("No profile found")));

        Response resp = RULE.target("/profile")
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", String.format("Bearer %s", AccessToken.withEmail(email)))
            .put(Entity.json("{\n" +
                             "    \"name\": \"" + name + "\"\n" +
                             "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(arg1.getValue()).isEqualTo(email);
    }
}
