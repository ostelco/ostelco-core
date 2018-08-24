package org.ostelco.analytics

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
import org.ostelco.analytics.api.AggregatedDataTrafficInfo
import org.ostelco.analytics.api.DataTrafficInfo
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PipelineTest {

    @Rule
    @Transient
    @JvmField
    val pipeline: TestPipeline? = TestPipeline.create()

    @Test
    @Category(NeedsRunner::class)
    fun testPipeline() {

        val testStream: TestStream<DataTrafficInfo> =
                TestStream.create(ProtoCoder.of(DataTrafficInfo::class.java))
                        .addElements(
                                DataTrafficInfo.newBuilder()
                                        .setMsisdn("123")
                                        .setBucketBytes(100)
                                        .setBundleBytes(900)
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().millis))
                                        .build(),
                                DataTrafficInfo.newBuilder()
                                        .setMsisdn("123")
                                        .setBucketBytes(100)
                                        .setBundleBytes(800)
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().millis))
                                        .build(),
                                DataTrafficInfo.newBuilder()
                                        .setMsisdn("123")
                                        .setBucketBytes(100)
                                        .setBundleBytes(700)
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().millis))
                                        .build(),
                                DataTrafficInfo.newBuilder()
                                        .setMsisdn("456")
                                        .setBucketBytes(100)
                                        .setBundleBytes(900)
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().millis))
                                        .build(),
                                DataTrafficInfo.newBuilder()
                                        .setMsisdn("456")
                                        .setBucketBytes(100)
                                        .setBundleBytes(800)
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().millis))
                                        .build(),
                                DataTrafficInfo.newBuilder()
                                        .setMsisdn("789")
                                        .setBucketBytes(100)
                                        .setBundleBytes(900)
                                        .setTimestamp(Timestamps.fromMillis(Instant.now().millis))
                                        .build())
                        .advanceWatermarkToInfinity()


        if (pipeline != null) {
            val currentHourDateTime = getCurrentHourDateTime()

            val out: PCollection<AggregatedDataTrafficInfo> = appendTransformations(pipeline.apply(testStream))
                    .setCoder(ProtoCoder.of(AggregatedDataTrafficInfo::class.java))

            PAssert.that(out).containsInAnyOrder(
                    AggregatedDataTrafficInfo.newBuilder().setMsisdn("123").setDataBytes(300).setDateTime(currentHourDateTime).build(),
                    AggregatedDataTrafficInfo.newBuilder().setMsisdn("456").setDataBytes(200).setDateTime(currentHourDateTime).build(),
                    AggregatedDataTrafficInfo.newBuilder().setMsisdn("789").setDataBytes(100).setDateTime(currentHourDateTime).build())

            pipeline.run().waitUntilFinish()
        }
    }

    private fun getCurrentHourDateTime(): String {
        val zonedDateTime = ZonedDateTime
                .ofInstant(java.time.Instant.now(), ZoneOffset.UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:SS")
        return formatter.format(zonedDateTime)
    }
}