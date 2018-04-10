package org.ostelco.exporter.resources

import org.slf4j.LoggerFactory
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response


/**
 * Resource used to handle the exporter related REST calls.
 */
@Path("/exporter")
class ExporterResource() {

    private val LOG = LoggerFactory.getLogger(ExporterResource::class.java)

    /**
     * Get the status
     */
    @GET
    @Path("/get/status")
    fun getStatus(): Response {
        LOG.info("GET status for exporter")
        return Response.ok().build()
    }
}
