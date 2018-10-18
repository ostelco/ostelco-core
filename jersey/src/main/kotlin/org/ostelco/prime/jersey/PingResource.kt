package org.ostelco.prime.jersey

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Path("/ping")
class PingResource {

    @GET
    fun ping(): Response = Response.ok().build()
}