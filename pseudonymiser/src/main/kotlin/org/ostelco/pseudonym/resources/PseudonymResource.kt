package org.ostelco.pseudonym.resources

import com.google.cloud.datastore.Datastore
import org.hibernate.validator.constraints.NotBlank
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Path("/pseudonym")
class PseudonymResource(val datastore: Datastore) {

    private val LOG = LoggerFactory.getLogger(PseudonymResource::class.java)

    /**
     * If the msisdn can be recognized by firebase, then a custom
     * token is generated (by firebase) and returned to the caller.
     */
    @GET
    @Path("/get/{msisdn}/{timestamp}")
    fun getPseudonym(@NotBlank @PathParam("msisdn") msisdn: String?,
                     @NotBlank @PathParam("timestamp") timestamp: String?): Response {

        LOG.info("Msisdn = ${msisdn} timestamp =${timestamp}")
        return Response.ok("Msisdn = ${msisdn} timestamp =${timestamp}", MediaType.TEXT_PLAIN_TYPE).build()
    }

    /**
     * If the msisdn can be recognized by firebase, then a custom
     * token is generated (by firebase) and returned to the caller.
     */
    @GET
    @Path("/current/{msisdn}")
    fun getPseudonym(@NotBlank @PathParam("msisdn") msisdn: String): Response {

        val timestamp = Instant.now().toEpochMilli()
        LOG.info("Msisdn = ${msisdn} timestamp =${timestamp}")
        return Response.ok("Msisdn = ${msisdn} timestamp =${timestamp}", MediaType.TEXT_PLAIN_TYPE).build()
    }
}
