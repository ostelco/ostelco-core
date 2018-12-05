package org.ostelco.prime.admin.api

import org.ostelco.prime.getLogger
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.*

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
            formData: MultivaluedMap<String, String>): Response {
        return Response.status(Response.Status.OK).entity(getRequestInfo(request, httpHeaders, formData)).build();
    }
    private fun getRequestInfo(request: HttpServletRequest, httpHeaders: HttpHeaders, formData: MultivaluedMap<String, String>):String {
        var result = ""
        result += "Address: ${request.remoteHost} (${request.remoteAddr} : ${request.remotePort}) \n"
        result += "Query: ${request.queryString} \n"
        result += "Headers == > \n"
        val requestHeaders = httpHeaders.getRequestHeaders()
        for (entry in requestHeaders.entries) {
            result += "${entry.key} = ${entry.value}\n"
        }
        result += "Data: \n"
        for (entry in formData.entries) {
            result += "${entry.key} = ${entry.value}\n"
        }
        logger.info("$result")

        return result
    }
}
