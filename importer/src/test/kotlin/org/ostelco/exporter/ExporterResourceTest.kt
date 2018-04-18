package org.ostelco.importer

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response.Status
import javax.xml.ws.Response
import kotlin.test.assertEquals


/**
 * Class for unit testing ImporterResource.
 */
class ImporterResourceTest {
    private val pathForGetStatus = "/importer/get/status"

    companion object {

        var importedResource: ImportDeclaration? = null

        val processor: ImportProcessor = object : ImportProcessor {
            public override fun import(decl: ImportDeclaration) : Boolean {
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

    /**
     * Test status API
     */
    @Test
    fun testGettingStatus() {

        val statusCode = resources
                ?.target("$pathForGetStatus")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.OK.statusCode, statusCode)
    }


    /**
     *  Testing reading a yaml file.
     */
    @Test
    fun testPostingConfig() {

        val text: String =
                this::class.java.classLoader.getResource("sample-offer-yaml.yaml").readText(Charsets.UTF_8)

        val response = resources
                ?.target("importer")
                ?.request("text/vnd.yaml")
                ?.post(Entity.entity(text, "text/vnd.yaml"))!!
        assertEquals(Status.OK.statusCode, response.status)
        // assertEquals("bar", importedResource?.foo)
    }
}