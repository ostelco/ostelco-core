package org.ostelco.prime.auth

import org.ostelco.prime.model.Identity
import java.security.Principal

/**
 * Holds the 'identity' obtained by verifying and decoding an OAuth2 'access-token'.
 */
class AccessTokenPrincipal(val identity: Identity) : Principal {
    override fun getName(): String = identity.id
}
