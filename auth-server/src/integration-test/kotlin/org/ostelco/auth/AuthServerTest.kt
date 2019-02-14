package org.ostelco.auth

import com.google.gson.JsonParser
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.glassfish.jersey.client.JerseyClientBuilder
import org.junit.ClassRule
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals


class AuthServerTest {

    private val msisdn = "MSISDN"
    companion object {

        @JvmField
        @ClassRule
        val RULE = DropwizardAppRule(
                AuthServerApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yaml"))
    }

    @Test
    fun testAuthServer() {

        val response = JerseyClientBuilder().build()
                ?.target("http://0.0.0.0:${RULE.localPort}/auth/token")
                ?.request()
                ?.header("X-MSISDN", msisdn)
                ?.get()

        assertEquals(200, response?.status)

        val customToken = String(response?.readEntity(java.lang.String::class.java)?.toCharArray() ?: CharArray(0)).split(".")

        // println(String(Base64.getDecoder().decode(customToken[0])))
        val header = JsonParser().parse(String(Base64.getDecoder().decode(customToken[0]))).asJsonObject
        assertEquals("RS256", header.get("alg").asString)

        // println(String(Base64.getDecoder().decode(customToken[1])))
        val payload = JsonParser().parse(String(Base64.getDecoder().decode(customToken[1]))).asJsonObject
        assertEquals(msisdn, payload.get("uid").asString)
    }
}
