package com.telenordigital.ostelco.auth

import com.telenordigital.ostelco.auth.resources.AuthResource
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals

class AuthResourceTest {

    companion object {

        @JvmField
        @ClassRule
        val resources = ResourceTestRule.builder()
                .addResource(AuthResource())
                .build()
    }

    @Test
    fun testAuthResourceForMissingHeader() {

        val statusCode = resources
                ?.target("/auth/token")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.INTERNAL_SERVER_ERROR.statusCode, statusCode)
    }
}