package org.ostelco.bqmetrics


import com.fasterxml.jackson.annotation.JsonProperty
import com.google.cloud.bigquery.*
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.prometheus.client.exporter.PushGateway
import io.prometheus.client.CollectorRegistry
import io.dropwizard.cli.Command
import io.dropwizard.Configuration
import io.prometheus.client.Summary
import net.sourceforge.argparse4j.inf.Namespace
import net.sourceforge.argparse4j.inf.Subparser
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory
import java.util.*
import javax.validation.Valid
import net.sourceforge.argparse4j.impl.Arguments.help
import org.slf4j.Logger
import java.io.File


fun main(args: Array<String>) {
    BqMetricsExtractorApplication().run(*args)
}

/**
 * Main entry point to the bq-metrics-extractor API server.
 */
class BqMetricsExtractorApplication : Application<Configuration>() {

    override fun initialize(bootstrap: Bootstrap<Configuration>) {
        bootstrap.addCommand(CollectAndPushMetrics())
        bootstrapLogging()
    }

    override fun run(
            configuration: Configuration,
            environment: Environment) {
    }
}


interface MetricBuilder {
    fun buildMetric(registry: CollectorRegistry)
}


class  BigquerySample(val metricName: String, val help: String, val sql: String, val resultColumn: String) : MetricBuilder {

    private val log:Logger = LoggerFactory.getLogger(BigquerySample::class.java)

    fun  countNumberOfActiveUsers(): Long {
        // Instantiate a client. If you don't specify credentials when constructing a client, the
        // client library will look for credentials in the environment, such as the
        // GOOGLE_APPLICATION_CREDENTIALS environment variable.
        val bigquery = BigQueryOptions.getDefaultInstance().service
        val queryConfig: QueryJobConfiguration =
        QueryJobConfiguration.newBuilder(
                sql.trimIndent())
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
        if (result.totalRows != 1L) {
            throw RuntimeException("Number of results was ${result.totalRows} which is different from the expected single row")
        }

        val count = result.iterateAll().iterator().next().get(resultColumn).longValue

        return count
    }


    override fun buildMetric(registry: CollectorRegistry) {
        val activeUsersSummary: Summary = Summary.build()
                .name(metricName)
                .help(help).register(registry)

        activeUsersSummary.observe(countNumberOfActiveUsers() * 1.0)
    }
}

/**
 * Adapter class that will push metrics to the Prometheus push gateway.
 */
class PrometheusPusher (val pushGateway: String, val job: String){

    private val log:Logger = LoggerFactory.getLogger(PrometheusPusher::class.java)

    val registry = CollectorRegistry()


    // Example code for  pushing to pushgateway (from https://github.com/prometheus/client_java#exporting-to-a-pushgateway,
    // translated to Kotlin)
    @Throws(Exception::class)
    fun publishMetrics() {


        // XXX Pick this up from the config file, and iterate over all the queries
        //     your heart desires.
        val metricSources:MutableList<MetricBuilder> = mutableListOf()
        metricSources.add(BigquerySample(
                "active_users",
                "Number of active users",
                """
                    SELECT count(distinct user_pseudo_id) AS count FROM `pantel-2decb.analytics_160712959.events_*`
                    WHERE event_name = "first_open"
                    LIMIT 1000""",
                "count"))

        log.info("Querying bigquery for metric values")
        val pg = PushGateway(pushGateway)
        metricSources.forEach({ it.buildMetric(registry) })

        log.info("Pushing metrics to pushgateway")
        pg.pushAdd(registry, job)
        log.info("Done transmitting metrics to pushgateway")
    }
}

class CollectAndPushMetrics : Command("query", "query BigQuery for a metric") {

    val pushgatewayKey = "pushgateway"

    override fun configure(subparser: Subparser?) {
        subparser!!.addArgument("-p", "--pushgateway")
                .dest(pushgatewayKey)
                .type(String::class.java)
                .required(true)
                .help("The pushgateway to report metrics to, format is hostname:portnumber")
    }

    override fun run(bootstrap: Bootstrap<*>?, namespace: Namespace?) {
        val pgw = namespace!!.get<String>(pushgatewayKey)
        PrometheusPusher(pgw,
                "bq_metrics_extractor").publishMetrics()
    }
}