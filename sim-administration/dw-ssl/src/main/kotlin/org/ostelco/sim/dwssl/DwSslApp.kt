package org.ostelco.sim.dwssl
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import org.apache.http.client.HttpClient
import javax.validation.Valid
import javax.validation.constraints.NotNull


fun main() = DwSslApp().run("server", "config/config.yaml")

class DwSslApp : Application<DweSslAppConfig>() {

    lateinit var client: HttpClient

    override fun run(
            config: DweSslAppConfig,
            env: Environment) {

        env.jersey().register(PingResource())


       this.client = HttpClientBuilder(env).using(config.getJerseyClientConfiguration()).build(getName())
    }
}


class DweSslAppConfig: Configuration() {
    @Valid
    @NotNull
     var httpClientConfiguration = HttpClientConfiguration()

    @JsonProperty("httpClient")
    fun getJerseyClientConfiguration(): HttpClientConfiguration {
        return httpClientConfiguration
    }

    @JsonProperty("httpClient")
    fun setJerseyClientConfiguration(config: HttpClientConfiguration) {
        this.httpClientConfiguration = config
    }
}

@Path("/ping")
class PingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun ping(): String = "pong"
}