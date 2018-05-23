package org.ostelco.topup.api.util;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.auth.OAuthAuthenticator;

public class AuthDynamicFeatureFactory {

    private static final String namespace = "https://ostelco.org";

    public static AuthDynamicFeature createInstance(final String key) {
        return new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<AccessTokenPrincipal>()
                        .setAuthenticator(new OAuthAuthenticator(namespace, key))
                        .setPrefix("Bearer")
                        .buildAuthFilter());
    }
}
