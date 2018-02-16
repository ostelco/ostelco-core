package com.telenordigital.ostelco.analytics

import ch.qos.logback.classic.util.ContextInitializer
import com.google.api.services.bigquery.model.TableRow
import com.google.protobuf.util.Timestamps
import com.telenordigital.ostelco.analytics.SumOfDataBytesConsumed.Accum
import com.telenordigital.ostelco.analytics.Table.HOURLY_CONSUMPTION
import com.telenordigital.ostelco.analytics.Table.RAW_CONSUMPTION
import com.telenordigital.prime.ocs.AggregatedDataTrafficInfo
import com.telenordigital.prime.ocs.DataTrafficInfo
import org.apache.beam.runners.dataflow.DataflowRunner
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.KvCoder
import org.apache.beam.sdk.coders.StringUtf8Coder
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.Combine
import org.apache.beam.sdk.transforms.Combine.CombineFn
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.GroupByKey
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.transforms.Values
import org.apache.beam.sdk.transforms.WithTimestamps
import org.apache.beam.sdk.transforms.windowing.FixedWindows
import org.apache.beam.sdk.transforms.windowing.Window
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.joda.time.Duration
import org.joda.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun main(args: Array<String>) {

    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "config/logback.xml");

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
    options.project = "pantel-2decb"
    options.runner = DataflowRunner::class.java
    options.stagingLocation = "gs://data-traffic/staging/"
    options.isUpdate = true
    options.region = "europe-west1"

    val pipeline = Pipeline.create(options)

    val dataTrafficInfoEvents = pipeline
            .apply(PubsubIO.readProtos(DataTrafficInfo::class.java)
                    .fromSubscription("projects/pantel-2decb/subscriptions/data-traffic"))

    val saveRawEventsToBigQuery = BigQueryIOUtils().writeTo(RAW_CONSUMPTION)

    val saveToBigQueryGroupedByHour = BigQueryIOUtils().writeTo(HOURLY_CONSUMPTION)

    val convertToRawTableRows = ParDo.of(object : DoFn<DataTrafficInfo, TableRow>() {
        @ProcessElement
        fun processElement(c: ProcessContext) {
            val dataTrafficInfo = c.element()
            c.output(TableRow()
                    .set("msisdn", dataTrafficInfo.msisdn)
                    .set("bucketBytes", dataTrafficInfo.bucketBytes)
                    .set("bundleBytes", dataTrafficInfo.bundleBytes)
                    .set("timestamp", ZonedDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(Timestamps.toMillis(dataTrafficInfo.timestamp)),
                            ZoneOffset.UTC).toString()))
        }
    })

    val convertToHourlyTableRows = ParDo.of(object : DoFn<AggregatedDataTrafficInfo, TableRow>() {
        @ProcessElement
        fun processElement(c: ProcessContext) {
            val value = c.element()
            c.output(TableRow()
                    .set("msisdn", value.msisdn)
                    .set("bytes", value.dataBytes)
                    .set("timestamp", value.dateTime))
        }
    })

    // PubSubEvents -> raw_consumption big-query
    dataTrafficInfoEvents
            .apply(convertToRawTableRows)
            .apply(saveRawEventsToBigQuery)

    // PubSubEvents -> aggregate by hour -> hourly_consumption big-query
//    appendTransformations(dataTrafficInfoEvents)
//            .apply(convertToHourlyTableRows)
//            .apply(saveToBigQueryGroupedByHour)

    pipeline.run()
            .waitUntilFinish()
}

// This method has a part of pipeline which is independent of GCP PubSubIO and BigQueryIO.
// So, this part of the pipeline can be run locally and does not need GCP.
// This separation is done so that it can be tested using JUnit.
fun appendTransformations(inCollection: PCollection<DataTrafficInfo>): PCollection<AggregatedDataTrafficInfo> {

    val linkTimestamps = WithTimestamps.of<DataTrafficInfo> { Instant(Timestamps.toMillis(it.timestamp)) }

    val groupByHour: Window<DataTrafficInfo> = Window.into(FixedWindows.of(Duration.standardHours(1L)))

    // One line code for transformation is not working for some reason. So having verbose one till it it is investigated.
//     val toKeyValuePairWithMsisdnAsKey = WithKeys.of<String, DataTrafficInfo> { it.msisdn }
    val toKeyValuePairWithMsisdnAsKey = ParDo.of(object : DoFn<DataTrafficInfo, KV<String, DataTrafficInfo>>() {
        @ProcessElement
        fun processElement(c: ProcessContext) {
            c.output(KV.of(c.element().msisdn, c.element()))
        }
    })

    val reduceToSumOfBucketBytes = Combine.groupedValues<String, DataTrafficInfo, AggregatedDataTrafficInfo>(SumOfDataBytesConsumed())

    // In this method, the code above is declaring all transformations.
    // Whereas the code below is chaining them into a pipeline.

    return inCollection
            .apply(linkTimestamps)
            .apply(groupByHour)
            .apply(toKeyValuePairWithMsisdnAsKey)
            .apply(GroupByKey.create())
            .apply(reduceToSumOfBucketBytes)
            .setCoder(KvCoder.of(StringUtf8Coder.of(), ProtoCoder.of(AggregatedDataTrafficInfo::class.java)))
            .apply(Values.create<AggregatedDataTrafficInfo>())
}

// Since the combine function is not a simple one,
// so cannot use the inbuilt functions, need to write custom one.
class SumOfDataBytesConsumed : CombineFn<DataTrafficInfo, Accum, AggregatedDataTrafficInfo>() {

    data class Accum(
            var msisdn : String = "",
            var bucketBytes : Long= 0L,
            var dateTime : String = "")

    override fun createAccumulator(): Accum {
        return Accum()
    }

    override fun addInput(accumulator: Accum, input: DataTrafficInfo): Accum {
        accumulator.msisdn = input.msisdn
        val zonedDateTime = ZonedDateTime
                .ofInstant(java.time.Instant.ofEpochMilli(Timestamps.toMillis(input.timestamp)), ZoneOffset.UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:SS")
        accumulator.dateTime = formatter.format(zonedDateTime)
        accumulator.bucketBytes += input.bucketBytes
        return accumulator
    }

    override fun mergeAccumulators(accumulators: MutableIterable<Accum>): Accum {
        val accumulator = Accum()
        accumulator.msisdn = accumulators.elementAt(0).msisdn
        accumulator.dateTime = accumulators.elementAt(0).dateTime
        accumulator.bucketBytes = accumulators.map { it.bucketBytes }.sum()
        return accumulator
    }

    override fun extractOutput(accumulator: Accum): AggregatedDataTrafficInfo {
        return AggregatedDataTrafficInfo.newBuilder()
                .setMsisdn(accumulator.msisdn)
                .setDataBytes(accumulator.bucketBytes)
                .setDateTime(accumulator.dateTime)
                .build()
    }
}