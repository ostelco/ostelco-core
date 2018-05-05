package org.ostelco.topup.api.auth;

import java.security.Principal;

/**
 *
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
