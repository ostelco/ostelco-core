package org.ostelco.prime.auth.apple

import io.dropwizard.testing.junit.DropwizardClientRule
import io.jsonwebtoken.Jwts
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Ignore
import org.junit.Test
import org.ostelco.prime.auth.AUTH_CODE
import org.ostelco.prime.auth.AppleIdAuthServiceEmulator
import org.ostelco.prime.auth.CLIENT_ID
import org.ostelco.prime.auth.ConfigRegistry
import org.ostelco.prime.auth.InternalConfig
import org.ostelco.prime.auth.KEY_ID
import org.ostelco.prime.auth.PRIVATE_KEY
import org.ostelco.prime.auth.TEAM_ID
import org.ostelco.prime.auth.firebase.FirebaseAuthUtil
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.fail

class AppleIdAuthClientTest {

    @Ignore
    @Test
    fun `test - Apple ID Auth test`() {
        AppleIdAuthClient.authorize(AUTH_CODE)
                .bimap(
                        { fail("status = ${it.status}, error = ${it.error}") },
                        {
                            assertEquals(
                                    TokenResponse(
                                            access_token = "ACCESS_TOKEN",
                                            expires_in = it.expires_in,
                                            id_token = Jwts.builder().setSubject("APPLE_ID").compact(),
                                            refresh_token = "REFRESH_TOKEN",
                                            token_type = "bearer"),
                                    it
                            )
                        }
                )
    }

    companion object {

        @JvmField
        @ClassRule
        val dropwizardClientRule = DropwizardClientRule(AppleIdAuthServiceEmulator())

        @BeforeClass
        @JvmStatic
        fun setup() {

            FirebaseAuthUtil.initUsingServiceAccount("../prime/config/prime-service-account.json")

            ConfigRegistry.config = InternalConfig(
                    teamId = TEAM_ID,
                    keyId = KEY_ID,
                    clientId = CLIENT_ID,
                    privateKey = KeyFactory
                            .getInstance("EC")
                            .generatePrivate(
                                    PKCS8EncodedKeySpec(
                                            Base64.getDecoder().decode(PRIVATE_KEY)
                                    )
                            ),
                    appleIdServiceUrl = dropwizardClientRule.baseUri().toASCIIString()
            )
        }
    }
}