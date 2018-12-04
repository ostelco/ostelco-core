package org.ostelco.prime.admin.api

import org.ostelco.prime.getLogger
import javax.servlet.http.HttpServletRequest
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
    fun handleCallback(
            @Context request: HttpServletRequest,
            @Context httpHeaders: HttpHeaders,
            data: String): Response {
        return Response.status(Response.Status.OK).entity(printHeaderInfo(request, httpHeaders, data)).build();
    }
    private fun printHeaderInfo(request: HttpServletRequest, httpHeaders: HttpHeaders, data: String):String {
        var result = ""
        result += "Address: ${request.remoteHost} (${request.remoteAddr} : ${request.remotePort}) \n"
        result += "Query: ${request.queryString} \n"
        result += "Data: ${data} \n"
        result += "Headers == > \n"
        val requestHeaders = httpHeaders.getRequestHeaders()
        for (entry in requestHeaders.entries) {
            result += "${entry.key} = ${entry.value}\n"
            logger.info("${entry.key} = ${entry.value}")
        }
        return result
    }
}
