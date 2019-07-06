package org.ostelco.prime.auth

import com.fasterxml.jackson.core.type.TypeReference
import io.dropwizard.testing.junit.DropwizardClientRule
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Ignore
import org.junit.Test
import org.ostelco.prime.auth.firebase.FirebaseAuthUtil
import org.ostelco.prime.auth.resources.AppleIdAuthResource
import org.ostelco.prime.auth.resources.LoginToken
import org.ostelco.prime.jsonmapper.objectMapper
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import kotlin.test.assertEquals

class AppleIdAuthTest {

    @Ignore
    @Test
    fun `test - Apple ID Auth test`() {

        val loginToken = resources.target("/appleId/authorize")
                .request()
                .method("POST", Entity.json("""{"authCode":"$AUTH_CODE"}"""), object : GenericType<LoginToken>() {})
        // """{"token":"ACCESS_TOKEN"}"""

        val parts = loginToken.token.split('.')
        assertEquals(3, parts.size)
        assertEquals("""{"alg":"RS256"}""", String(Base64.getDecoder().decode(parts[0])))
        val map: Map<String, *> = objectMapper.readValue(
                String(Base64.getDecoder().decode(parts[1])),
                object : TypeReference<LinkedHashMap<String, *>>() {}
        )
        assertEquals("APPLE_ID", map["uid"])
        assertEquals(
                mapOf(
                        "apple" to mapOf(
                                "identity" to "APPLE_ID",
                                "type" to "APPLE_ID",
                                "provider" to "apple.com"
                        )
                ),
                map["claims"]
        )
    }

    companion object {

        @JvmField
        @ClassRule
        val dropwizardClientRule = DropwizardClientRule(AppleIdAuthServiceEmulator())

        @JvmField
        @ClassRule
        val resources: ResourceTestRule = ResourceTestRule.builder()
                .setMapper(objectMapper)
                .addResource(AppleIdAuthResource())
                .build()

        @BeforeClass
        @JvmStatic
        fun teardown() {

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