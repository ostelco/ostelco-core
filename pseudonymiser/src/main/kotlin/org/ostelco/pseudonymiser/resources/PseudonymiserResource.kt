package org.ostelco.pseudonymiser.resources

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Key
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.StructuredQuery.PropertyFilter
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
 * Class representing the Pseudonym entity in Datastore.
 */
data class PseudonymEntity(val msisdn: String, val pseudonym: String, val start: Long, val end: Long)

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
