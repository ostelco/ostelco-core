package org.ostelco.bqmetrics


import com.fasterxml.jackson.annotation.JsonProperty
import com.google.cloud.bigquery.*
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.prometheus.client.exporter.PushGateway
import io.prometheus.client.CollectorRegistry
import io.dropwizard.Configuration
import io.dropwizard.cli.ConfiguredCommand
import io.prometheus.client.Gauge
import io.prometheus.client.Summary
import net.sourceforge.argparse4j.inf.Namespace
import net.sourceforge.argparse4j.inf.Subparser
import org.slf4j.LoggerFactory
import java.util.*
import org.slf4j.Logger
import javax.validation.Valid
import javax.validation.constraints.NotNull

/**
 * Bridge between "latent metrics" stored in BigQuery and Prometheus
 * metrics available for instrumentation ana alerting services.
 *
 * Common usecase:
 *
 *       java -jar /bq-metrics-extractor.jar query --pushgateway pushgateway:8080 config/config.yaml
 *
 * the pushgateway:8080 is the hostname  (dns resolvable) and portnumber of the
 * Prometheus Push Gateway.
 *
 * The config.yaml file contains specifications of queries and how they map
 * to metrics:
 *
 *      bqmetrics:
 *      - type: summary
 *        name: active_users
 *        help: Number of active users
 *        resultColumn: count
 *        sql: >
 *        SELECT count(distinct user_pseudo_id) AS count FROM `pantel-2decb.analytics_160712959.events_*`
 *        WHERE event_name = "first_open"
 *        LIMIT 1000
 *
 *  Use standard SQL syntax  (not legacy) for queries.
 *  See: https://cloud.google.com/bigquery/sql-reference/
 *
 *  If not running in a google kubernetes cluster (e.g. in docker compose, or from the command line),
 *  it's necessary to set the environment variable GOOGLE_APPLICATION_CREDENTIALS to point to
 *  a credentials file that will provide access for the BigQuery library.
 *
 */


/**
 * Main entry point, invoke dropwizard application.
 */
fun main(args: Array<String>) {
    BqMetricsExtractorApplication().run(*args)
}

/**
 * Config of a single metric that will be extracted using a BigQuery
 * query.
 */
private class  MetricConfig {

    /**
     * Type of the metric.  Currently the only permitted type is
     * "summary", the intent is to extend this as more types
     * of metrics (counters, gauges, ...) are added.
     */
    @Valid
    @NotNull
    @JsonProperty
    lateinit var type: String

    /**
     * The name of the metric, as it will be seen by Prometheus.
     */
    @Valid
    @NotNull
    @JsonProperty
    lateinit var name: String

    /**
     * A help string, used to describe the metric.
     */
    @Valid
    @NotNull
    @JsonProperty
    lateinit var help: String

    /**
     * When running the query, the result should be placed in a named
     * column, and this field contains the name of that column.
     */
    @Valid
    @NotNull
    @JsonProperty
    lateinit var resultColumn: String

    /**
     * The SQL used to extract the value of the metric from BigQuery.
     */
    @Valid
    @NotNull
    @JsonProperty
    lateinit var sql:  String
}


/**
 * Configuration for the extractor, default config
 * plus a list of metrics descriptions.
 */
private class BqMetricsExtractorConfig: Configuration() {
    @Valid
    @NotNull
    @JsonProperty("bqmetrics")
    lateinit var metrics: List<MetricConfig>
}


/**
 * Main entry point to the bq-metrics-extractor API server.
 */
private class BqMetricsExtractorApplication : Application<BqMetricsExtractorConfig>() {

    override fun initialize(bootstrap: Bootstrap<BqMetricsExtractorConfig>) {
        bootstrap.addCommand(CollectAndPushMetrics())
    }

    override fun run(
            configuration: BqMetricsExtractorConfig,
            environment: Environment) {
    }
}


private interface MetricBuilder {
    fun buildMetric(registry: CollectorRegistry)

    fun getNumberValueViaSql(sql: String, resultColumn: String): Long {
        // Instantiate a client. If you don't specify credentials when constructing a client, the
        // client library will look for credentials in the environment, such as the
        // GOOGLE_APPLICATION_CREDENTIALS environment variable.
        val bigquery = BigQueryOptions.getDefaultInstance().service
        val queryConfig: QueryJobConfiguration =
                QueryJobConfiguration.newBuilder(
                        sql.trimIndent())
                        .setUseLegacySql(false)
                        .build();

        // Create a job ID so that we can safely retry.
        val jobId: JobId = JobId.of(UUID.randomUUID().toString());
        var queryJob: Job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw  BqMetricsExtractionException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw BqMetricsExtractionException(queryJob.getStatus().getError().toString());
        }
        val result = queryJob.getQueryResults()
        if (result.totalRows != 1L) {
            throw BqMetricsExtractionException("Number of results was ${result.totalRows} which is different from the expected single row")
        }

