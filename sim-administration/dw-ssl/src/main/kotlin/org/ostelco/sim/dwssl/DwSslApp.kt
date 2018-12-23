package org.ostelco.sim.dwssl
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.client.JerseyClientConfiguration
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.client.Client


fun main() = DwSslApp().run("server", "config/config.yaml")

class DwSslApp : Application<DweSslAppConfig>() {

    lateinit var client: Client

    override fun run(
            config: DweSslAppConfig,
            env: Environment) {

        env.jersey().register(PingResource())


       this.client = JerseyClientBuilder(env).using(config.getJerseyClientConfiguration()).build(getName())
    }
}


class DweSslAppConfig: Configuration() {
    @Valid
    @NotNull
     var jerseyClient = JerseyClientConfiguration()

    @JsonProperty("jerseyClient")
    fun getJerseyClientConfiguration(): JerseyClientConfiguration {
        return jerseyClient
    }

    @JsonProperty("jerseyClient")
    fun setJerseyClientConfiguration(jerseyClient: JerseyClientConfiguration) {
        this.jerseyClient = jerseyClient
    }
}

@Path("/ping")
class PingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun ping(): String = "pong"
}