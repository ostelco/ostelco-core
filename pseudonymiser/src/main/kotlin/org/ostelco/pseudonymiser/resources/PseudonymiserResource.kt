package org.ostelco.pseudonymiser.resources

import org.hibernate.validator.constraints.NotBlank
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import kotlin.collections.HashMap

/**
 * Resource used to handle the pseudonymiser REST calls.
 */
@Path("/pseudonymiser")
class PseudonymiserResource() {

    private val LOG = LoggerFactory.getLogger(PseudonymiserResource::class.java)
    /**
     * Get the status of pseudonymiser
     */
    @GET
    @Path("/status")
    fun getStatus(): Response {
        LOG.info("Status of pseudonymiser")
        return Response.ok("EVERYTHING OK", MediaType.TEXT_PLAIN).build()
    }
}
