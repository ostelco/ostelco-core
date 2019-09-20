package org.ostelco.dataflow.pipelines

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import org.apache.beam.sdk.extensions.protobuf.ProtoCoder
import org.apache.beam.sdk.testing.NeedsRunner
import org.apache.beam.sdk.testing.PAssert
import org.apache.beam.sdk.testing.TestPipeline
import org.apache.beam.sdk.testing.TestStream
import org.apache.beam.sdk.values.PCollection
import org.joda.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE.JAVA_8
import org.ostelco.analytics.api.AggregatedDataTrafficInfo
import org.ostelco.analytics.api.DataTrafficInfo
import org.ostelco.dataflow.pipelines.definitions.consumptionPerMsisdn

class ConsumptionPerMsisdnTest {

    @Rule
    @JvmField
    val pipeline: TestPipeline? = TestPipeline.create()

    @Test
    @EnabledOnJre(JAVA_8)
    @Category(NeedsRunner::class)
    fun testPipeline() {

        val testStream: TestStream<DataTrafficInfo> =
                TestStream.create(ProtoCoder.of(DataTrafficInfo::class.java))
                        .addElements(
                                createDataTrafficInfo(msisdn = "123", bucketBytes = 100, bundleBytes = 900, apn = "ostelco", mccMnc = "242_02"),
                                createDataTrafficInfo(msisdn = "123", bucketBytes = 100, bundleBytes = 800, apn = "ostelco", mccMnc = "242_02"),
                                createDataTrafficInfo(msisdn = "123", bucketBytes = 100, bundleBytes = 700, apn = "ostelco", mccMnc = "242_02"),
                                createDataTrafficInfo(msisdn = "123", bucketBytes = 100, bundleBytes = 600, apn = "pi", mccMnc = "242_02"),
                                createDataTrafficInfo(msisdn = "456", bucketBytes = 100, bundleBytes = 900, apn = "ostelco", mccMnc = "242_02"),
                                createDataTrafficInfo(msisdn = "456", bucketBytes = 100, bundleBytes = 800, apn = "ostelco", mccMnc = "242_02"),
                                createDataTrafficInfo(msisdn = "789", bucketBytes = 100, bundleBytes = 900, apn = "ostelco", mccMnc = "242_02"))
                        .advanceWatermarkToInfinity()


        if (pipeline != null) {
            val currentHourDateTime = getCurrentHourDateTime()

            val out: PCollection<AggregatedDataTrafficInfo> = pipeline
                    .apply(testStream)
                    .apply(consumptionPerMsisdn)
                    .setCoder(ProtoCoder.of(AggregatedDataTrafficInfo::class.java))

            PAssert.that(out).containsInAnyOrder(
                    createAggregatedDataTrafficInfo(msisdn = "123", dataBytes = 300, timestamp = currentHourDateTime, apn = "ostelco", mccMnc = "242_02"),
                    createAggregatedDataTrafficInfo(msisdn = "123", dataBytes = 100, timestamp = currentHourDateTime, apn = "pi", mccMnc = "242_02"),
                    createAggregatedDataTrafficInfo(msisdn = "456", dataBytes = 200, timestamp = currentHourDateTime, apn = "ostelco", mccMnc = "242_02"),
                    createAggregatedDataTrafficInfo(msisdn = "789", dataBytes = 100, timestamp = currentHourDateTime, apn = "ostelco", mccMnc = "242_02"))

            pipeline.run().waitUntilFinish()
        }
    }

    private fun getCurrentHourDateTime(): Timestamp = Timestamps.fromSeconds((java.time.Instant.now().epochSecond / 3600) * 3600)

    private fun createDataTrafficInfo(
            msisdn: String,
            bucketBytes: Long,
            bundleBytes: Long,
            apn: String,
            mccMnc: String): DataTrafficInfo =

            DataTrafficInfo.newBuilder()
                    .setSubscriptionAnalyticsId(msisdn)
                    .setUsedBucketBytes(bucketBytes)
                    .setBundleBytes(bundleBytes)
                    .setTimestamp(Timestamps.fromMillis(Instant.now().millis))
                    .setApn(apn)
                    .setMccMnc(mccMnc)
                    .build()

    private fun createAggregatedDataTrafficInfo(
            msisdn: String,
            dataBytes: Long,
            timestamp: Timestamp,
            apn: String,
            mccMnc: String): AggregatedDataTrafficInfo =

            AggregatedDataTrafficInfo.newBuilder()
                    .setMsisdn(msisdn)
                    .setDataBytes(dataBytes)
                    .setTimestamp(timestamp)
                    .setApn(apn)
                    .setMccMnc(mccMnc)
                    .build()
}