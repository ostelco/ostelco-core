package org.ostelco.bqmetrics

import com.google.cloud.bigquery.*
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.prometheus.client.exporter.PushGateway
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge.*
import io.dropwizard.cli.Command
import io.dropwizard.Configuration
import io.prometheus.client.Gauge
import net.sourceforge.argparse4j.inf.Namespace
import net.sourceforge.argparse4j.inf.Subparser
import java.util.*




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


class  BigquerySample : MetricBuilder {

    fun  countNumberOfActiveUsers(): Long {
        // Instantiate a client. If you don't specify credentials when constructing a client, the
        // client library will look for credentials in the environment, such as the
        // GOOGLE_APPLICATION_CREDENTIALS environment variable.
        val bigquery = BigQueryOptions.getDefaultInstance().service
        val queryConfig: QueryJobConfiguration =
        QueryJobConfiguration.newBuilder(
                """
                    SELECT count(distinct user_pseudo_id) AS count FROM `pantel-2decb.analytics_160712959.events_*`
                    WHERE event_name = "first_open"
                    LIMIT 1000""".trimIndent())
                // Use standard SQL syntax for queries.
                // See: https://cloud.google.com/bigquery/sql-reference/
                .setUseLegacySql(false)
                .build();

        // Create a job ID so that we can safely retry.
        val  jobId: JobId = JobId . of (UUID.randomUUID().toString());
        var  queryJob: Job = bigquery . create (JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw  RuntimeException ("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw RuntimeException (queryJob.getStatus().getError().toString());
        }
        val result = queryJob.getQueryResults()
        System.out.println("Total # of rows = ${result.totalRows}")
        if (result.totalRows != 1L) {
            throw RuntimeException("Number of results was ${result.totalRows} which is different from the expected single row")
        }

        val count = result.iterateAll().iterator().next().get("count").longValue

        return count
    }


    override fun buildMetric(registry: CollectorRegistry) {
        val myGauge: Gauge = build()
                .name("foo_active_users")
                .help("Number of active users").register(registry)

        // XXX How about updates?
        myGauge.set(countNumberOfActiveUsers() * 1.0)
    }
}

/**
 * Adapter class that will push metrics to the Prometheus push gateway.
 */
class PrometheusPusher (val job: String){

    val registry = CollectorRegistry()


    // Example code for  pushing to pushgateway (from https://github.com/prometheus/client_java#exporting-to-a-pushgateway,
    // translated to Kotlin)
    @Throws(Exception::class)
    fun publishMetrics() {

        val metricSources:MutableList<MetricBuilder> = mutableListOf()
        metricSources.add(BigquerySample())

        val pg = PushGateway("127.0.0.1:9091")
        metricSources.forEach({ it.buildMetric(registry) })

        pg.pushAdd(registry, job)
    }
}

class CollectAndPushMetrics : Command("query", "query BigQuery for a metric") {
    override fun configure(subparser: Subparser?) {
    }

    override fun run(bootstrap: Bootstrap<*>?, namespace: Namespace?) {
        println("Running query")
        PrometheusPusher("bq_metrics_extractor").publishMetrics()
    }
}