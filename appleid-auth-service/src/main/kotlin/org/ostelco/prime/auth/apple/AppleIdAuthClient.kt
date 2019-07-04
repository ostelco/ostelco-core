package org.ostelco.prime.auth.apple

import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm.ES256
import org.ostelco.prime.auth.ConfigRegistry.config
import org.ostelco.prime.jersey.client.get
import org.ostelco.prime.jersey.client.post
import java.time.Instant
import java.util.*
import javax.ws.rs.core.Form
import javax.ws.rs.core.MultivaluedHashMap

const val APPLE_ID_SERVICE_URL = "https://appleid.apple.com"

/**
 * https://developer.apple.com/documentation/signinwithapplerestapi
 */
object AppleIdAuthClient {

    fun fetchApplePublicKey() = get<String, JWKSet> {
        target = APPLE_ID_SERVICE_URL
        path = "auth/keys"
    }

    fun authorize(authCode: String) = post<ErrorResponse, TokenResponse>(expectedResultCode = 200) {
        target = APPLE_ID_SERVICE_URL
        path = "/auth/token"
        form = Form(MultivaluedHashMap(mapOf(
                "client_id" to config.clientId,
                "client_secret" to generateClientSecret(),
                "code" to authCode,
                "grant_type" to GrantType.authorization_code.name
        )))
    }

    fun validate(token: String) = post<ErrorResponse, TokenResponse> {
        target = APPLE_ID_SERVICE_URL
        path = "/auth/token"
        form = Form(MultivaluedHashMap(mapOf(
                "client_id" to config.clientId,
                "client_secret" to generateClientSecret(),
                "refresh_token" to token,
                "grant_type" to GrantType.refresh_token.name
        )))
    }

    private fun generateClientSecret(): String {
        val now = Instant.now()
        return Jwts.builder()
                .setHeader("kid" to config.keyId)
                .setIssuer(config.teamId)
                .setIssuedAt(Date(now.toEpochMilli()))
                .setExpiration(Date(now.plusSeconds(300).toEpochMilli()))
                .setAudience(APPLE_ID_SERVICE_URL)
                .setSubject(config.clientId)
                .signWith(ES256, config.privateKey)
                .compact()
    }

    private fun JwtBuilder.setHeader(header: Pair<String, String>): JwtBuilder {
        this.setHeader(mapOf(header))
        return this
    }
}