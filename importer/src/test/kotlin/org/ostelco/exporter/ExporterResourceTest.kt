package org.ostelco.importer

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.importer.ImporterResource
import javax.ws.rs.core.Response
import javax.ws.rs.client.Entity
import kotlin.test.assertEquals
import javax.ws.rs.core.Response.Status
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory



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
/*
    val classLoader = getClass().getClassLoader()
    val file = File(classLoader.getResource("deals.yaml").getFile())
    */

    /**
     *  Testing reading a yaml file.
     */
    @Test
    fun testPostingConfig() {

        val yamlText:String = """
foo: bar
            """
        val response: Response = resources
                ?.target("importer")
                ?.request("text/vnd.yaml")
                ?.post(Entity.entity(yamlText, "text/vnd.yaml"))!!
        assertEquals(Status.OK.statusCode, response.status)
        assertEquals("bar", importedResource?.foo)
    }
}