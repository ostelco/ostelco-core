package org.ostelco.importer.resources

import org.slf4j.LoggerFactory
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response


/**
 * Resource used to handle the importer related REST calls.
 */
@Path("/importer")
class ImporterResource() {

    private val LOG = LoggerFactory.getLogger(ImporterResource::class.java)

    /**
     * Get the status
     */
    @GET
    @Path("/get/status")
    fun getStatus(): Response {
        LOG.info("GET status for importer")
        return Response.ok().build()
    }
}
