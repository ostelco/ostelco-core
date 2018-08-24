package org.ostelco.pseudonym.resources

import org.hibernate.validator.constraints.NotBlank
import org.ostelco.pseudonym.service.PseudonymizerServiceSingleton
import org.ostelco.pseudonym.service.PseudonymizerServiceSingleton.getExportTask
import org.ostelco.pseudonym.service.PseudonymizerServiceSingleton.getPseudonymEntityFor
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.ws.rs.DELETE
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

    private val logger = LoggerFactory.getLogger(PseudonymResource::class.java)

    /**
     * Get the pseudonym which is valid at the timestamp for the given
     * msisdn. In case pseudonym doesn't exist, a new one will be created
     * for the period. Timestamps are in UTC
     */
    @GET
    @Path("/get/{msisdn}/{timestamp}")
    fun getPseudonym(@NotBlank @PathParam("msisdn") msisdn: String,
                     @NotBlank @PathParam("timestamp") timestamp: String): Response {
        logger.info("GET pseudonym for Msisdn = $msisdn at timestamp = $timestamp")
        val entity = PseudonymizerServiceSingleton.getPseudonymEntityFor(msisdn, timestamp.toLong())
        return Response.ok(entity, MediaType.APPLICATION_JSON).build()
    }

    /**
     * Get the pseudonym which is valid at the time of the call for the given
     * msisdn. In case pseudonym doesn't exist, a new one will be created
     * for the period
     */
    @GET
    @Path("/current/{msisdn}")
    fun getPseudonym(@NotBlank @PathParam("msisdn") msisdn: String): Response {
        val timestamp = Instant.now().toEpochMilli()
        logger.info("GET pseudonym for Msisdn = $msisdn at current time, timestamp = $timestamp")
        val entity = getPseudonymEntityFor(msisdn, timestamp)
        return Response.ok(entity, MediaType.APPLICATION_JSON).build()
    }

    /**
     * Get the pseudonyms valid for current & next time periods for the given
     * msisdn. In case pseudonym doesn't exist, a new one will be created
     * for the periods
     */
    @GET
    @Path("/active/{msisdn}")
    fun getActivePseudonyms(@NotBlank @PathParam("msisdn") msisdn: String): Response {
        return Response.ok(
                PseudonymizerServiceSingleton.getActivePseudonymsForMsisdn(msisdn = msisdn),
                MediaType.APPLICATION_JSON).build()
    }

    /**
     * Find the msisdn and other details about the given pseudonym.
     * In case pseudonym doesn't exist, it returns 404
     */
    @GET
    @Path("/find/{pseudonym}")
    fun findPseudonym(@NotBlank @PathParam("pseudonym") pseudonym: String): Response {
        logger.info("Find details for pseudonym = $pseudonym")
        return PseudonymizerServiceSingleton.findPseudonym(pseudonym = pseudonym)
                ?.let { Response.ok(it, MediaType.APPLICATION_JSON).build() }
                ?: Response.status(Status.NOT_FOUND).build()
    }

    /**
     * Delete all pseudonym entities for the given msisdn
     * Returns a json object with no of records deleted.
     *  { count : <no. of entities deleted> }
     */
    @DELETE
    @Path("/delete/{msisdn}")
    fun deleteAllPseudonyms(@NotBlank @PathParam("msisdn") msisdn: String): Response {
        logger.info("delete all pseudonyms for Msisdn = $msisdn")
        val count = PseudonymizerServiceSingleton.deleteAllPseudonyms(msisdn = msisdn)
        // Return a Json object with number of records deleted.
        val countMap = mapOf("count" to count)
        logger.info("deleted $count records for Msisdn = $msisdn")
        return Response.ok(countMap, MediaType.APPLICATION_JSON).build()
    }

    /**
     * Exports all pseudonyms to a bigquery table. the name is generated from the exportId
     * parameter. The exportId is expected to be a UUIDV4. The '-' are removed from the name.
     * Return Ok when the process has started, used /exportstatus to get the result.
     */
    @GET
    @Path("/export/{exportId}")
    fun exportPseudonyms(@NotBlank @PathParam("exportId") exportId: String): Response {
        logger.info("GET export all pseudonyms to the table $exportId")
        PseudonymizerServiceSingleton.exportPseudonyms(exportId = exportId)
        return Response.ok("Started Exporting", MediaType.TEXT_PLAIN).build()
    }

    /**
     * Returns the result of the export. Return 404 if exportId id not found in the system
     */
    @GET
    @Path("/exportstatus/{exportId}")
    fun getExportStatus(@NotBlank @PathParam("exportId") exportId: String): Response {
        logger.info("GET status of export $exportId")
        return getExportTask(exportId)
                ?.let { Response.ok(it, MediaType.APPLICATION_JSON).build() }
                ?: Response.status(Status.NOT_FOUND).build()
    }
}
