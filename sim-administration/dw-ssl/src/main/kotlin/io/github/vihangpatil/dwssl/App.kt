package io.github.vihangpatil.dwssl

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment

fun main() = DwSslApp().run("server", "config/config.yaml")

class DwSslApp : Application<Configuration>() {

    override fun run(
            config: Configuration,
            env: Environment) {

        env.jersey().register(PingResource())
    }
}