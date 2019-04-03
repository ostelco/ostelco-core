package org.ostelco.prime.auth

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import io.dropwizard.auth.AuthenticationException
import io.dropwizard.auth.Authenticator
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.objectMapper
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

private const val DEFAULT_USER_INFO_ENDPOINT = "https://auth.oya.world/userinfo"
private const val NAMESPACE = "https://ostelco"

class OAuthAuthenticator(private val client: Client) : Authenticator<String, AccessTokenPrincipal> {

    private val logger by getLogger()

    override fun authenticate(accessToken: String): Optional<AccessTokenPrincipal> {

        var userInfoEndpoint = DEFAULT_USER_INFO_ENDPOINT

        var provider  = ""
        try {
            val claims = getClaims(accessToken)
            if (claims != null) {

                val email =  getEmail(claims)
                provider = getSubjectPrefix(claims)
                if(email != null) {
                    return Optional.of(AccessTokenPrincipal(email = email, provider = provider))
                }

                userInfoEndpoint = getUserInfoEndpointFromAudience(claims)
            }
        } catch (e: Exception) {
            logger.error("No audience field in the 'access-token' claims part", e)
        }

        val userInfo = getUserInfo(userInfoEndpoint, accessToken)
        val email = userInfo.email

        if (email == null || email.isEmpty()) {
            logger.warn("email is missing in userInfo")
            return Optional.empty()
        }
        return Optional.of(AccessTokenPrincipal(email = email, provider = provider))
    }

    private fun getUserInfo(userInfoEndpoint: String, accessToken: String): UserInfo {

        val response = client.target(userInfoEndpoint)
                .request()
                .accept("application/json")
                .header("Authorization", "Bearer $accessToken")
                .get(Response::class.java)

        if (response.status != Response.Status.OK.statusCode) {
            logger.error("Unexpected HTTP status {} code when fetching 'user-info' from {}",
                    response.status,
                    userInfoEndpoint)
            throw AuthenticationException(
                    "Unexpected HTTP status ${response.status} code when fetching 'user-info' from $userInfoEndpoint")
        }

        val userInfo = response.readEntity(UserInfo::class.java)

        if (userInfo == null) {
            logger.error("No 'user-info' body part in response from {}", userInfoEndpoint)
            throw AuthenticationException("No 'user-info' body part in response from $userInfoEndpoint")
        }

        return userInfo
    }

    private fun getUserInfoEndpointFromAudience(claims: JsonNode): String {

        if (claims.has("aud")) {
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
        logger.error("No audience field in the 'access-token' claims")
        return DEFAULT_USER_INFO_ENDPOINT
    }

    /* Extracts 'claims' part from JWT token.
       Throws 'illegalargumentexception' exception on error. */
    private fun getClaims(token: String): JsonNode? {
        if (token.codePoints().filter { ch -> ch == '.'.toInt() }.count() != 2L) {
            throw java.lang.IllegalArgumentException("The provided token is an Invalid JWT token")
        }
        val parts = token.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return String(Base64.getDecoder().decode(parts[1]
                .replace("-", "+")
                .replace("_", "/")))
                .let(::decodeClaims)
    }

    /* Decodes the claims part of a JWT token.
       Returns null on error. */
    private fun decodeClaims(claims: String): JsonNode? {
        try {
            return objectMapper.readTree(claims)
        } catch (e: JsonParseException) {
            logger.error("Parsing of the provided json doc {} failed: {}", claims, e)
        } catch (e: IOException) {
            logger.error("Unexpected error when parsing the json doc {}: {}", claims, e)
        }
        return null
    }

    private fun getEmail(claims: JsonNode): String? {

        return if (claims.has("$NAMESPACE/email")) {
            claims.get("$NAMESPACE/email").textValue()
        } else {
            logger.error("Missing '{}/email' field in claims part of JWT token {}",
                    NAMESPACE, claims)
            null
        }
    }

    private fun getSubjectPrefix(claims: JsonNode): String {
        return if (claims.has("sub")) {
            claims.get("sub").textValue().split('|').first()
        } else {
            logger.error("Missing 'sub' field in claims part of JWT token {}", claims)
            ""
        }
    }
}
