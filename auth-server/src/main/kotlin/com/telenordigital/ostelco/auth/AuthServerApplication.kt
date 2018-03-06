package com.telenordigital.ostelco.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.telenordigital.ostelco.auth.config.AuthServerConfig
import com.telenordigital.ostelco.auth.resources.AuthResource
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory
import java.io.FileInputStream

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

    private val LOG = LoggerFactory.getLogger(AuthServerApplication::class.java)

    /**
     * Run the dropwizard application (called by the kotlin [main] wrapper).
     */
    override fun run(
            config: AuthServerConfig,
            env: Environment) {

        val serviceAccount = FileInputStream(config.serviceAccountKey)

        val options = FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://${config.databaseName}.firebaseio.com/")
                .build()

        FirebaseApp.initializeApp(options)

        env.jersey().register(AuthResource())
    }
}