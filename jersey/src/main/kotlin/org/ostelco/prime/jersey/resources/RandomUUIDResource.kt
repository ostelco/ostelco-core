package org.ostelco.prime.jersey.resources

import org.ostelco.prime.jsonmapper.asJson
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/uuid")
class RandomUUIDResource() {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getRandomUUID(): Response = Response
            .ok(asJson(mapOf("uuid" to UUID.randomUUID().toString())))
            .build()

}
