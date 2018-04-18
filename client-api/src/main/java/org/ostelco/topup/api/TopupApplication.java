package org.ostelco.topup.api;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.db.SubscriberDAOImpl;
import org.ostelco.topup.api.resources.AnalyticsResource;
import org.ostelco.topup.api.resources.AuthResource;
import org.ostelco.topup.api.resources.ConsentsResource;
import org.ostelco.topup.api.resources.OffersResource;
import org.ostelco.topup.api.resources.ProfileResource;
import org.ostelco.topup.api.resources.SignUpResource;
import org.ostelco.topup.api.resources.SubscriptionResource;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.IOException;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides API for "top-up" client.
 *
 */
public class TopupApplication extends Application<TopupConfiguration> {

    /**
     * Application entry point.
     * @param args - command line args
     */
    public static void main(String[] args) throws Exception {
        new TopupApplication().run(args);
    }

    @Override
    public String getName() {
        return "top-up";
    }

    @Override
    public void initialize(Bootstrap<TopupConfiguration> bootstrap) {
        checkNotNull(bootstrap);
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor()));
    }

    @Override
    public void run(final TopupConfiguration config, final Environment env) throws IOException {
        checkNotNull(config);
        checkNotNull(env);
        final SubscriberDAO dao = new SubscriberDAOImpl(DatastoreOptions.getDefaultInstance()
                .getService());

        /* APIs. */
        env.jersey().register(new AnalyticsResource(dao));
        env.jersey().register(new AuthResource(dao));
        env.jersey().register(new ConsentsResource(dao));
        env.jersey().register(new OffersResource(dao));
        env.jersey().register(new ProfileResource(dao));
        env.jersey().register(new SignUpResource(dao));
        env.jersey().register(new SubscriptionResource(dao));

        /* OAuth2. */
        env.jersey().register(new AuthDynamicFeature(
                        new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(new OAuthAuthenticator("jwtsecret"))
                        .setPrefix("Bearer")
                        .buildAuthFilter()));
        env.jersey().register(new AuthValueFactoryProvider.Binder<>(AccessTokenPrincipal.class));
    }
}
