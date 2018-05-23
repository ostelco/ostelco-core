package org.ostelco.topup.api.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.collection.HashMap;

import java.util.Map;

public class AccessToken {

    private AccessToken() {
    }

    public static String withEmail(final String email) {

        final String key = "secret";
        final String namespace = "https://ostelco.org";

        final Map<String, Object> claims = HashMap
                .of(namespace + "/email", (Object) email)
                .toJavaMap();

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();
    }
}
