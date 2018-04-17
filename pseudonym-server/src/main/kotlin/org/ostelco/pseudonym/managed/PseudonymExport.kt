package org.ostelco.pseudonym.managed

import com.google.cloud.bigquery.*
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert
import com.google.cloud.datastore.*
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.ostelco.pseudonym.resources.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Callable


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

class PseudonymExport(val exportId: String, val bigquery: BigQuery, val datastore: Datastore) {
    private val LOG = LoggerFactory.getLogger(PseudonymExport::class.java)

    enum class Status {
        INITIAL, RUNNING, FINISHED, ERROR
    }

    private val datasetName = "exported_pseudonyms"
    private val tableName:String
    private val msisdnFieldName = "msisdn"
    private val pseudonymFiledName = "pseudonym"
    private val idFieldName = "msisdnid"
    val idCache: Cache<String, String>
    var status = Status.INITIAL
    var error: String = ""

    init {
        tableName = exportId.replace("-","")
        idCache = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .build()
    }

    private fun createTable(): Table {
        // Delete existing table
        val deleted = bigquery.delete(datasetName, tableName)
        if (deleted) {
            LOG.info("Existing table deleted.")
        }
        val tableId = TableId.of(datasetName, tableName)
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
                .setOrderBy(StructuredQuery.OrderBy.asc(msisdnPropertyName))
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
                error = "$error${response.insertErrors.toString()}\n"
            }
        }
        if(results < pageSize) {
            return null
        } else {
            return pseudonyms.getCursorAfter()
        }
    }

    private fun start() {
        status = Status.RUNNING
        upsertTaskStatus()
        val table = createTable()
        var cursor: Cursor? = null
        do {
            cursor = createTablePage(100, cursor, table)
        } while(cursor != null)
        if (status == Status.RUNNING) {
            status = Status.FINISHED
            upsertTaskStatus()
        }
        LOG.info("Exported Pseudonyms for ${exportId}")
    }

    fun getRunnable(): Runnable {
        return Runnable {
            start()
        }
    }

    private fun upsertTaskStatus() {
        val exportKey = datastore.newKeyFactory().setKind(ExportTaskKind).newKey(exportId)
        val transaction = datastore.newTransaction()
        try {
            // Verify before writing a new value.
            val currentEntity = transaction.get(exportKey);
            var builder:Entity.Builder?
            if (currentEntity == null) {
                builder = Entity.newBuilder(exportKey)
            } else {
                builder = Entity.newBuilder(currentEntity)
            }
            // Prepare the new datastore entity
            val exportTask = builder
                    .set(exportIdPropertyName, exportId)
                    .set(statusPropertyName, status.toString())
                    .set(errorPropertyName, error)
                    .build()
            transaction.put(exportTask)
            transaction.commit()
        } finally {
            if (transaction.isActive) {
                transaction.rollback()
            }
        }
    }

}