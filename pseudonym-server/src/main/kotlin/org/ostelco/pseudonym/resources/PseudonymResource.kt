package org.ostelco.pseudonym.resources

import org.hibernate.validator.constraints.NotBlank
import org.ostelco.prime.getLogger
import org.ostelco.pseudonym.service.PseudonymizerServiceSingleton
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status


/**
 * Class representing the Export task entity in Datastore.
 */
data class ExportTask(val exportId: String, val status: String, val error: String)

/**
 * Resource used to handle the pseudonym related REST calls. The map of pseudonym objects
 * are store in datastore. The key for the object is made from "<msisdn>-<start timestamp ms>.
 */
@Path("/pseudonym")
class PseudonymResource {

    private val logger by getLogger()

    /**
     * Exports all pseudonyms to a bigquery table. the name is generated from the exportId
     * parameter. The exportId is expected to be a UUIDV4. The '-' are removed from the name.
     * Return Ok when the process has started, used /exportstatus to get the result.
     */
    @GET
    @Path("/export/{exportId}")
    fun exportPseudonyms(@NotBlank @PathParam("exportId") exportId: String): Response {
        logger.info("GET export all pseudonyms to the table $exportId")
        PseudonymizerServiceSingleton.exportMsisdnPseudonyms(exportId = exportId)
        return Response.ok("Started Exporting", MediaType.TEXT_PLAIN).build()
    }

    /**
     * Returns the result of the export. Return 404 if exportId id not found in the system
     */
    @GET
    @Path("/exportstatus/{exportId}")
    fun getExportStatus(@NotBlank @PathParam("exportId") exportId: String): Response {
        logger.info("GET status of export $exportId")
        return PseudonymizerServiceSingleton.getExportTask(exportId)
                ?.let { Response.ok(it, MediaType.APPLICATION_JSON).build() }
                ?: Response.status(Status.NOT_FOUND).build()
    }
}
