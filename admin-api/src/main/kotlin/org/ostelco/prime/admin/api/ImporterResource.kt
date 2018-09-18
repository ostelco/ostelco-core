package org.ostelco.prime.admin.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.ostelco.prime.admin.importer.ImportDeclaration
import org.ostelco.prime.admin.importer.ImportProcessor
import org.ostelco.prime.logger
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Response

/**
 * Resource used to handle the importer related REST calls.
 */
@Path("/importer")
class ImporterResource(val processor: ImportProcessor) {

    private val logger by logger()

    /**
     * Upload a new import specification
     */
    @POST
    @Consumes("text/vnd.yaml")
    fun postStatus(yaml: String): Response {
        logger.info("POST status for importer")

        return try {
            val mapper = ObjectMapper(YAMLFactory())
            val declaration: ImportDeclaration =
                    mapper.readValue(yaml, ImportDeclaration::class.java)

            val result: Boolean = processor.import(declaration)

            if (result) {
                Response.ok().build()
            } else {
                Response.status(Response.Status.BAD_REQUEST).build()
            }
        } catch (e: Exception) {
            logger.error("Failed to Import", e)
            Response.serverError().build()
        }
    }
}