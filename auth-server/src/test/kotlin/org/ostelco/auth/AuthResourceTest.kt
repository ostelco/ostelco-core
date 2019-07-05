package org.ostelco.auth

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.auth.resources.AuthResource
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals

class AuthResourceTest {

    @Test
    fun testAuthResourceForMissingHeader() {

        val statusCode = resources
                .target("/auth/token")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.INTERNAL_SERVER_ERROR.statusCode, statusCode)
    }

    companion object {

        @JvmField
        @ClassRule
        val resources: ResourceTestRule = ResourceTestRule.builder()
                .addResource(AuthResource())
                .build()
    }
}