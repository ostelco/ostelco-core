package org.ostelco.prime.auth.apple

import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.ostelco.prime.auth.AUTH_CODE
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
import kotlin.test.fail

class AppleIdAuthClientTest {

    @Ignore
    @Test
    fun `test - Apple ID Auth test`() {
        AppleIdAuthClient.authorize(AUTH_CODE)
                .mapLeft { fail("status = ${it.status}, error = ${it.error}") }
    }

    companion object {

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
                            )
            )
        }
    }
}