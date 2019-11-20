package org.ostelco.prime.auth.resources

import org.ostelco.prime.auth.apple.AppleIdAuthClient
import org.ostelco.prime.auth.firebase.FirebaseAuthUtil
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.jwt.JwtParser
import org.ostelco.prime.tracing.EnableTracing
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_GATEWAY

@Path("/appleId")
class AppleIdAuthResource {

    private val logger by getLogger()

    @EnableTracing
    @POST
    @Path("/authorize")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun getAuthToken(authCode: AuthCode): Response {

        return AppleIdAuthClient.authorize(authCode.authCode)
                .fold(
                        {
                            logger.warn("AppleId Auth Error Response: {}, cause: {}", it.error, it.error.error.cause)
                            Response.status(it.status).entity(asJson(it))
                        },
                        { tokenResponse ->

                            val subject = JwtParser.getClaims(tokenResponse.id_token)?.get("sub")?.textValue()
                            if (subject == null) {
                                logger.warn("subject is missing in id_token claims for Apple Id Auth")
                                Response.status(BAD_GATEWAY)
                            } else {
                                Response.ok(LoginToken(FirebaseAuthUtil.createCustomToken(subject)))
                            }
                        }
                ).build()
    }
}

/**
 * Request body for /appleId/authorize
 */
data class AuthCode(val authCode: String)

/**
 * /appleId/authorize
 */
data class LoginToken(val token: String)