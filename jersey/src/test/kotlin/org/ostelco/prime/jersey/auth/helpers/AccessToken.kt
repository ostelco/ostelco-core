package org.ostelco.prime.jersey.auth.helpers

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm

object AccessToken {

    private const val key = "secret"
    private const val namespace = "https://ostelco.org"

    fun withEmail(email: String, audience: List<String>): String {

        val claims = mapOf("$namespace/email" to email,
                        "aud" to audience,
                        "sub" to email)

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, key)
                .compact()
    }
}
