package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.db.SubscriberDAO;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
//import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.vavr.control.Option;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests 'sign up' API.
 *
 */
public class SignUpResourceTest {
    private static final Logger LOG = LoggerFactory.getLogger(SignUpResourceTest.class);

    private Datastore datastore;
    private KeyFactory keyFactory;
    private static LocalDatastoreHelper helper = LocalDatastoreHelper.create();

    private static final SubscriberDAO DAO = mock(SubscriberDAO.class);

    private final String email = "boaty@internet.org";
    private final String name = "Boaty McBoatface";

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
        .addResource(new SignUpResource(DAO))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .build();

    @BeforeClass
    public static void setUpGSC() throws InterruptedException, IOException {
        helper.start();
        System.setProperty("DATASTORE_EMULATOR_HOST",
                "localhost:" + helper.getPort());
        LOG.info("[Datastore-Emulator] listening on port: " + helper.getPort());
    }

    @AfterClass
    public static void tearDownGCS()
        throws InterruptedException, TimeoutException, IOException {
        LOG.info("[Datastore-Emulator] shutting down...");
        helper.stop();
    }

    @Before
    public void setUp() {
        datastore = DatastoreOptions.getDefaultInstance().getService();
        keyFactory = datastore.newKeyFactory().setKind("TestEntity");
    }

    @After
    public void tearDown() throws IOException {
        helper.reset();
    }

    @Test
    public void signUp() throws Exception {
        ArgumentCaptor<Profile> arg = ArgumentCaptor.forClass(Profile.class);

        when(DAO.signUp(arg.capture())).thenReturn(Option.of(null));

        Response resp = RULE.target("/register")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("{\n" +
                              "    \"name\": \"" + name + "\",\n" +
                              "    \"email\": \"" + email + "\"\n" +
                              "}\n"));

        assertThat(resp.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(resp.getMediaType()).isNull();
        assertThat(arg.getValue().getName()).isEqualTo(name);
        assertThat(arg.getValue().getEmail()).isEqualTo(email);
    }
}
