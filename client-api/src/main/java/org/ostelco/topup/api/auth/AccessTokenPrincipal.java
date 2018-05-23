package org.ostelco.topup.api.auth;

import java.security.Principal;

/**
 * Holds the 'user-id' obtained by verifying and decoding an OAuth2
 * 'access-token'.
 */
public class AccessTokenPrincipal implements Principal {

    private final String subject;

    public AccessTokenPrincipal(String subject) {
        this.subject = subject;
    }

    @Override
    public String getName() {
        return subject;
    }
}
