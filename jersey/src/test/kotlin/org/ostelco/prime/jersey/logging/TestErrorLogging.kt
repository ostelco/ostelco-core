package org.ostelco.prime.jersey.logging

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST

class TestErrorLogging {

    @Test
    fun `test - error logging non-200 responses`() {

        resourceTestRule.target("/test/success")
                .request()
                .get()

        resourceTestRule.target("/test/error")
                .request()
                .get()
    }

    companion object {

        @ClassRule
        @JvmField
        val resourceTestRule: ResourceTestRule = ResourceTestRule.builder()
                .addResource(TestResource())
                .addProvider(ErrorLoggingFeature::class.java)
                .build()
    }
}

@Path("/test")
class TestResource {

    @Critical
    @GET
    @Path("/success")
    @Produces(MediaType.APPLICATION_JSON)
    fun sampleImportantEndpointMethodForSuccess(): Response = Response
            .ok("""{"message":"OK"}""")
            .build()

    @Critical
    @GET
    @Path("/error")
    @Produces(MediaType.APPLICATION_JSON)
    fun sampleImportantEndpointMethodForError(): Response = Response
            .status(BAD_REQUEST)
            .entity("""{"error":"Bad Request"}""")
            .build()
}