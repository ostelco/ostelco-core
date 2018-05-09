package org.ostelco.topup.api.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.Claims;
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

        Claims claims = Jwts.parser()
            .setSigningKey(key)
            .parseClaimsJws(accessToken)
            .getBody();
        String email = claims.get(claims.getIssuer() + "email",
                String.class);

        if (email == null || email.isEmpty()) {
            throw new AuthenticationException("Invalid accesss token");
        }
        return Optional.of(new AccessTokenPrincipal(email));
    }
}
