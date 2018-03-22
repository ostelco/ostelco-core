package org.ostelco.pseudonym.resources

import com.google.cloud.Tuple
import com.google.cloud.datastore.Datastore
import org.hibernate.validator.constraints.NotBlank
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.util.TimeZone


interface DateBounds {
    fun getBounds(timestamp: Long): Pair<Long, Long>
}

@Path("/pseudonym")
class PseudonymResource(val datastore: Datastore, val dateBounds: DateBounds) {

    private val LOG = LoggerFactory.getLogger(PseudonymResource::class.java)
    private val timeZone = TimeZone.getTimeZone("UTC")
    /**
     * Get the pseudonym which is valid at the timestamp for the given
     * msisdn. In case pseudonym doesn't exist, a new one will be created
     * for the period. Timestamps are in UTC
     */
    @GET
    @Path("/get/{msisdn}/{timestamp}")
    fun getPseudonym(@NotBlank @PathParam("msisdn") msisdn: String?,
                     @NotBlank @PathParam("timestamp") timestamp: String?): Response {

        LOG.info("Msisdn = ${msisdn} timestamp =${timestamp}")
        val bounds = dateBounds.getBounds(timestamp!!.toLong())
        LOG.info("timestamp = ${timestamp} Bounds (${bounds.first} - ${bounds.second})")
        return Response.ok("Msisdn = ${msisdn} timestamp =${timestamp}", MediaType.TEXT_PLAIN_TYPE).build()
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
        LOG.info("Msisdn = ${msisdn} timestamp =${timestamp}")
        val bounds = dateBounds.getBounds(timestamp)
        LOG.info("timestamp = ${timestamp} Bounds (${bounds.first} - ${bounds.second})")
        return Response.ok("Msisdn = ${msisdn} timestamp =${timestamp}", MediaType.TEXT_PLAIN_TYPE).build()
    }
}
