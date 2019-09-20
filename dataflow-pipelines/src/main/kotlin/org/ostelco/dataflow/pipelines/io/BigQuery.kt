package org.ostelco.dataflow.pipelines.io

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.ostelco.analytics.api.AggregatedDataTrafficInfo
import org.ostelco.analytics.api.DataTrafficInfo
import org.ostelco.dataflow.pipelines.dsl.ParDoFn
import org.ostelco.dataflow.pipelines.io.Table.DAILY_CONSUMPTION
import org.ostelco.dataflow.pipelines.io.Table.HOURLY_CONSUMPTION
import org.ostelco.dataflow.pipelines.io.Table.RAW_CONSUMPTION
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

// This code is an attempt to keep all database schema in one place.

/**
 * Enum containing identifiers for three tables
 * stored in bigtable.
 */
enum class Table {
    RAW_CONSUMPTION,
    HOURLY_CONSUMPTION,
    DAILY_CONSUMPTION
}

// Need to explore if the schema can be expressed in config (like SQL file) instead of code.

/**
 * Schemas for tables.
 */
private object TableSchemas {

    /**
     * Getting a table schema for the tables
     * listed in the [Table] enum
     */
    fun getTableSchema(table: Table): TableSchema? {
        return when (table) {
            RAW_CONSUMPTION -> {
                val fields = ArrayList<TableFieldSchema>()
                fields.add(TableFieldSchema().setName("subscriptionAnalyticsId").setType("STRING"))
                fields.add(TableFieldSchema().setName("usedBucketBytes").setType("INTEGER"))
                fields.add(TableFieldSchema().setName("bundleBytes").setType("INTEGER"))
                fields.add(TableFieldSchema().setName("timestamp").setType("TIMESTAMP"))
                fields.add(TableFieldSchema().setName("apn").setType("STRING"))
                fields.add(TableFieldSchema().setName("mccMnc").setType("STRING"))
                TableSchema().setFields(fields)
            }
            HOURLY_CONSUMPTION, DAILY_CONSUMPTION -> {
                val fields = ArrayList<TableFieldSchema>()
                fields.add(TableFieldSchema().setName("msisdn").setType("STRING"))
                fields.add(TableFieldSchema().setName("bytes").setType("INTEGER"))
                fields.add(TableFieldSchema().setName("timestamp").setType("TIMESTAMP"))
                fields.add(TableFieldSchema().setName("apn").setType("STRING"))
                fields.add(TableFieldSchema().setName("mccMnc").setType("STRING"))
                TableSchema().setFields(fields)
            }
        }
    }
}

//
// convert to BigQuery table rows
//
val convertToRawTableRows = ParDoFn.transform<DataTrafficInfo, TableRow> {
    TableRow()
            .set("subscriptionAnalyticsId", it.subscriptionAnalyticsId)
            .set("usedBucketBytes", it.usedBucketBytes)
            .set("bundleBytes", it.bundleBytes)
            .set("timestamp", protobufTimestampToZonedDateTime(it.timestamp))
            .set("apn", it.apn)
            .set("mccMnc", it.mccMnc)
}

val convertToHourlyTableRows = ParDoFn.transform<AggregatedDataTrafficInfo, TableRow> {
    TableRow()
            .set("msisdn", it.msisdn)
            .set("bytes", it.dataBytes)
            .set("timestamp", protobufTimestampToZonedDateTime(it.timestamp))
            .set("apn", it.apn)
            .set("mccMnc", it.mccMnc)
}

fun protobufTimestampToZonedDateTime(timestamp: Timestamp) = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(Timestamps.toMillis(timestamp)),
        ZoneOffset.UTC).toString()
//
// Save to BigQuery Table
//

/**
 * Helpers for accessing BigTable
 */
object BigQueryIOUtils {

    /**
     * Create a [BigQueryIO.Write<TableRow>] query for writing all the
     * rows in a [Table] - denoted table.
     */
    fun saveToBigQuery(project: String, dataset: String, table: Table): BigQueryIO.Write<TableRow> {
        return BigQueryIO.writeTableRows()
                .to("$project:$dataset.${table.name.toLowerCase()}")
                .withSchema(TableSchemas.getTableSchema(table))
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
    }
}
