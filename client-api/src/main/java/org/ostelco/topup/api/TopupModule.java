package org.ostelco.topup.api;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import org.ostelco.prime.module.PrimeModule;
import org.ostelco.topup.api.db.SubscriberDAOInMemoryImpl;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;
import org.ostelco.topup.api.db.SubscriberDAO;
import org.ostelco.topup.api.resources.AnalyticsResource;
import org.ostelco.topup.api.resources.ConsentsResource;
import org.ostelco.topup.api.resources.ProductsResource;
import org.ostelco.topup.api.resources.ProfileResource;
import org.ostelco.topup.api.resources.SubscriptionResource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides API for "top-up" client.
 *
 */
@JsonTypeName("api")
public class TopupModule implements PrimeModule {

    @Override
    public void init(final Environment env) {

        checkNotNull(env);
        // final SubscriberDAO dao = new SubscriberDAOImpl(DatastoreOptions.getDefaultInstance().getService());
        final SubscriberDAO dao = new SubscriberDAOInMemoryImpl();

        /* APIs. */
        env.jersey().register(new AnalyticsResource(dao));
        env.jersey().register(new ConsentsResource(dao));
        env.jersey().register(new ProductsResource(dao));
        env.jersey().register(new ProfileResource(dao));
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
