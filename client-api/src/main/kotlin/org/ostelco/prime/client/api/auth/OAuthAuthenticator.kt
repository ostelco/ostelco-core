package org.ostelco.prime.client.api.auth

import io.dropwizard.auth.AuthenticationException
import io.dropwizard.auth.Authenticator
import io.jsonwebtoken.Jwts
import org.ostelco.prime.client.api.core.UserInfo
import org.ostelco.prime.logger
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.ws.rs.client.Client
import javax.ws.rs.core.Response

/**
 * Verifies an OAuth2 'access-token' and fetches additional user information
 * using the '.../userinfo' endpoint.
 *
 * Assumes that the OAuth2 vendor is compliant with OpenID.
 *
 * Ref.: http://openid.net/specs/openid-connect-core-1_0.html#UserInfo
 * https://www.dropwizard.io/1.3.2/docs/manual/auth.html#oauth2
 */
class OAuthAuthenticator(private val client: Client,
                         private val key: String) : Authenticator<String, AccessTokenPrincipal> {

    private val LOG by logger()

    @Throws(AuthenticationException::class)
    override fun authenticate(accessToken: String): Optional<AccessTokenPrincipal> {

        val audience = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(accessToken)
                .body
                .audience

        if (audience == null || audience.isEmpty()) {
            LOG.error("No audience field in the 'access-token' claims part")
            throw AuthenticationException("No audience field in the 'access-token' claims part")
        }

        val ep = findFirstMatch("/userinfo$", decodeAudience(audience))

        if (!ep.isPresent) {
            LOG.error("No 'userinfo' endpoint found in audience claim")
            throw AuthenticationException("No 'userinfo' endpoint found in audience claim")
        }

        val userInfo = getUserInfo(ep.get(), accessToken)
        val email = userInfo.email

        return if (email != null && !email.isEmpty())
            Optional.of(AccessTokenPrincipal(email))
        else
            Optional.empty()
    }

    @Throws(AuthenticationException::class)
    private fun getUserInfo(ep: String, accessToken: String): UserInfo {

        val response = client.target(ep)
                .request()
                .accept("application/json")
                .header("Authorization", "Bearer $accessToken")
                .get(Response::class.java)

        if (response.status != Response.Status.OK.statusCode) {
            LOG.error("Unexpected HTTP status {} code when fething 'user-info' from {}",
                    response.status, ep)
            throw AuthenticationException(
                    "Unexpected HTTP status ${response.status} code when fething 'user-info' from $ep")
        }

        val userInfo = response.readEntity(UserInfo::class.java)

        if (userInfo == null) {
            LOG.error("No 'user-info' body part in respose from {}",
                    ep)
            throw AuthenticationException("No 'user-info' body part in respose from $ep")
        }

        return userInfo
    }

    /* As an 'audience' claim can either be a string or an array of strings,
       convert the claim(s) to a list even if it should be a string. */
    private fun decodeAudience(audience: String): List<String> {
        return Arrays.asList(*audience.replace("[\\[\\]]".toRegex(), "").split("\\s*,\\s*".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    private fun findFirstMatch(pattern: String, lst: List<String>): Optional<String> {
        return findMatches(Pattern.compile(pattern), lst)
                .stream()
                .findFirst()
    }

    private fun findMatches(match: Pattern, lst: List<String>): List<String> {
        return lst.stream()
                .filter(match.asPredicate())
                .collect(Collectors.toList())
    }
}
