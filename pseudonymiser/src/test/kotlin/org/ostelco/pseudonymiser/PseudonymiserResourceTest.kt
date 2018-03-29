package org.ostelco.pseudonymiser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.pseudonymiser.resources.PseudonymEntity
import org.ostelco.pseudonymiser.resources.PseudonymiserResource
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Class for unit testing PseudonymiserResource.
 */
class PseudonymiserResourceTest {

    companion object {

        @ClassRule
        @JvmField
        val resources = ResourceTestRule.builder()
                .addResource(PseudonymiserResource())
                .build()
    }
    val mapper = jacksonObjectMapper()
    /**
     * Test a normal request will all parameters
     */
    @Test
    fun testPseudonymiser() {
        val statusCode = resources
                ?.target("/pseudonymiser/status")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.OK.statusCode, statusCode)
    }
}