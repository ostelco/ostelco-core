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
import io.prometheus.client.Summary
import net.sourceforge.argparse4j.inf.Namespace
import net.sourceforge.argparse4j.inf.Subparser
import org.slf4j.LoggerFactory
import java.util.*
import org.slf4j.Logger
import javax.validation.Valid
import javax.validation.constraints.NotNull


fun main(args: Array<String>) {
    BqMetricsExtractorApplication().run(*args)
}

class  MetricConfig {

    @Valid
    @NotNull
    @JsonProperty
    lateinit var type: String

    @Valid
    @NotNull
    @JsonProperty
    lateinit var name: String

    @Valid
    @NotNull
    @JsonProperty
    lateinit var help: String

    @Valid
    @NotNull
    @JsonProperty
    lateinit var resultColumn: String

    @Valid
    @NotNull
    @JsonProperty
    lateinit var sql:  String
}


class BqMetricsExtractorConfig: Configuration() {
    @Valid
    @NotNull
    @JsonProperty("bqmetrics")
    lateinit var metrics: List<MetricConfig>
}


/**
 * Main entry point to the bq-metrics-extractor API server.
 */
class BqMetricsExtractorApplication : Application<BqMetricsExtractorConfig>() {

    override fun initialize(bootstrap: Bootstrap<BqMetricsExtractorConfig>) {
        bootstrap.addCommand(CollectAndPushMetrics())
    }

    override fun run(
            configuration: BqMetricsExtractorConfig,
            environment: Environment) {
    }
}


interface MetricBuilder {
    fun buildMetric(registry: CollectorRegistry)
}


class SummaryMetricBuilder(val metricName: String, val help: String, val sql: String, val resultColumn: String) : MetricBuilder {

    private val log: Logger = LoggerFactory.getLogger(SummaryMetricBuilder::class.java)

    fun getSummaryViaSql(): Long {
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
        val jobId: JobId = JobId.of(UUID.randomUUID().toString());
        var queryJob: Job = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            throw  RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw RuntimeException(queryJob.getStatus().getError().toString());
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
        val value: Long = getSummaryViaSql()

        log.info("Summarizing metric $metricName  to be $value")

        activeUsersSummary.observe(value * 1.0)
    }
}

/**
 * Adapter class that will push metrics to the Prometheus push gateway.
 */
class PrometheusPusher(val pushGateway: String, val job: String) {

    private val log: Logger = LoggerFactory.getLogger(PrometheusPusher::class.java)

    val registry = CollectorRegistry()

    @Throws(Exception::class)
    fun publishMetrics(metrics: List<MetricConfig>) {

        val metricSources: MutableList<MetricBuilder> = mutableListOf()
        metrics.forEach{
            if (it.type.trim().toUpperCase().equals("SUMMARY")) {
                metricSources.add(SummaryMetricBuilder(
                        it.name,
                        it.help,
                        it.sql,
                        it.resultColumn))
            } else {
                log.error("Unknown metrics type '${it.type}'")
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

class CollectAndPushMetrics : ConfiguredCommand<BqMetricsExtractorConfig>("query","query BigQuery for a metric") {
    override fun run(bootstrap: Bootstrap<BqMetricsExtractorConfig>?, namespace: Namespace?, configuration: BqMetricsExtractorConfig?) {
        val pgw = namespace!!.get<String>(pushgatewayKey)
        PrometheusPusher(pgw,
                "bq_metrics_extractor").publishMetrics(configuration!!.metrics)
    }

    val pushgatewayKey = "pushgateway"

    override fun configure(subparser: Subparser?) {
        super.configure(subparser)
        subparser!!.addArgument("-p", "--pushgateway")
                .dest(pushgatewayKey)
                .type(String::class.java)
                .required(true)
                .help("The pushgateway to report metrics to, format is hostname:portnumber")
    }
}