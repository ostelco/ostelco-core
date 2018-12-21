package org.ostelco.sim.dwssl
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

fun main() = DwSslApp().run("server", "config/config.yaml")

class DwSslApp : Application<Configuration>() {

    override fun run(
            config: Configuration,
            env: Environment) {

        env.jersey().register(PingResource())
    }
}

@Path("/ping")
class PingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun ping(): String = "pong"
}