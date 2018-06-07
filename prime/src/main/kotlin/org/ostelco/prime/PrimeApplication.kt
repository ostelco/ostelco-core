package org.ostelco.prime

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment


fun main(args: Array<String>) {
    PrimeApplication().run(*args)
}

class PrimeApplication : Application<PrimeConfiguration>() {

    override fun initialize(bootstrap: Bootstrap<PrimeConfiguration>) {
        bootstrap.objectMapper.registerModule(KotlinModule())
    }

    override fun run(
            primeConfiguration: PrimeConfiguration,
            environment: Environment) {

        primeConfiguration.modules.forEach { it.init(environment) }
    }
}
