package org.ostelco.topup.api.auth;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@AllArgsConstructor
public class OAuthAuthenticator implements Authenticator<String, AccessTokenPrincipal> {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticator.class);

    private final ObjectMapper MAPPER = new ObjectMapper();

    @NonNull
    private String key;

    @Override
    public Optional<AccessTokenPrincipal> authenticate(String accessToken)
        throws AuthenticationException {

        JsonNode claims;
        try {
            claims = decodeClaims(getClaims(accessToken));
        } catch (IllegalArgumentException e) {
            LOG.error("Illegal or incomplete JWT token {}", accessToken);
            return Optional.empty();
        }

        String email = getEmail(claims);
        if (email == null) {
            return Optional.empty();
        }

        return Optional.of(new AccessTokenPrincipal(email));
    }

    private String getEmail(final JsonNode claims) {
        String issuer = getIssuer(claims);

        if (issuer != null && claims.has(issuer + "email")) {
            return claims.get(issuer + "email").textValue();
        } else {
            LOG.error("Missing '{}email' field in claims part of JWT token {}",
                    issuer, claims);
            return null;
        }
    }

    private String getIssuer(final JsonNode claims) {
        if (!claims.has("iss")) {
            LOG.error("Missing 'iss' field in claims part of JWT token {}",
                    claims);
            return null;
        }
        return claims.get("iss").textValue();
    }

    /* Extracts 'claims' part from JWT token.
       Throws 'illegalargumentexception' exception on error. */
    private String getClaims(final String token) {
        if (token.codePoints().filter(ch -> ch == '.').count() != 2) {
            throw new IllegalArgumentException("The provided token is an Invalid JWT token");
        }
        String parts[] = token.split("\\.");

        return new String(Base64.getDecoder().decode(parts[1]
                        .replace("-", "+")
                        .replace("_", "/")));
    }

    /* Decodes the claims part of a JWT token.
       Returns null on error. */
    private JsonNode decodeClaims(final String claims) {
        JsonNode obj = null;
        try {
            obj = MAPPER.readTree(claims);
        } catch (JsonParseException e) {
            LOG.error("Parsing of the provided json doc {} failed: {}", claims,
                    e);
        } catch (IOException e) {
            LOG.error("Unexpected error when parsing the json doc {}: {}", claims,
                    e);
        }
        return obj;
    }
}
