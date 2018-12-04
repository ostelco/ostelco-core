package org.ostelco.prime.admin.api

import org.ostelco.prime.getLogger
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Resource used to handle the eKYC related REST calls.
 */
@Path("/ekyc/callback")
class KYCResource {
    private val logger by getLogger()

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun handleCallback(@Context
                       httpHeaders: HttpHeaders): Response {
        return Response.status(Response.Status.OK).entity(printHeaderInfo(httpHeaders)).build();
    }
    private fun printHeaderInfo(httpHeaders: HttpHeaders):String {
        var result = ""
        val requestHeaders = httpHeaders.getRequestHeaders()
        for (entry in requestHeaders.entries) {
            result += "${entry.key} = ${entry.value}\n"
            logger.info("${entry.key} = ${entry.value}")
        }
        return result
    }
}
