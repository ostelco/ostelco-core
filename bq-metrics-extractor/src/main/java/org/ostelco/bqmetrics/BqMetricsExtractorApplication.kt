package org.ostelco.bqmetrics

import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.prometheus.client.exporter.PushGateway
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge.*
import io.dropwizard.cli.Command
import com.google.cloud.bigquery.DatasetInfo
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Dataset
import io.dropwizard.Configuration
import io.prometheus.client.Gauge
import net.sourceforge.argparse4j.inf.Namespace
import net.sourceforge.argparse4j.inf.Subparser


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


interface MetricBuilder {
    fun buildMetric(Registry: CollectorRegistry)
}

// XXX Make a query class that when invoked will return a metric that is pushed.
//     Start by refactoring the code we already have into that shape, then
//     add an actual query and we're essentially done.   Adding abstractions for
//     fun and beauty can then be done.

object BigquerySample : MetricBuilder {
    @Throws(Exception::class)
    @JvmStatic
    fun countSubscribers(args: Array<String>) {
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

    // XXX Next step is to get this metric via the bigquery
    override fun buildMetric(registry: CollectorRegistry) {
        val duration: Gauge = build()
                .name("my_batch_job_duration_seconds")
                .help("Duration of my batch job in seconds.").register(registry)
        val durationTimer = duration.startTimer()
        try {
            val lastSuccess = build()
                    .name("my_batch_job_last_success")
                    .help("Last time my batch job succeeded, in unixtime.").register(registry)
            lastSuccess.setToCurrentTime()
        } finally {
            durationTimer.setDuration()
        }
    }
}

/**
 * Adapter class that will push metrics to the Prometheus push gateway.
 */
class PrometheusPusher {

    val registry = CollectorRegistry()


    // Example code for  pushing to pushgateway (from https://github.com/prometheus/client_java#exporting-to-a-pushgateway,
    // translated to Kotlin)
    @Throws(Exception::class)
    fun publishMetrics() {

        val metricSources:MutableList<MetricBuilder> = mutableListOf()
        metricSources.add(BigquerySample)

        val pg = PushGateway("127.0.0.1:9091")
        metricSources.forEach({ it.buildMetric(registry) })

        pg.pushAdd(registry, "my_batch_job")

    }
}

class CollectAndPushMetrics : Command("query", "query BigQuery for a metric") {
    override fun configure(subparser: Subparser?) {
    }

    override fun run(bootstrap: Bootstrap<*>?, namespace: Namespace?) {
        println("Running query")
        PrometheusPusher().publishMetrics()
    }
}