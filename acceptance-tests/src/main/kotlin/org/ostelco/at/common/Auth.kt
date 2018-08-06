package org.ostelco.at.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm

private const val JWT_SIGNING_KEY = "jwt_secret"

fun generateAccessToken(subject: String): String = Jwts.builder()
        .setClaims(mapOf(
                "aud" to "http://ext-auth-provider:8080/userinfo",
                "sub" to subject))
        .signWith(SignatureAlgorithm.HS512, JWT_SIGNING_KEY.toByteArray())
        .compact()