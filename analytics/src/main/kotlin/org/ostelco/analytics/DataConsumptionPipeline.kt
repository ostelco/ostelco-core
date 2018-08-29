package org.ostelco.analytics

import ch.qos.logback.classic.util.ContextInitializer
import com.google.api.services.bigquery.model.TableRow
import com.google.protobuf.util.Timestamps
import org.apache.beam.runners.dataflow.DataflowRunner
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.KvCoder
import org.apache.beam.sdk.coders.VarLongCoder
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.Combine
import org.apache.beam.sdk.transforms.Filter
import org.apache.beam.sdk.transforms.GroupByKey
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.transforms.Sum
import org.apache.beam.sdk.transforms.WithTimestamps
import org.apache.beam.sdk.transforms.windowing.FixedWindows
import org.apache.beam.sdk.transforms.windowing.Window
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.joda.time.Duration
import org.joda.time.Instant
import org.ostelco.analytics.ParDoFn.transform
import org.ostelco.analytics.Table.HOURLY_CONSUMPTION
import org.ostelco.analytics.Table.RAW_CONSUMPTION
import org.ostelco.analytics.api.AggregatedDataTrafficInfo
import org.ostelco.analytics.api.DataTrafficInfo
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun main(args: Array<String>) {

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "config/logback.xml")

    // may be we need to pass options via command-line args
    /*
    val options = PipelineOptionsFactory
            .fromArgs(
                    "--project=pantel-2decb",
                    "--runner=DataflowRunner",
                    "--stagingLocation=gs://data-traffic/staging/",
                    "--jobName=data-traffic")
            .withValidation()
            .create()
    */

    val options = PipelineOptionsFactory.`as`(DataflowPipelineOptions::class.java)
    options.jobName = "data-traffic"
    options.project = "pantel-2decb"
    options.stagingLocation = "gs://data-traffic/staging/"
    options.region = "europe-west1"
    options.runner = DataflowRunner::class.java
    options.isUpdate = true

    val pipeline = Pipeline.create(options)

    val dataTrafficInfoEvents = pipeline
            .apply(PubsubIO.readProtos(DataTrafficInfo::class.java)
                    .fromSubscription("projects/pantel-2decb/subscriptions/data-traffic"))
            .apply<PCollection<DataTrafficInfo>>(Filter.by(SerializableFunction { it.bucketBytes > 0 }))

    val saveRawEventsToBigQuery = BigQueryIOUtils().writeTo(RAW_CONSUMPTION)

    val saveToBigQueryGroupedByHour = BigQueryIOUtils().writeTo(HOURLY_CONSUMPTION)

    val convertToRawTableRows = transform<DataTrafficInfo, TableRow> {
        TableRow()
                .set("msisdn", it.msisdn)
                .set("bucketBytes", it.bucketBytes)
                .set("bundleBytes", it.bundleBytes)
                .set("timestamp", ZonedDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(Timestamps.toMillis(it.timestamp)),
                        ZoneOffset.UTC).toString())
    }

    val convertToHourlyTableRows = transform<AggregatedDataTrafficInfo, TableRow> {
        TableRow()
                .set("msisdn", it.msisdn)
                .set("bytes", it.dataBytes)
                .set("timestamp", it.dateTime)
    }

    // PubSubEvents -> raw_consumption big-query
    dataTrafficInfoEvents
            .apply(convertToRawTableRows)
            .apply(saveRawEventsToBigQuery)

    // PubSubEvents -> aggregate by hour -> hourly_consumption big-query
    appendTransformations(dataTrafficInfoEvents)
            .apply(convertToHourlyTableRows)
            .apply(saveToBigQueryGroupedByHour)

    pipeline.run()
            .waitUntilFinish()
}

// This method has a part of pipeline which is independent of GCP PubSubIO and BigQueryIO.
// So, this part of the pipeline can be run locally and does not need GCP.
// This separation is done so that it can be tested using JUnit.
fun appendTransformations(inCollection: PCollection<DataTrafficInfo>): PCollection<AggregatedDataTrafficInfo> {

    val linkTimestamps = WithTimestamps
            .of<DataTrafficInfo> { Instant(Timestamps.toMillis(it.timestamp)) }
            .withAllowedTimestampSkew(Duration.standardMinutes(1L))

    val groupByHour: Window<DataTrafficInfo> = Window
            .into<DataTrafficInfo>(FixedWindows.of(Duration.standardHours(1L)))
            .withAllowedLateness(Duration.standardMinutes(1L))
            .discardingFiredPanes()

    val toKeyValuePair = transform<DataTrafficInfo, KV<AggregatedDataTrafficInfo, Long>> {
        val zonedDateTime = ZonedDateTime
                .ofInstant(java.time.Instant.ofEpochMilli(Timestamps.toMillis(it.timestamp)), ZoneOffset.UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:SS")
        KV.of(
                AggregatedDataTrafficInfo.newBuilder()
                        .setMsisdn(it.msisdn)
                        .setDateTime(formatter.format(zonedDateTime))
                        .setDataBytes(0)
                        .build(),
                it.bucketBytes)
    }

    val reduceToSumOfBucketBytes = Combine.groupedValues<AggregatedDataTrafficInfo, Long, Long>(Sum.ofLongs())

    val kvToSingleObject = transform<KV<AggregatedDataTrafficInfo, Long>, AggregatedDataTrafficInfo> {
        AggregatedDataTrafficInfo.newBuilder()
                .setMsisdn(it.key?.msisdn)
                .setDateTime(it.key?.dateTime)
                .setDataBytes(it.value)
                .build()
    }

    // In this method, the code above is declaring all transformations.
    // Whereas the code below is chaining them into a pipeline.

    return inCollection
            // In order to use timestamp in the event object instead of timestamp when event was registered to PubSub
            .apply("linkTimestamps", linkTimestamps)
            .apply("groupByHour", groupByHour)
            // change to KV and then group by Key
            .apply("toKeyValuePair", toKeyValuePair)
            .setCoder(KvCoder.of(ProtoCoder.of(AggregatedDataTrafficInfo::class.java), VarLongCoder.of()))
            .apply("groupByKey", GroupByKey.create())
            // sum for each group
            .apply("reduceToSumOfBucketBytes", reduceToSumOfBucketBytes)
            .apply("kvToSingleObject", kvToSingleObject)
}