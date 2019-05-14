package org.ostelco.at.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm

private const val JWT_SIGNING_KEY = "jwt_secret"

object Auth {
    fun generateAccessToken(email: String): String = Jwts.builder()
            .setClaims(mapOf(
                    "https://ostelco/email" to email,
                    "aud" to "http://ext-auth-provider:8080/userinfo",
                    "sub" to email))
            .signWith(SignatureAlgorithm.HS512, JWT_SIGNING_KEY.toByteArray())
            .compact()
}