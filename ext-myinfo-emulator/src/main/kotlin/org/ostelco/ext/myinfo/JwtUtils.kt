package org.ostelco.ext.myinfo

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm.RS256
import org.ostelco.ext.myinfo.JsonUtils.compactJson
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*

object JwtUtils {

    fun createAccessToken(signingPrivateKey: String): String {

        val now = Instant.now()
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("zip", "NONE")
                .setHeaderParam("kid", "AldO1lzsCInrpAQ2FF29oJsAGHk=")
                .setPayload(compactJson("""
            {
              "sub": "S9812381D",
              "auth_level": 0,
              "auditTrackingId": "35a7f7ff-90cc-4368-9f00-26bba249629a",
              "iss": "https://stg-home.singpass.gov.sg/consent/oauth2/consent/myinfo-com",
              "tokenName": "access_token",
              "token_type": "Bearer",
              "authGrantId": "1659e065-7cdb-4d9e-af54-54e6b1fab953",
              "aud": "myinfo",
              "nbf": 1552124436,
              "grant_type": "authorization_code",
              "scope": [
                "name",
                "sex",
                "dob",
                "residentialstatus",
                "nationality",
                "mobileno",
                "email",
                "mailadd"
              ],
              "auth_time": 1552124419000,
              "realm": "/consent/myinfo-com",
              "exp": ${now.plusMillis(10000).toEpochMilli()},
              "iat": ${now.toEpochMilli()},
              "expires_in": 1800,
              "jti": "be832d1c-21b7-4ba3-8c04-ec78a0928533"
            }
            """))
                .signWith(RS256, KeyFactory
                        .getInstance("RSA")
                        .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(signingPrivateKey))))
                .compact()
    }

    fun getClaims(accessToken: String, signingPublicKey: String) = Jwts.parser()
            .setCompressionCodecResolver(ExtendedCompressionCodecResolver)
            .setSigningKey(KeyFactory
                    .getInstance("RSA")
                    .generatePublic(X509EncodedKeySpec(Base64
                            .getDecoder()
                            .decode(signingPublicKey))))
            .parseClaimsJws(accessToken)

    fun createJws(signingPrivateKey: String, payload: String): String {
        return Jwts.builder()
                .setHeaderParam("alg", "RS256")
                .setHeaderParam("kid", "C6Q-0bsHc4qyNq6MBEtftpB-DsTHNth4ZnlrFPUQ8PI")
                .setPayload(payload)
                .signWith(RS256, KeyFactory
                        .getInstance("RSA")
                        .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(signingPrivateKey))))
                .compact()
    }
}

fun main() {
    val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA")
            .apply { this.initialize(2048) }
            .genKeyPair()


    println("-----BEGIN CERTIFICATE-----\n" +
            "${Base64.getMimeEncoder(
                    64,
                    System.lineSeparator().toByteArray())
                    .encodeToString(keyPair.public.encoded)}\n" +
            "-----END CERTIFICATE-----")

    println(Base64.getEncoder().encodeToString(keyPair.public.encoded))
    println(Base64.getEncoder().encodeToString(keyPair.private.encoded))
}