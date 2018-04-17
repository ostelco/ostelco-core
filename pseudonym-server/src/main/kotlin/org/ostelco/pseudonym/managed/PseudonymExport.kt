package org.ostelco.pseudonym.managed

import com.google.cloud.bigquery.*
import com.google.cloud.datastore.*
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.ostelco.pseudonym.resources.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Callable
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert
import java.util.ArrayList


/**
 * Exports all pseudonyms to a bigquery Table
 *  SELECT
        hc.bytes, ps.msisdnid, hc.timestamp
    FROM
        [pantel-2decb:data_consumption.hourly_consumption] as hc
    JOIN
        [pantel-2decb:exported_pseudonyms.3ebcdc4a7ecc4cd385e82087e49b7b7b] as ps
    ON  ps.msisdn = hc.msisdn
  */

class PseudonymExport(val exportUUID: String, val bigquery: BigQuery, val datastore: Datastore) {
    private val LOG = LoggerFactory.getLogger(PseudonymExport::class.java)

    private val msisdnFieldName = "msisdn"
    private val pseudonymFiledName = "pseudonym"
    private val idFieldName = "msisdnid"
    val idCache: Cache<String, String>

    init {
        idCache = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .build()
    }

    private fun createTable(datasetName: String, tableName: String): Table {
        val tableId = TableId.of(datasetName, tableName.replace("-",""))
        // Table field definition
        val id = Field.of(idFieldName, LegacySQLTypeName.STRING)
        val pseudonym = Field.of(pseudonymFiledName, LegacySQLTypeName.STRING)
        val msisdn = Field.of(msisdnFieldName, LegacySQLTypeName.STRING)
        // Table schema definition
        val schema = Schema.of(id, pseudonym, msisdn)
        val tableDefinition = StandardTableDefinition.of(schema)
        val tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
        return bigquery.create(tableInfo)
    }

    private fun getIdForMsisdn(msisdn : String): String {
        // Retrieves the element from cache.
        // Incase of cache miss, generate a new UUID
        return idCache.get(msisdn, Callable {
            UUID.randomUUID().toString()
        })
    }

    private fun createTablePage(pageSize: Int, cursor: Cursor?, table: Table): Cursor? {
        val queryBuilder = Query.newEntityQueryBuilder()
                .setKind(PseudonymEntityKind)
                .setLimit(pageSize)
        if (cursor != null) {
            queryBuilder.setStartCursor(cursor)
        }
        val pseudonyms = datastore.run(queryBuilder.build())
        var results = 0
        while(pseudonyms.hasNext()) {
            val entity = pseudonyms.next()
            results++
            val row = hashMapOf(
                    msisdnFieldName to entity.getString(msisdnPropertyName),
                    pseudonymFiledName to entity.getString(pseudonymPropertyName),
                    idFieldName to getIdForMsisdn(entity.getString(msisdnPropertyName)))
            LOG.info("Row = ${row.toString()}")
            val rowId = "rowId${results}"
            val rows = ArrayList<RowToInsert>()
            rows.add(RowToInsert.of(rowId, row))
            val response = table.insert(rows, true, true)
            if(response.hasErrors()) {
                LOG.error("Failed to insert Records", response.insertErrors)
            }
        }
        if(results < pageSize) {
            return null
        } else {
            return pseudonyms.getCursorAfter()
        }
    }

    fun start() {
        val table = createTable("exported_pseudonyms", exportUUID)
        var cursor: Cursor? = null
        do {
            cursor = createTablePage(100, cursor, table)
        } while(cursor != null)
        LOG.info("Exported Pseudonyms for ${exportUUID}")
    }

}