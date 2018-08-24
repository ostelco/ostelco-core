package org.ostelco.pseudonym.managed

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import com.google.cloud.datastore.Cursor
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.Query
import com.google.cloud.datastore.StructuredQuery
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.ostelco.pseudonym.ExportTaskKind
import org.ostelco.pseudonym.PseudonymEntityKind
import org.ostelco.pseudonym.errorPropertyName
import org.ostelco.pseudonym.exportIdPropertyName
import org.ostelco.pseudonym.msisdnPropertyName
import org.ostelco.pseudonym.pseudonymPropertyName
import org.ostelco.pseudonym.statusPropertyName
import org.slf4j.LoggerFactory
import java.util.*

private const val datasetName = "exported_pseudonyms"
private const val msisdnFieldName = "msisdn"
private const val pseudonymFiledName = "pseudonym"
private const val idFieldName = "msisdnid"

/**
 * Exports pseudonym objects to a bigquery Table
 */
class PseudonymExport(private val exportId: String, private val bigquery: BigQuery, private val datastore: Datastore) {
    private val logger = LoggerFactory.getLogger(PseudonymExport::class.java)

    /**
     * Status of the export operation in progress.
     */
    enum class Status {
        INITIAL, RUNNING, FINISHED, ERROR
    }

    private val tableName: String = exportId.replace("-", "")
    private val idCache: Cache<String, String> = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .build()
    private var status = Status.INITIAL
    private var error: String = ""

    init {
        upsertTaskStatus()
    }

    private fun createTable(): Table {
        // Delete existing table
        val deleted = bigquery.delete(datasetName, tableName)
        if (deleted) {
            logger.info("Existing table deleted.")
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

    private fun getIdForMsisdn(msisdn: String): String {
        // Retrieves the element from cache.
        // Incase of cache miss, generate a new UUID
        return idCache.get(msisdn) { UUID.randomUUID().toString() }
    }

    private fun createTablePage(pageSize: Int, cursor: Cursor?, table: Table): Cursor? {
        // Dump pseudonyms to BQ, one page at a time. Since all records in a
        // page are inserted at once, use a small page size
        val queryBuilder = Query.newEntityQueryBuilder()
                .setKind(PseudonymEntityKind)
                .setOrderBy(StructuredQuery.OrderBy.asc(msisdnPropertyName))
                .setLimit(pageSize)
        if (cursor != null) {
            queryBuilder.setStartCursor(cursor)
        }
        val rows = ArrayList<RowToInsert>()
        val pseudonyms = datastore.run(queryBuilder.build())
        var totalPseudonyms = 0
        while (pseudonyms.hasNext()) {
            val entity = pseudonyms.next()
            totalPseudonyms++
            val row = hashMapOf(
                    msisdnFieldName to entity.getString(msisdnPropertyName),
                    pseudonymFiledName to entity.getString(pseudonymPropertyName),
                    idFieldName to getIdForMsisdn(entity.getString(msisdnPropertyName)))
            val rowId = "rowId$totalPseudonyms"
            rows.add(RowToInsert.of(rowId, row))
        }
        if (totalPseudonyms != 0) {
            val response = table.insert(rows, true, true)
            if (response.hasErrors()) {
                logger.error("Failed to insert Records", response.insertErrors)
                error = "$error${response.insertErrors}\n"
            }
        }
        return if (totalPseudonyms < pageSize) {
            null
        } else {
            pseudonyms.cursorAfter
        }
    }

    private fun start() {
        logger.info("Starting to export Pseudonyms for $exportId")
        status = Status.RUNNING
        upsertTaskStatus()
        val table = createTable()
        var cursor: Cursor? = null
        do {
            cursor = createTablePage(100, cursor, table)
        } while (cursor != null)
        if (status == Status.RUNNING) {
            status = Status.FINISHED
            upsertTaskStatus()
        }
        logger.info("Exported Pseudonyms for $exportId")
    }

    /**
     * Returns a runnable that can be passed to executor. It starts the
     * export operation.
     */
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
            val currentEntity = transaction.get(exportKey)
            val builder: Entity.Builder =
                    if (currentEntity == null) {
                        Entity.newBuilder(exportKey)
                    } else {
                        Entity.newBuilder(currentEntity)
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