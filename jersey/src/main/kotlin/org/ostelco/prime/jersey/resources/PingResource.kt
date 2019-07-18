package org.ostelco.prime.jersey.resources

import java.time.Instant
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/ping")
class PingResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun ping(): Response = Response
            .ok(mapOf("timestamp" to Instant.now().toEpochMilli()))
            .build()
}