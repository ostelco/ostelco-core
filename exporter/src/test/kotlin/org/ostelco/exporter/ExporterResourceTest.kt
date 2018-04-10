package org.ostelco.exporter

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.exporter.resources.ExporterResource
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals

/**
 * Class for unit testing ExporterResource.
 */
class ExporterResourceTest {
    private val pathForGetStatus = "/exporter/get/status"

    companion object {
        @ClassRule
        @JvmField
        val resources = ResourceTestRule.builder()
                .addResource(ExporterResource())
                .build()
    }
    /**
     * Test status API
     */
    @Test
    fun testPseudonymResource() {

        val statusCode = resources
                ?.target("$pathForGetStatus")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.OK.statusCode, statusCode)
    }
}