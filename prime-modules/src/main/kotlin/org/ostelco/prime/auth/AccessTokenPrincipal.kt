package org.ostelco.prime.auth

import java.security.Principal

/**
 * Holds the 'user-id' obtained by verifying and decoding an OAuth2
 * 'access-token'.
 */
class AccessTokenPrincipal(private val email: String, val provider: String) : Principal {
    override fun getName(): String = email
}
