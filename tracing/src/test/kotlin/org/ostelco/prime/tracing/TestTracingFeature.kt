package org.ostelco.prime.tracing

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import java.lang.Thread.sleep
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class TestTracingFeature {

    @Test
    fun `test - tracing http resources`() {

        TraceSingleton.init()

        resourceTestRule.target("/test/trace")
                .request()
                .get()

        resourceTestRule.target("/test/no_trace")
                .request()
                .get()
    }

    companion object {

        @ClassRule
        @JvmField
        val resourceTestRule: ResourceTestRule = ResourceTestRule.builder()
                .addResource(TestResource())
                .addProvider(TracingFeature::class.java)
                .build()
    }

}

@Path("/test")
class TestResource {

    @EnableTracing
    @GET
    @Path("/trace")
    @Produces(MediaType.APPLICATION_JSON)
    fun sampleMethodWithTracing(): Response {

        TraceSingleton.childSpan("childSpan") {
            sleep(200)
        }
        return Response.ok().build()
    }

    @GET
    @Path("/no_trace")
    @Produces(MediaType.APPLICATION_JSON)
    fun sampleMethodWithoutTracing(): Response = Response.ok().build()
}