package org.ostelco.auth

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.ostelco.auth.resources.AuthResource
import org.ostelco.common.firebasex.usingCredentialsFile
import org.slf4j.LoggerFactory

/**
 * Entry point for running the authentiation server application
 */
fun main(args: Array<String>) {
    AuthServerApplication().run(*args)
}

/**
 * A Dropwizard application for running an authentication service that
 * uses Firebase to authenticate users.
 */
class AuthServerApplication : Application<AuthServerConfig>() {

    private val logger = LoggerFactory.getLogger(AuthServerApplication::class.java)

    override fun getName(): String = "AuthServer"

    override fun initialize(bootstrap: Bootstrap<AuthServerConfig>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor())
        bootstrap.objectMapper.registerModule(KotlinModule())
    }

    /**
     * Run the dropwizard application (called by the kotlin [main] wrapper).
     */
    override fun run(
            config: AuthServerConfig,
            env: Environment) {

        val options = FirebaseOptions.Builder()
                .usingCredentialsFile(config.serviceAccountKey)
                .build()

        FirebaseApp.initializeApp(options)

        env.jersey().register(AuthResource())
    }
}

data class AuthServerConfig(val serviceAccountKey: String) : Configuration()