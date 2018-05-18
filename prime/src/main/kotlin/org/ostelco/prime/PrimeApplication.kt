package org.ostelco.prime

import io.dropwizard.Application
import io.dropwizard.setup.Environment

fun main(args: Array<String>) {
    PrimeApplication().run(*args)
}

class PrimeApplication : Application<PrimeConfiguration>() {

    override fun run(
            primeConfiguration: PrimeConfiguration,
            environment: Environment) {

        primeConfiguration.modules.forEach { it.init(environment) }
    }
}
