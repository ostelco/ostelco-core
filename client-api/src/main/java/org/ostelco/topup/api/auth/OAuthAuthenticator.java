package org.ostelco.topup.api.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 *
 */
@AllArgsConstructor
public class OAuthAuthenticator implements Authenticator<String, AccessTokenPrincipal> {

    @NonNull
    private String key;

    @Override
    public Optional<AccessTokenPrincipal> authenticate(String accessToken)
        throws AuthenticationException {

        String subject = Jwts.parser()
            .setSigningKey(key)
            .parseClaimsJws(accessToken)
            .getBody()
            .getSubject();

        if (subject == null || subject.isEmpty()) {
            throw new AuthenticationException("Invalid accesss token");
        }
        return Optional.of(new AccessTokenPrincipal(subject));
    }
}