        val count = result.iterateAll().iterator().next().get(resultColumn).longValue

        return count
    }
}

private class SummaryMetricBuilder(
        val metricName: String,
        val help: String,
        val sql: String,
        val resultColumn: String) : MetricBuilder {

    private val log: Logger = LoggerFactory.getLogger(SummaryMetricBuilder::class.java)


    override fun buildMetric(registry: CollectorRegistry) {
        val summary: Summary = Summary.build()
                .name(metricName)
                .help(help).register(registry)
        val value: Long = getNumberValueViaSql(sql, resultColumn)

        log.info("Summarizing metric $metricName  to be $value")

        summary.observe(value * 1.0)
    }
}

private class GaugeMetricBuilder(
        val metricName: String,
        val help: String,
        val sql: String,
        val resultColumn: String) : MetricBuilder {

    private val log: Logger = LoggerFactory.getLogger(SummaryMetricBuilder::class.java)

    override fun buildMetric(registry: CollectorRegistry) {
        val gauge: Gauge = Gauge.build()
                .name(metricName)
                .help(help).register(registry)
        val value: Long = getNumberValueViaSql(sql, resultColumn)

        log.info("Gauge metric $metricName = $value")

        gauge.set(value * 1.0)
    }
}

/**
 * Thrown when something really bad is detected and it's necessary to terminate
 * execution immediately.  No cleanup of anything will be done.
 */
private class BqMetricsExtractionException: RuntimeException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}


/**
 * Adapter class that will push metrics to the Prometheus push gateway.
 */
private class PrometheusPusher(val pushGateway: String, val job: String) {

    private val log: Logger = LoggerFactory.getLogger(PrometheusPusher::class.java)

    val registry = CollectorRegistry()

    @Throws(Exception::class)
    fun publishMetrics(metrics: List<MetricConfig>) {

        val metricSources: MutableList<MetricBuilder> = mutableListOf()
        metrics.forEach {
            val typeString: String = it.type.trim().toUpperCase()
            when (typeString) {
                "SUMMARY" -> {
                    metricSources.add(SummaryMetricBuilder(
                            it.name,
                            it.help,
                            it.sql,
                            it.resultColumn))
                }
                "GAUGE" -> {
                    metricSources.add(GaugeMetricBuilder(
                            it.name,
                            it.help,
                            it.sql,
                            it.resultColumn))
                }
                else -> {
                    log.error("Unknown metrics type '${it.type}'")
                }
            }
        }

        log.info("Querying bigquery for metric values")
        val pg = PushGateway(pushGateway)
        metricSources.forEach({ it.buildMetric(registry) })

        log.info("Pushing metrics to pushgateway")
        pg.pushAdd(registry, job)
        log.info("Done transmitting metrics to pushgateway")
    }
}

private class CollectAndPushMetrics : ConfiguredCommand<BqMetricsExtractorConfig>(
        "query",
        "query BigQuery for a metric") {
    override fun run(bootstrap: Bootstrap<BqMetricsExtractorConfig>?, namespace: Namespace?, configuration: BqMetricsExtractorConfig?) {

        if (configuration == null) {
            throw BqMetricsExtractionException("Configuration is null")
        }


        if (namespace == null) {
            throw BqMetricsExtractionException("Namespace from config is null")
        }

        val pgw = namespace.get<String>(pushgatewayKey)
        PrometheusPusher(pgw,
                "bq_metrics_extractor").publishMetrics(configuration.metrics)
    }

    val pushgatewayKey = "pushgateway"

    override fun configure(subparser: Subparser?) {
        super.configure(subparser)
        if (subparser == null) {
            throw BqMetricsExtractionException("subparser is null")
        }
        subparser.addArgument("-p", "--pushgateway")
                .dest(pushgatewayKey)
                .type(String::class.java)
                .required(true)
                .help("The pushgateway to report metrics to, format is hostname:portnumber")
    }

    private class CollectAndPushMetrics : ConfiguredCommand<BqMetricsExtractorConfig>(
            "quit",
            "Do nothing, only used to prime caches") {
        override fun run(bootstrap: Bootstrap<BqMetricsExtractorConfig>?,
                         namespace: Namespace?,
                         configuration: BqMetricsExtractorConfig?) {
            // Doing nothing, as advertised.
        }
    }
}
