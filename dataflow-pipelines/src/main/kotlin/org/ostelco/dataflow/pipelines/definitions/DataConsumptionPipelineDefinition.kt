package org.ostelco.dataflow.pipelines.definitions

import com.google.protobuf.util.Timestamps
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.KvCoder
import org.apache.beam.sdk.coders.VarLongCoder
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder
import org.apache.beam.sdk.io.gcp.bigquery.TableRowJsonCoder
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
import org.ostelco.dataflow.pipelines.ConsumptionPipelineOptions
import org.ostelco.dataflow.pipelines.dsl.ParDoFn
import org.ostelco.dataflow.pipelines.io.BigQueryIOUtils.saveToBigQuery
import org.ostelco.dataflow.pipelines.io.Table.HOURLY_CONSUMPTION
import org.ostelco.dataflow.pipelines.io.Table.RAW_CONSUMPTION
import org.ostelco.dataflow.pipelines.io.convertToHourlyTableRows
import org.ostelco.dataflow.pipelines.io.convertToRawTableRows
import org.ostelco.dataflow.pipelines.io.readFromPubSub

object DataConsumptionPipelineDefinition : PipelineDefinition {

    override fun define(pipeline: Pipeline, options: ConsumptionPipelineOptions) {

        // Filter events with empty buckets
        val filterEmptyBucketEvents = Filter.by(SerializableFunction { dataTrafficInfo: DataTrafficInfo ->
            dataTrafficInfo.usedBucketBytes > 0
        })

        //
        // Construct pipeline chain
        //

        // First two common steps of pipeline, before it gets forked.
        val dataTrafficInfoEvents = pipeline
                .apply("readFromPubSub", readFromPubSub(project = options.project, topic = options.pubsubTopic))
                .apply("filterEmptyBucketEvents", filterEmptyBucketEvents)

        // PubSubEvents -> raw_consumption big-query
        dataTrafficInfoEvents
                .apply("convertToRawTableRows", convertToRawTableRows)
                .setCoder(TableRowJsonCoder.of())
                .apply("saveRawEventsToBigQuery", saveToBigQuery(
                        project = options.project,
                        dataset = options.dataset,
                        table = RAW_CONSUMPTION))

        // PubSubEvents -> aggregate by hour -> hourly_consumption big-query
        dataTrafficInfoEvents
                .apply("TotalDataConsumptionGroupByMsisdn", consumptionPerMsisdn)
                .apply("convertToHourlyTableRows", convertToHourlyTableRows)
                .setCoder(TableRowJsonCoder.of())
                .apply("saveToBigQueryGroupedByHour", saveToBigQuery(
                        project = options.project,
                        dataset = options.dataset,
                        table = HOURLY_CONSUMPTION))
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
            val hoursSinceEpoch: Long = it.timestamp.seconds / 3600
            KV.of(
                    AggregatedDataTrafficInfo.newBuilder()
                            .setMsisdn(it.subscriptionAnalyticsId)
                            .setTimestamp(Timestamps.fromSeconds(hoursSinceEpoch * 3600))
                            .setDataBytes(0)
                            .setApn(it.apn)
                            .setMccMnc(it.mccMnc)
                            .build(),
                    it.usedBucketBytes)
        }

        val reduceToSumOfBucketBytes = Combine.groupedValues<AggregatedDataTrafficInfo, Long, Long>(Sum.ofLongs())

        val kvToSingleObject = ParDoFn.transform<KV<AggregatedDataTrafficInfo, Long>, AggregatedDataTrafficInfo> {
            AggregatedDataTrafficInfo.newBuilder()
                    .setMsisdn(it.key?.msisdn)
                    .setTimestamp(it.key?.timestamp)
                    .setDataBytes(it.value)
                    .setApn(it.key?.apn)
                    .setMccMnc(it.key?.mccMnc)
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
                .setCoder(ProtoCoder.of(AggregatedDataTrafficInfo::class.java))
    }
}