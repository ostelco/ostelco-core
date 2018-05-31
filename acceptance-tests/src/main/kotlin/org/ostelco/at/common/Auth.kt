package org.ostelco.at.common

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm

private val jwtSigningKey = "jwt_secret"

val accessToken = Jwts.builder()
        .setClaims(mapOf("aud" to "http://ext-auth-provider:8080/userinfo"))
        .signWith(SignatureAlgorithm.HS512, jwtSigningKey)
        .compact()