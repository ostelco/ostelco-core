package org.ostelco.at.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys

object Auth {
    fun generateAccessToken(email: String): String = Jwts.builder()
            .setClaims(mapOf(
                    "https://ostelco/email" to email,
                    "aud" to "http://ext-auth-provider:8080/userinfo",
                    "sub" to email))
            .signWith(
                    Keys.secretKeyFor(SignatureAlgorithm.HS512),
                    SignatureAlgorithm.HS512
            )
            .compact()
}