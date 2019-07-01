package org.ostelco.prime.slack

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("slack")
class SlackIntegrationModule : PrimeModule {

    @JsonProperty
    var config: Config? = null

    override fun init(env: Environment) {

        config?.notificationsConfig?.apply {

            val httpClient = HttpClientBuilder(env)
                    .using(this.httpClientConfiguration)
                    .build("slack")

            Registry.slackWebHookClient = SlackWebHookClient(
                    webHookUri = this.webHookUri,
                    httpClient = httpClient)

            Registry.channel = this.channel
            Registry.userName = this.userName
            Registry.environment = this.environment
            Registry.deployment = this.deployment
            Registry.isInitialized = true
        }
    }
}

object Registry {
    var isInitialized = false
    lateinit var slackWebHookClient: SlackWebHookClient
    lateinit var channel: String
    lateinit var userName: String
    lateinit var environment: String
    lateinit var deployment: String
}

data class Config(
    @JsonProperty("notifications")
    val notificationsConfig: NotificationsConfig)

class NotificationsConfig {

    @JsonProperty
    lateinit var webHookUri: String

    @JsonProperty("httpClient")
    var httpClientConfiguration = HttpClientConfiguration()

    @JsonProperty
    var channel: String = "general"

    @JsonProperty
    var userName: String = "prime"

    @JsonProperty
    var environment: String = "Production"

    @JsonProperty
    var deployment: String = "prod"
}