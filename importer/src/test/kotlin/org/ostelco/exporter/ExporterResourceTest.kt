package org.ostelco.importer

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.importer.resources.ImporterResource
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals

/**
 * Class for unit testing ImporterResource.
 */
class ImporterResourceTest {
    private val pathForGetStatus = "/importer/get/status"

    companion object {
        @ClassRule
        @JvmField
        val resources = ResourceTestRule.builder()
                .addResource(ImporterResource())
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