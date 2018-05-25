package org.ostelco.topup.api.auth;

import org.ostelco.topup.api.core.UserInfo;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.jsonwebtoken.Jwts;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies an OAuth2 'access-token' and fetches additional user information
 * using the '.../userinfo' endpoint.
 *
 * Assumes that the OAuth2 vendor is compliant with OpenID.
 *
 * Ref.: http://openid.net/specs/openid-connect-core-1_0.html#UserInfo
 *       https://www.dropwizard.io/1.3.2/docs/manual/auth.html#oauth2
 */
public class OAuthAuthenticator implements Authenticator<String, AccessTokenPrincipal> {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticator.class);

    @NonNull
    private final Client client;

    @NonNull
    private final String key;

    public OAuthAuthenticator(String namespace, String key) {
        this.namespace = namespace;
        this.key = key;
    }

    @Override
    public Optional<AccessTokenPrincipal> authenticate(String accessToken)
        throws AuthenticationException {

        String audience = Jwts.parser()
            .setSigningKey(key)
            .parseClaimsJws(accessToken)
            .getBody()
            .getAudience();

        if (audience == null || audience.isEmpty()) {
            LOG.error("No audience field in the 'access-token' claims part");
            throw new AuthenticationException("No audience field in the 'access-token' claims part");
        }

        Optional<String> ep = findFirstMatch("/userinfo$", decodeAudience(audience));

        if (!ep.isPresent()) {
            LOG.error("No 'userinfo' endpoint found in audience claim");
            throw new AuthenticationException("No 'userinfo' endpoint found in audience claim");
        }

        UserInfo userInfo = getUserInfo(ep.get(), accessToken);
        String email = userInfo.getEmail();

        return email != null && !email.isEmpty()
            ? Optional.of(new AccessTokenPrincipal(email))
            : Optional.empty();
    }

    private UserInfo getUserInfo(final String ep, final String accessToken)
        throws AuthenticationException {

        Response response = client.target(ep)
            .request()
            .accept("application/json")
            .header("Authorization", String.format("Bearer %s", accessToken))
            .get(Response.class);

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            LOG.error("Unexpected HTTP status {} code when fething 'user-info' from {}",
                    response.getStatus(), ep);
            throw new AuthenticationException(String.format(
                            "Unexpected HTTP status %s code when fething 'user-info' from %s",
                            response.getStatus(), ep));
        }

        UserInfo userInfo = response.readEntity(UserInfo.class);

        if (userInfo == null) {
            LOG.error("No 'user-info' body part in respose from {}",
                    ep);
            throw new AuthenticationException("No 'user-info' body part in respose from " +
                    ep);
        }

        return userInfo;
    }

    /* As an 'audience' claim can either be a string or an array of strings,
       convert the claim(s) to a list even if it should be a string. */
    private List<String> decodeAudience(final String audience) {
        return Arrays.asList(audience.replaceAll("[\\[\\]]", "").split("\\s*,\\s*"));
    }

    private Optional<String> findFirstMatch(final String pattern, final List<String> lst) {
        return findMatches(Pattern.compile(pattern), lst)
            .stream()
            .findFirst();
    }

    private List<String> findMatches(final Pattern match, final List<String> lst) {
        return lst.stream()
            .filter(match.asPredicate())
            .collect(Collectors.toList());
    }
}
