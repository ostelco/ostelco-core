package org.ostelco.prime.client.api.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.vavr.collection.Array
import io.vavr.collection.HashMap

object AccessToken {

    private val key = "secret"
    private val namespace = "https://ostelco.org"

    fun withEmail(email: String): String {

        val audience = Array.of("http://kmmtest", "$namespace/userinfo")
                .toJavaList()

        return withEmail(email, audience)
    }

    fun withEmail(email: String, audience: List<String>): String {

        val claims = HashMap
                .of("$namespace/email", email as Any,
                        "aud", audience as Any,
                        "sub", email as Any)
                .toJavaMap()

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, key)
                .compact()
    }
}
