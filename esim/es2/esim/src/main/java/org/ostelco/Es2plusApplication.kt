package org.ostelco

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

class Es2plusApplication : Application<Es2plusConfiguration>() {

    override fun getName(): String {
        return "es2+ application"
    }

    override fun initialize(bootstrap: Bootstrap<Es2plusConfiguration>) {
        // TODO: application initialization
    }

    override fun run(configuration: Es2plusConfiguration,
            environment: Environment) {
        // TODO: implement application
        environment.jersey().register(Es2PlusResource())
    }

    companion object {

        @Throws(Exception::class)
        fun main(args: Array<String>) {
            Es2plusApplication().run("foo")
        }
    }

    // We're basing this implementaiton on
    // https://www.gsma.com/newsroom/wp-content/uploads/SGP.22-v2.0.pdf

}

data class Blah(
        @JsonProperty("fooz") val fooz: String
)


@Path("/foo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class Es2PlusResource() {


    @GET
    fun get(): Response {
        return Response.noContent().build() // XXXX Just to gt there
    }

    @POST
    fun add(): Response {
        return Response.created(UriBuilder.fromPath("http://bananas.org/").build()).build()
    }
}
