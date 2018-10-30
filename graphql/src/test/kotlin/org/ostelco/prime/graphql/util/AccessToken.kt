package org.ostelco.prime.graphql.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm

object AccessToken {

    private val key = "secret"
    private val namespace = "https://ostelco.org"

    fun withEmail(email: String): String {

        val audience = listOf("http://kmmtest", "$namespace/userinfo")

        return withEmail(email, audience)
    }

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