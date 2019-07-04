package org.ostelco.prime.auth

import com.fasterxml.jackson.databind.JsonNode
import io.dropwizard.auth.Authenticator
import org.ostelco.prime.getLogger
import org.ostelco.prime.jwt.JwtParser
import java.util.*
import javax.ws.rs.client.Client


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

    private val logger by getLogger()

    override fun authenticate(accessToken: String): Optional<AccessTokenPrincipal> {
        try {
            val claims = JwtParser.getClaims(accessToken)
            if (claims != null) {
                return when {
                    isApple(claims) -> AppleIdAuthenticator(claims).authenticate()
                    isFirebase(claims) -> FirebaseAuthenticator(claims).authenticate()
                    else -> Auth0Authenticator(client, claims).authenticate(accessToken)
                }
            }
        } catch (e: Exception) {
            logger.error("Could not parse claims from the 'access-token'", e)
        }
        return Optional.empty()
    }

    private fun isFirebase(claims: JsonNode): Boolean {
        return claims.has("firebase")
    }

    private fun isApple(claims: JsonNode): Boolean {
        return claims.has("apple")
    }
}