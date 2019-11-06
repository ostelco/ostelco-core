package org.ostelco.ext.myinfo

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.glassfish.jersey.logging.LoggingFeature
import org.glassfish.jersey.logging.LoggingFeature.Verbosity.PAYLOAD_ANY
import java.util.logging.Logger

fun main(args: Array<String>) = MyInfoEmulatorApp().run(*args)

class MyInfoEmulatorApp : Application<MyInfoEmulatorConfig>() {

    override fun initialize(bootstrap: Bootstrap<MyInfoEmulatorConfig>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor(false))
        bootstrap.objectMapper.registerModule(KotlinModule())
    }

    override fun run(
            config: MyInfoEmulatorConfig,
            env: Environment) {


        env.jersey().register(LoggingFeature(
                Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME),
                PAYLOAD_ANY))

        env.jersey().register(org.ostelco.ext.myinfo.v3.TokenResource(config))
        env.jersey().register(org.ostelco.ext.myinfo.v3.PersonResource(config))
    }
}

data class MyInfoEmulatorConfig(
        val myInfoApiClientId: String,
        val myInfoApiClientSecret: String,
        val myInfoRedirectUri: String,
        val myInfoServerPublicKey: String,
        val myInfoServerPrivateKey: String,
        val myInfoClientPublicKey: String) : Configuration()