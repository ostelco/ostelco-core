package org.ostelco.importer

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.prime.admin.api.ImporterResource
import org.ostelco.prime.admin.importer.ImportDeclaration
import org.ostelco.prime.admin.importer.ImportProcessor
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response.Status


/**
 * Class for unit testing ImporterResource.
 */
class ImporterResourceTest {
    private val pathForGetStatus = "/importer/status"

    companion object {

        var importedResource: ImportDeclaration? = null

        val processor: ImportProcessor = object : ImportProcessor {
            override fun import(decl: ImportDeclaration) : Boolean {
                importedResource = decl
                return true
            }
        }

        @ClassRule
        @JvmField
        val resources = ResourceTestRule.builder()
                .addResource(ImporterResource(processor))
                .build()
    }

    @Before
    fun setUp() {
        importedResource = null
    }

    /**
     *  Testing reading a yaml file.
     */
    @Test
    fun testPostingConfig() {

        val text: String =
                this::class.java.classLoader.getResource("sample-offer-yaml.yaml").readText(Charsets.UTF_8)

        val response = resources
                ?.target("/importer")
                ?.request("text/vnd.yaml")
                ?.post(Entity.entity(text, "text/vnd.yaml"))

        assertEquals(Status.OK.statusCode, response?.status)
        assertEquals("Simple agent", importedResource?.producingAgent?.name)
        assertEquals("1.0", importedResource?.producingAgent?.version)
        assertEquals("2018-02-22T12:41:49.871Z", importedResource?.offer?.visibility?.from)
        assertEquals("2018-02-22T12:41:49.871Z", importedResource?.offer?.visibility?.to)

        // Missing tests for presentation, financials, product within offer, and everything within segment.

        System.out.println("members = " + importedResource?.segment?.members?.members)
    }
}