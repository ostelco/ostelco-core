package org.ostelco.prime.auth

import com.fasterxml.jackson.databind.JsonNode
import org.ostelco.prime.model.Identity
import java.util.*

class AppleIdAuthenticator(private val claims: JsonNode) {

    fun authenticate(): Optional<AccessTokenPrincipal> {
        val apple = claims.path("apple")
        val identity: String? = apple.get("identity").textValue()
        val provider: String? = apple.get("provider").textValue()
        val type: String? = apple.get("type").textValue()
        return if (identity != null && type != null && provider != null) {
            Optional.of(AccessTokenPrincipal(Identity(id = identity, type = type, provider = provider)))
        } else {
            Optional.empty()
        }
    }
}
