package org.ostelco.prime.notifications.email

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.notifications.email.ConfigRegistry.config
import org.ostelco.prime.notifications.email.Registry.httpClient

@JsonTypeName("email")
class EmailNotifierModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: Config) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        httpClient = HttpClientBuilder(env)
                .using(config.httpClientConfiguration)
                .build("mandrill")
    }
}

class Config {
    lateinit var mandrillApiKey: String

    @JsonProperty("httpClient")
    var httpClientConfiguration = HttpClientConfiguration()
}

object ConfigRegistry {
    lateinit var config: Config
}

object Registry {
    lateinit var httpClient: HttpClient
}