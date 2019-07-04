package org.ostelco.prime.auth

import com.fasterxml.jackson.databind.JsonNode
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Identity
import java.util.*

class FirebaseAuthenticator(private val claims: JsonNode) {

    private val logger by getLogger()

    fun authenticate(): Optional<AccessTokenPrincipal> {
        val email = getEmail(claims)
        val provider = getProvider(claims)
        return if (email.isNullOrBlank()) {
            logger.warn("email is missing in userInfo")
            Optional.empty()
        } else {
            Optional.of(AccessTokenPrincipal(Identity(id = email, type = "EMAIL", provider = provider)))
        }
    }

    private fun getEmail(claims: JsonNode): String? {
        return when {
            claims.path("firebase").path("identities").has("email") -> {
                claims.path("firebase")
                        .path("identities")
                        .get("email")
                        .get(0)
                        .textValue()
            }
            else -> {
                logger.error("Missing 'email' field in claims part of JWT token {}", claims)
                null
            }
        }
    }

    private fun getProvider(claims: JsonNode): String {
        return when {
            claims.path("firebase").has("sign_in_provider") -> claims.path("firebase").get("sign_in_provider").textValue()
            else -> {
                logger.error("Unsupported firebase type")
                "firebase"
            }
        }
    }
}
