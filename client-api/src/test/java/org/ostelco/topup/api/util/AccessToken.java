package org.ostelco.topup.api.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vavr.collection.HashMap;
import java.util.List;
import io.vavr.collection.Array;

import java.util.Map;

public class AccessToken {

    private final static String key = "secret";
    private final static String namespace = "https://ostelco.org";

    private AccessToken() {
    }

    public static String withEmail(final String email) {

        List<String> audience = Array.of("http://kmmtest", namespace + "/userinfo")
            .toJavaList();

        return AccessToken.withEmail(email, audience);
    }

    public static String withEmail(final String email, List<String> audience) {

        final Map<String, Object> claims = HashMap
                .of(namespace + "/email", (Object) email,
                    "aud", (Object) audience,
                    "sub", (Object) email)
                .toJavaMap();

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();
    }
}
