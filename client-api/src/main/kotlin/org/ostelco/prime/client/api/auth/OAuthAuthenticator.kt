package org.ostelco.prime.client.api.auth

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.dropwizard.auth.AuthenticationException
import io.dropwizard.auth.Authenticator
import org.ostelco.prime.client.api.core.UserInfo
import org.ostelco.prime.logger
import java.io.IOException
import java.util.*
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
class OAuthAuthenticator(private val client: Client) : Authenticator<String, AccessTokenPrincipal> {

    private val LOG by logger()

    private val MAPPER = ObjectMapper()

    private val DEFAULT_USER_INFO_ENDPOINT = "https://ostelco.eu.auth0.com/userinfo"

    @Throws(AuthenticationException::class)
    override fun authenticate(accessToken: String): Optional<AccessTokenPrincipal> {

        var userInfoEndpoint: String

        try {
            val claims = decodeClaims(getClaims(accessToken))
            userInfoEndpoint = getUserInfoEndpointFromAudience(claims)
        } catch (e: Exception) {
            LOG.error("No audience field in the 'access-token' claims part", e)
            userInfoEndpoint = DEFAULT_USER_INFO_ENDPOINT
        }

        val userInfo = getUserInfo(userInfoEndpoint, accessToken)
        val email = userInfo.email

        if (email == null || email.isEmpty()) {
            return Optional.empty()
        }
        return Optional.of(AccessTokenPrincipal(email))
    }

    @Throws(AuthenticationException::class)
    private fun getUserInfo(userInfoEndpoint: String, accessToken: String): UserInfo {

        val response = client.target(userInfoEndpoint)
                .request()
                .accept("application/json")
                .header("Authorization", "Bearer $accessToken")
                .get(Response::class.java)

        if (response.status != Response.Status.OK.statusCode) {
            LOG.error("Unexpected HTTP status {} code when fetching 'user-info' from {}",
                    response.status,
                    userInfoEndpoint)
            throw AuthenticationException(
                    "Unexpected HTTP status ${response.status} code when fetching 'user-info' from $userInfoEndpoint")
        }

        val userInfo = response.readEntity(UserInfo::class.java)

        if (userInfo == null) {
            LOG.error("No 'user-info' body part in response from {}", userInfoEndpoint)
            throw AuthenticationException("No 'user-info' body part in response from $userInfoEndpoint")
        }

        return userInfo
    }

    private fun getUserInfoEndpointFromAudience(claims: JsonNode?): String {

        if (claims?.has("aud") == true) {
            val audienceNode = claims.get("aud")

            if (audienceNode.isTextual) {
                val audience = audienceNode.asText()
                if(audience.endsWith("/userinfo")) {
                    return audience
                }
            } else if (audienceNode.isArray) {
                return audienceNode.asIterable()
                        .toList()
                        .map { it.asText() }
                        .first {
                            it.endsWith("/userinfo")
                        }
            }
        }
        LOG.error("No audience field in the 'access-token' claims")
        return DEFAULT_USER_INFO_ENDPOINT
    }

    /* Extracts 'claims' part from JWT token.
       Throws 'illegalargumentexception' exception on error. */
    private fun getClaims(token: String): String {
        if (token.codePoints().filter { ch -> ch == '.'.toInt() }.count() != 2L) {
            throw IllegalArgumentException("The provided token is an Invalid JWT token")
        }
        val parts = token.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return String(Base64.getDecoder().decode(parts[1]
                .replace("-", "+")
                .replace("_", "/")))
    }

    /* Decodes the claims part of a JWT token.
       Returns null on error. */
    private fun decodeClaims(claims: String): JsonNode? {
        var obj: JsonNode? = null
        try {
            obj = MAPPER.readTree(claims)
        } catch (e: JsonParseException) {
            LOG.error("Parsing of the provided json doc {} failed: {}", claims, e)
        } catch (e: IOException) {
            LOG.error("Unexpected error when parsing the json doc {}: {}", claims, e)
        }
        return obj
    }
}
