package org.ostelco.pseudonym.resources

import com.google.cloud.datastore.Datastore
import com.google.firebase.auth.FirebaseAuth
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

/**
 * Resource used to authentiation using an X-MSISDN (injected header)
 * authentication.  If there is an user registered for that MSISDN in
 * the firebase databae, then a custom firebase token is generated and
 * delivered as plain text, with a 200 (OK) return code.
 */
@Path("/pseudonym")
class PseudonymResource(val datastore: Datastore) {

    private val LOG = LoggerFactory.getLogger(PseudonymResource::class.java)

    /**
     * If the msisdn can be recognized by firebase, then a custom
     * token is generated (by firebase) and returned to the caller.
     */
    @GET
    @Path("/get/{msisdn}/{timestamp}")
    fun getPseudonym(@PathParam("msisdn") msisdn: String?,
                     @PathParam("timestamp") timestamp: String?): Response {

        if (msisdn == null || timestamp == null) {
            throw WebApplicationException(Status.NOT_FOUND)
        }
        LOG.info("Msisdn = ${msisdn} timestamp =${timestamp}")
        return Response.ok("Msisdn = ${msisdn} timestamp =${timestamp}", MediaType.TEXT_PLAIN_TYPE).build()
    }

    /**
     * If the msisdn can be recognized by firebase, then a custom
     * token is generated (by firebase) and returned to the caller.
     */
    @GET
    @Path("/current/{msisdn}")
    fun getPseudonym(@PathParam("msisdn") msisdn: String?): Response {

        if (msisdn == null) {
            throw WebApplicationException(Status.NOT_FOUND)
        }
        val timestamp = Instant.now().toEpochMilli()
        LOG.info("Msisdn = ${msisdn} timestamp =${timestamp}")
        return Response.ok("Msisdn = ${msisdn} timestamp =${timestamp}", MediaType.TEXT_PLAIN_TYPE).build()
    }

    /**
     * If the msisdn can be recognized by firebase, then a custom
     * token is generated (by firebase) and returned to the caller.
     */
    @GET
    @Path("/token")
    fun getAuthToken(@HeaderParam("X-MSISDN") msisdn: String?): Response {

        if (msisdn == null) {
            throw WebApplicationException(Status.INTERNAL_SERVER_ERROR)
        }

        // Construct parameters for the firebase API.
        val additionalClaims = HashMap<String, Any>()
        additionalClaims["msisdn"] = msisdn

        // Interrogate Firebase.
        val customToken = FirebaseAuth
                .getInstance()
                .createCustomTokenAsync(
                        getUid(msisdn),
                        additionalClaims)
                .get()

        // TODO: Missing explicit handling of error-situation where
        //       firebase response gives error, times out or
        //       something else that shouldn't be interpreted as success.
        return Response.ok(customToken, MediaType.TEXT_PLAIN_TYPE).build()
    }

    /**
     * As of now, `msisdn` is considered as `user-id`.
     * This is subjected to change in future.
     */
    private fun getUid(msisdn: String): String {
        return msisdn;
    }
}
