package com.telenordigital.ostelco.auth.resources

import com.google.firebase.auth.FirebaseAuth
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status


@Path("/auth")
class AuthResource {

    private val LOG = LoggerFactory.getLogger(AuthResource::class.java)

    @GET
    @Path("/token")
    fun getAuthToken(@HeaderParam("X-MSISDN") msisdn: String?): Response {

        if(msisdn == null) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build()
        }

        val additionalClaims = HashMap<String, Any>()
        additionalClaims["msisdn"] = msisdn

        val customToken = FirebaseAuth
                .getInstance()
                .createCustomTokenAsync(
                        getUid(msisdn),
                        additionalClaims)
                .get()
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
