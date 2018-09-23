package org.ostelco.pseudonym.service

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
import org.apache.commons.codec.binary.Hex
import org.ostelco.pseudonym.*
import org.slf4j.LoggerFactory
import java.util.*
import java.security.MessageDigest

private const val datasetName = "exported_pseudonyms"
private const val idFieldName = "pseudoid"

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

    private var status = Status.INITIAL
    private var error: String = ""
    private val msisdnExporter: DS2BQExporter = DS2BQExporter(
            tableName = tableName("msisdn"),
            sourceEntity = MsisdnPseudonymEntityKind,
            sourceField = msisdnPropertyName,
            datasetName = datasetName,
            datastore = datastore,
            bigquery = bigquery)
    private val subscriberIdExporter: DS2BQExporter = DS2BQExporter(
            tableName = tableName("subscriber"),
            sourceEntity = SubscriberIdPseudonymEntityKind,
            sourceField = subscriberIdPropertyName,
            datasetName = datasetName,
            datastore = datastore,
            bigquery = bigquery)

    init {
        upsertTaskStatus()
    }

    private fun tableName(suffix:String) = "${exportId.replace("-", "")}_$suffix"

    private fun start() {
        logger.info("Starting to export Pseudonyms for $exportId")
        status = Status.RUNNING
        upsertTaskStatus()
        msisdnExporter.doExport()
        subscriberIdExporter.doExport()
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

/**
 * Class for exporting Datastore tables to BigQuery.
 */
class DS2BQExporter(
        private val tableName: String,
        private val sourceEntity: String,
        private val sourceField: String,
        private val datasetName: String,
        private val datastore: Datastore,
        private val bigquery: BigQuery) {

    private val logger = LoggerFactory.getLogger(DS2BQExporter::class.java)
    private val randomKey = UUID.randomUUID().toString();
    private val digest = MessageDigest.getInstance("SHA-256")

    private val idCache: Cache<String, String> = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .build()
    private var error: String = ""

    private fun createTable(): Table {
        // Delete existing table
        val deleted = bigquery.delete(datasetName, tableName)
        if (deleted) {
            logger.info("Existing table '$tableName' deleted.")
        }
        val tableId = TableId.of(datasetName, tableName)
        // Table field definition
        val id = Field.of(idFieldName, LegacySQLTypeName.STRING)
        val pseudonym = Field.of(pseudonymPropertyName, LegacySQLTypeName.STRING)
        val source = Field.of(sourceField, LegacySQLTypeName.STRING)
        // Table schema definition
        val schema = Schema.of(id, pseudonym, source)
        val tableDefinition = StandardTableDefinition.of(schema)
        val tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
        return bigquery.create(tableInfo)
    }

    private fun getIdForKey(key: String): String {
        // Retrieves the element from cache.
        // Incase of cache miss, generate a new SHA
        return idCache.get(key) {
            val keyString: String = "$randomKey-$key"
            val hash = digest.digest(keyString.toByteArray(Charsets.UTF_8))
            String(Hex.encodeHex(hash))
        }
    }

    private fun exportPage(pageSize: Int, cursor: Cursor?, table: Table): Cursor? {
        // Dump pseudonyms to BQ, one page at a time. Since all records in a
        // page are inserted at once, use a small page size
        val queryBuilder = Query.newEntityQueryBuilder()
                .setKind(sourceEntity)
                .setOrderBy(StructuredQuery.OrderBy.asc(sourceField))
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
                    sourceField to entity.getString(sourceField),
                    pseudonymPropertyName to entity.getString(pseudonymPropertyName),
                    idFieldName to getIdForKey(entity.getString(sourceField)))
            val rowId = "rowId$totalPseudonyms"
            rows.add(RowToInsert.of(rowId, row))
        }
        if (totalPseudonyms != 0) {
            val response = table.insert(rows, true, true)
            if (response.hasErrors()) {
                logger.error("Failed to insert Records to '$tableName'", response.insertErrors)
                error = "$error${response.insertErrors}\n"
            }
        }
        return if (totalPseudonyms < pageSize) {
            null
        } else {
            pseudonyms.cursorAfter
        }
    }
    /**
     * Export the Datastore table to BQ.
     * his is done in pages of 100 records.
     */
    fun doExport() {
        val table = createTable()
        var cursor: Cursor? = null
        do {
            cursor = exportPage(100, cursor, table)
        } while (cursor != null)
    }

}