package org.ostelco.topup.api.auth;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

/**
 *
 */
public class OAuthAuthenticator implements Authenticator<String, AccessTokenPrincipal> {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticator.class);

    private final ObjectMapper MAPPER = new ObjectMapper();

    private String namespace;

    private String key;

    public OAuthAuthenticator(String namespace, String key) {
        this.namespace = namespace;
        this.key = key;
    }

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

        if (claims.has(namespace + "/email")) {
            return claims.get(namespace + "/email").textValue();
        } else {
            LOG.error("Missing '{}/email' field in claims part of JWT token {}",
                    namespace, claims);
            return null;
        }
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
