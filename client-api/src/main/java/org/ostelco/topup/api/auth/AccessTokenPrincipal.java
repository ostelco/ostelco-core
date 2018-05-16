package org.ostelco.topup.api.auth;

import java.security.Principal;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 *
 */
@AllArgsConstructor
public class AccessTokenPrincipal implements Principal {

    @NonNull
    private final String subject;

    @Override
    public String getName() {
        return subject;
    }
}
