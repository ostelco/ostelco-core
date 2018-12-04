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
        printHeaderInfo(httpHeaders);
        return Response.status(Response.Status.OK).build();
    }
    private fun printHeaderInfo(httpHeaders: HttpHeaders) {
        val requestHeaders = httpHeaders.getRequestHeaders()
        for (entry in requestHeaders.entries) {
            logger.info("${entry.key} = ${entry.value}")
        }
    }
}
