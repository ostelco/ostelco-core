package org.ostelco.bqmetrics

import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.prometheus.client.exporter.PushGateway
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge.*
import net.sourceforge.argparse4j.inf.Subparser
import io.dropwizard.cli.Command
import com.google.cloud.bigquery.DatasetInfo
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Dataset
import io.dropwizard.Configuration
import net.sourceforge.argparse4j.inf.Namespace


/**
 * Main entry point to the bq-metrics-extractor API server.
 */
class BqMetricsExtractorApplication : Application<Configuration>() {

    override fun initialize(bootstrap: Bootstrap<Configuration>?) {
        bootstrap!!.addCommand(CollectAndPushMetrics())
    }

    override fun run(configuration: Configuration, environment: Environment) {
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


object BigquerySample {
    @Throws(Exception::class)
    @JvmStatic
    fun foo(args: Array<String>) {
        // Instantiate a client. If you don't specify credentials when constructing a client, the
        // client library will look for credentials in the environment, such as the
        // GOOGLE_APPLICATION_CREDENTIALS environment variable.
        val bigquery = BigQueryOptions.getDefaultInstance().service

        // The name for the new dataset
        val datasetName = "my_new_dataset"

        // Prepares a new dataset
        var dataset: Dataset? = null
        val datasetInfo = DatasetInfo.newBuilder(datasetName).build()

        // Creates the dataset
        dataset = bigquery.create(datasetInfo)

        System.out.printf("Dataset %s created.%n", dataset!!.getDatasetId().getDataset())
    }
}

class Pusher {
    // Example code for  pushing to pushgateway (from https://github.com/prometheus/client_java#exporting-to-a-pushgateway,
    // translated to Kotlin)
    @Throws(Exception::class)
    fun executeBatchJob() {
        val registry = CollectorRegistry()
        val duration = build()
                .name("my_batch_job_duration_seconds").help("Duration of my batch job in seconds.").register(registry)
        val durationTimer = duration.startTimer()
        try {
            // Your code here.

            // This is only added to the registry after success,
            // so that a previous success in the Pushgateway isn't overwritten on failure.
            val lastSuccess = build()
                    .name("my_batch_job_last_success").help("Last time my batch job succeeded, in unixtime.").register(registry)
            lastSuccess.setToCurrentTime()
        } finally {
            durationTimer.setDuration()
            val pg = PushGateway("127.0.0.1:9091")
            pg.pushAdd(registry, "my_batch_job")
        }
    }
}

class CollectAndPushMetrics : Command("hello", "Prints a greeting") {

    override fun run(bootstrap: Bootstrap<*>?, namespace: Namespace?) {
        val user: String? =  namespace!!.getString("user")
        println("Hello $user")
    }

    override fun configure(subparser: Subparser) {
        // Add a command line option
        subparser.addArgument("-u", "--user")
                .dest("user")
                .type(String::class.java)
                .required(true)
                .help("The user of the program")
    }
}