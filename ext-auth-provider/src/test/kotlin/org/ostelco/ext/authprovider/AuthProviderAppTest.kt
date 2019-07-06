package org.ostelco.ext.authprovider

import io.dropwizard.testing.junit.ResourceTestRule
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.junit.ClassRule
import org.junit.Test
import kotlin.test.assertEquals

fun generateAccessToken(subject: String): String = Jwts.builder()
        .setClaims(mapOf(
                "aud" to "http://ext-auth-provider:8080/userinfo",
                "sub" to subject))
        .signWith(JWT_SIGNING_KEY, SignatureAlgorithm.HS512)
        .compact()

data class UserInfo(var email: String? = null)

class AuthProviderAppTest {

    @Test
    fun testGetUserInfo() {

        val userInfo = resources.target("/userinfo")
                .request()
                .header("Authorization", "Bearer ${generateAccessToken("foo@bar.com")}")
                .get(UserInfo::class.java)

        assertEquals("foo@bar.com", userInfo.email)
    }

    companion object {

        @ClassRule
        @JvmField
        val resources:ResourceTestRule = ResourceTestRule.builder()
                .addResource(UserInfoResource())
                .build()
    }
}

