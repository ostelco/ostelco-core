package org.ostelco.bqmetrics

import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.ostelco.bqmetrics.backend.DatabaseBackend

/**
 * Main entry point to the bq-metrics-extractor API server.
 */
class BqMetricsExtractorApplication : Application<BqMetricsExtractorConfiguration>() {

    override fun initialize(bootstrap: Bootstrap<BqMetricsExtractorConfiguration>?) {

    }

    override fun run(configuration: BqMetricsExtractorConfiguration, environment: Environment) {
        //DatabaseBackend databaseBackend = configuration.getDatabaseBackend(environment);
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            BqMetricsExtractorApplication().run(*args)
        }
    }
}
