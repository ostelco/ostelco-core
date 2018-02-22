package com.telenordigital.ostelco.analytics

import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableRow
import com.google.api.services.bigquery.model.TableSchema
import com.telenordigital.ostelco.analytics.Table.DAILY_CONSUMPTION
import com.telenordigital.ostelco.analytics.Table.HOURLY_CONSUMPTION
import com.telenordigital.ostelco.analytics.Table.RAW_CONSUMPTION
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import java.util.*

// This code is an attempt to keep all database schema in one place.

// This may be moved to config.
val project = "pantel-2decb"
val dataset = "data_consumption"

enum class Table {
    RAW_CONSUMPTION,
    HOURLY_CONSUMPTION,
    DAILY_CONSUMPTION
}

// Need to explore if the schema can be expressed in config (like SQL file) instead of code.
class TableSchemas {
    fun getTableSchema(table: Table): TableSchema? {
        return when (table) {
            RAW_CONSUMPTION -> {
                val fields = ArrayList<TableFieldSchema>()
                fields.add(TableFieldSchema().setName("msisdn").setType("STRING"))
                fields.add(TableFieldSchema().setName("bucketBytes").setType("INTEGER"))
                fields.add(TableFieldSchema().setName("bundleBytes").setType("INTEGER"))
                fields.add(TableFieldSchema().setName("timestamp").setType("TIMESTAMP"))
                TableSchema().setFields(fields)
            }
            HOURLY_CONSUMPTION, DAILY_CONSUMPTION -> {
                val fields = ArrayList<TableFieldSchema>()
                fields.add(TableFieldSchema().setName("msisdn").setType("STRING"))
                fields.add(TableFieldSchema().setName("bytes").setType("INTEGER"))
                fields.add(TableFieldSchema().setName("timestamp").setType("DATETIME"))
                TableSchema().setFields(fields)
            }
        }
    }
}

class BigQueryIOUtils {
    fun writeTo(table: Table) : BigQueryIO.Write<TableRow> {
        return BigQueryIO.writeTableRows()
                .to("%s:%s.%s".format(project, dataset, table.name.toLowerCase()))
                .withSchema(TableSchemas().getTableSchema(table))
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
    }
}