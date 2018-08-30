package org.ostelco.dataflow.pipelines.definitions

import com.google.protobuf.util.Timestamps
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.KvCoder
import org.apache.beam.sdk.coders.VarLongCoder
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder
import org.apache.beam.sdk.transforms.Combine
import org.apache.beam.sdk.transforms.Filter
import org.apache.beam.sdk.transforms.GroupByKey
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.transforms.Sum
import org.apache.beam.sdk.transforms.WithTimestamps
import org.apache.beam.sdk.transforms.windowing.FixedWindows
import org.apache.beam.sdk.transforms.windowing.Window
import org.apache.beam.sdk.values.KV
import org.apache.beam.sdk.values.PCollection
import org.joda.time.Duration
import org.joda.time.Instant
import org.ostelco.analytics.api.AggregatedDataTrafficInfo
import org.ostelco.analytics.api.DataTrafficInfo
import org.ostelco.dataflow.pipelines.dsl.ParDoFn
import org.ostelco.dataflow.pipelines.io.BigQueryIOUtils.saveToBigQuery
import org.ostelco.dataflow.pipelines.io.Table.HOURLY_CONSUMPTION
import org.ostelco.dataflow.pipelines.io.Table.RAW_CONSUMPTION
import org.ostelco.dataflow.pipelines.io.convertToHourlyTableRows
import org.ostelco.dataflow.pipelines.io.convertToRawTableRows
import org.ostelco.dataflow.pipelines.io.readFromPubSub
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object DataConsumptionPipelineDefinition : PipelineDefinition {

    override fun define(pipeline: Pipeline) {

        // Filter events with empty buckets
        val filterEmptyBucketEvents = Filter.by(SerializableFunction { dataTrafficInfo: DataTrafficInfo ->
            dataTrafficInfo.bucketBytes > 0
        })

        //
        // Construct pipeline chain
        //


        // First two common steps of pipeline, before it gets forked.
        val dataTrafficInfoEvents = pipeline
                .apply("readFromPubSub", readFromPubSub("data-traffic"))
                .apply("filterEmptyBucketEvents", filterEmptyBucketEvents)

        // PubSubEvents -> raw_consumption big-query
        dataTrafficInfoEvents
                .apply("convertToRawTableRows", convertToRawTableRows)
                .apply("saveRawEventsToBigQuery", saveToBigQuery(RAW_CONSUMPTION))

        // PubSubEvents -> aggregate by hour -> hourly_consumption big-query
        dataTrafficInfoEvents
                .apply("TotalDataConsumptionGroupByMsisdn", consumptionPerMsisdn)
                .apply("convertToHourlyTableRows", convertToHourlyTableRows)
                .apply("saveToBigQueryGroupedByHour", saveToBigQuery(HOURLY_CONSUMPTION))
    }
}

// This method has a part of pipeline which is independent of GCP PubSubIO and BigQueryIO.
// So, this part of the pipeline can be run locally and does not need GCP.
// This separation is done so that it can be tested using JUnit.
val consumptionPerMsisdn = object : PTransform<PCollection<DataTrafficInfo>, PCollection<AggregatedDataTrafficInfo>>() {

    override fun expand(inCollection: PCollection<DataTrafficInfo>): PCollection<AggregatedDataTrafficInfo> {

        val linkTimestamps = WithTimestamps
                .of<DataTrafficInfo> { Instant(Timestamps.toMillis(it.timestamp)) }
                .withAllowedTimestampSkew(Duration.standardMinutes(1L))

        val groupByHour: Window<DataTrafficInfo> = Window
                .into<DataTrafficInfo>(FixedWindows.of(Duration.standardHours(1L)))
                .withAllowedLateness(Duration.standardMinutes(1L))
                .discardingFiredPanes()

        val toKeyValuePair = ParDoFn.transform<DataTrafficInfo, KV<AggregatedDataTrafficInfo, Long>> {
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

        val kvToSingleObject = ParDoFn.transform<KV<AggregatedDataTrafficInfo, Long>, AggregatedDataTrafficInfo> {
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
}