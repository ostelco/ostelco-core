package org.ostelco.at.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm

private const val JWT_SIGNING_KEY = "jwt_secret"

val accessToken: String = Jwts.builder()
        .setClaims(mapOf("aud" to "http://ext-auth-provider:8080/userinfo"))
        .signWith(SignatureAlgorithm.HS512, JWT_SIGNING_KEY)
        .compact()