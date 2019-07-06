package org.ostelco.prime.graphql.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys

object AccessToken {

    private val namespace = "https://ostelco.org"

    fun withEmail(email: String): String {

        val audience = listOf("http://kmmtest", "$namespace/userinfo")

        return withEmail(email, audience)
    }

    private fun withEmail(email: String, audience: List<String>): String {

        val claims = mapOf(
                "$namespace/email" to email,
                "aud" to audience,
                "sub" to email
        )

        return Jwts.builder()
                .setClaims(claims)
                .signWith(
                        Keys.secretKeyFor(SignatureAlgorithm.HS512),
                        SignatureAlgorithm.HS512
                )
                .compact()
    }
}