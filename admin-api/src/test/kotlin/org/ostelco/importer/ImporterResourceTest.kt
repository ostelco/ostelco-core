package org.ostelco.importer

import arrow.core.Either
import io.dropwizard.testing.FixtureHelpers.fixture
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.Assert.assertEquals
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.prime.admin.api.ImporterResource
import org.ostelco.prime.admin.api.YamlMessageBodyReader
import org.ostelco.prime.admin.importer.ImportDeclaration
import org.ostelco.prime.admin.importer.ImportProcessor
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.model.Price
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response.Status


/**
 * Class for unit testing ImporterResource.
 */
class ImporterResourceTest {

    companion object {

        lateinit var importedResource: ImportDeclaration

        private val processor: ImportProcessor = object : ImportProcessor {
            override fun import(importDeclaration: ImportDeclaration): Either<ApiError, Unit> {
                importedResource = importDeclaration
                return Either.right(Unit)
            }
        }

        @ClassRule
        @JvmField
        val resources: ResourceTestRule? = ResourceTestRule.builder()
                .addResource(ImporterResource(processor))
                .addProvider(YamlMessageBodyReader::class.java)
                .build()
    }

    @Test
    fun `test creating offer with products and segments`() {

        val text: String = fixture("sample-offer-products-segments.yaml")

        val response = resources
                ?.target("/importer")
                ?.request("text/vnd.yaml")
                ?.post(Entity.entity(text, "text/vnd.yaml"))

        assertEquals(response?.readEntity(String::class.java), Status.CREATED.statusCode, response?.status)
        assertEquals("Simple agent", importedResource.producingAgent.name)
        assertEquals("1.0", importedResource.producingAgent.version)

        // check offer
        assertEquals("test-offer", importedResource.offer.id)
        assertEquals(emptyList<String>(), importedResource.offer.products)
        assertEquals(emptyList<String>(), importedResource.offer.segments)

        // check product
        assertEquals(1, importedResource.products.size)
        val product = importedResource.products.first()
        assertEquals("1GB_249NOK", product.sku)
        assertEquals(Price(249, "NOK"), product.price)
        assertEquals(mapOf("noOfBytes" to "1_000_000_000"), product.properties)
        assertEquals(
                mapOf("isDefault" to "true",
                        "offerLabel" to "Default Offer",
                        "priceLabel" to "249 NOK"),
                product.presentation)

        // check segment
        assertEquals(1, importedResource.segments.size)
        val segment = importedResource.segments.first()
        assertEquals("test-segment", segment.id)
        assertEquals(emptyList<String>(), segment.subscribers)
    }

    @Test
    fun `test creating offer using existing products and segments`() {

        val text: String = fixture("sample-offer-only.yaml")

        val response = resources
                ?.target("/importer")
                ?.request("text/vnd.yaml")
                ?.post(Entity.entity(text, "text/vnd.yaml"))

        assertEquals(response?.readEntity(String::class.java), Status.CREATED.statusCode, response?.status)
        assertEquals("Simple agent", importedResource.producingAgent.name)
        assertEquals("1.0", importedResource.producingAgent.version)

        // check offer
        assertEquals("test-offer", importedResource.offer.id)
        assertEquals(listOf("1GB_249NOK"), importedResource.offer.products)
        assertEquals(listOf("test-segment"), importedResource.offer.segments)

        // check product
        assertEquals(0, importedResource.products.size)

        // check segment
        assertEquals(0, importedResource.segments.size)
    }

    /**
     *  Testing reading a yaml file.
     */
    /*
    @Test
    fun `test creating offer with products and segments`() {

        val text: String =
                this::class.java.classLoader.getResource("sample-offer-legacy.yaml").readText(Charsets.UTF_8)

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
    */
}