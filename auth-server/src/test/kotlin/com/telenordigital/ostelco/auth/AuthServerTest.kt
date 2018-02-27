package com.telenordigital.ostelco.auth

import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.glassfish.jersey.client.JerseyClientBuilder
import org.junit.ClassRule
import org.junit.Test
import kotlin.test.assertEquals


class AuthServerTest {

    companion object {

        @JvmField
        @ClassRule
        val RULE = DropwizardAppRule(
                AuthServerApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yaml"))
    }

    @Test
    fun testAuthServer() {

        assertEquals(
                200,
                JerseyClientBuilder().build()
                        ?.target("http://0.0.0.0:${RULE.getLocalPort()}/auth/token")
                        ?.request()
                        ?.header("X-MSISDN", "4746862166")
                        ?.get()?.status)

        // TODO validate the JWT received in response
    }
}
