package org.ostelco.prime.admin.api

import org.ostelco.prime.getLogger
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

/**
 * Resource used to handle the eKYC related REST calls.
 */
@Path("/ekyc")
class KYCResource {
    private val logger by getLogger()

    @Context
    private val httpHeaders: HttpHeaders? = null

    @GET
    @Path("callback")
    @Produces(MediaType.APPLICATION_JSON)
    fun handleCallback() {
        printHeaderInfo();
    }
    private fun printHeaderInfo() {
        println("---------------")
        val requestHeaders = httpHeaders!!.getRequestHeaders()
        for (entry in requestHeaders.entries) {
            logger.info("${entry.key} = ${entry.value}")
        }
    }
}
