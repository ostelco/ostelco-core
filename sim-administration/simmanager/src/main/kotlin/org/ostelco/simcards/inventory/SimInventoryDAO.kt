package org.ostelco.simcards.inventory

import arrow.core.Either
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.effects.IO
import arrow.instances.either.monad.monad
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.hss.HssEntry
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.sql.ResultSet
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong


enum class HssState {
    NOT_ACTIVATED,
    ACTIVATED,
}

/* ES2+ interface description - GSMA states forward transition. */
enum class SmDpPlusState {
    /* ES2+ protocol - between SM-DP+ service and backend. */
    AVAILABLE,
    ALLOCATED,
    CONFIRMED,         /* Not used as 'releaseFlag' is set to true in 'confirm-order' message. */
    RELEASED,
    /* ES9+ protocol - between SM-DP+ service and handset. */
    DOWNLOADED,
    INSTALLED,
    ENABLED,
    DELETED
}

enum class ProvisionState {
    AVAILABLE,
    PROVISIONED,       /* The SIM profile has been taken into use (by a subscriber). */
    RESERVED,           /* Reserved SIM profile (f.ex. used for testing). */
    ALLOCATION_FAILED
}


/**
 *  Representing a single SIM card.
 */
data class SimEntry(
        @JsonProperty("id") val id: Long? = null,
        @JsonProperty("batch") val batch: Long,
        @ColumnName("hlrId") @JsonProperty("hssId") val hssId: Long,
        @JsonProperty("profileVendorId") val profileVendorId: Long,
        @JsonProperty("msisdn") val msisdn: String,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("imsi") val imsi: String,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("profile") val profile: String,
        @ColumnName("hlrState") @JsonProperty("hssState") val hssState: HssState = HssState.NOT_ACTIVATED,
        @JsonProperty("smdpPlusState") val smdpPlusState: SmDpPlusState = SmDpPlusState.AVAILABLE,
        @JsonProperty("provisionState") val provisionState: ProvisionState = ProvisionState.AVAILABLE,
        @JsonProperty("matchingId") val matchingId: String? = null,
        @JsonProperty("pin1") val pin1: String? = null,
        @JsonProperty("pin2") val pin2: String? = null,
        @JsonProperty("puk1") val puk1: String? = null,
        @JsonProperty("puk2") val puk2: String? = null,
        @JsonProperty("code") val code: String? = null
)

/**
 * Describe a batch of SIM cards that was imported at some time
 */
data class SimImportBatch(
        @JsonProperty("id") val id: Long,
        @JsonProperty("endedAt") val endedAt: Long,
        @JsonProperty("message") val status: String?,
        @JsonProperty("importer") val importer: String,
        @JsonProperty("size") val size: Long,
        @ColumnName("hlrId") @JsonProperty("hssId") val hssId: Long,
        @JsonProperty("profileVendorId") val profileVendorId: Long
)


class SimEntryIterator(profileVendorId: Long,
                       hssId: Long,
                       batchId: Long,
                       initialHssState: HssState,
                       csvInputStream: InputStream) : Iterator<SimEntry> {

    var count = AtomicLong(0)
    // TODO: The current implementation puts everything in a deque at startup.
    //     This is correct, but inefficient, in particular for large
    //     batches.   Once proven to work, this thing should be rewritten
    //     to use coroutines, to let the "next" get the next available
    //     sim entry.  It may make sense to have a reader and writer thread
    //     coordinating via the deque.
    private val values = ConcurrentLinkedDeque<SimEntry>()

    init {
        // XXX Adjust to fit whatever format we should cater to, there may
        //     be some variation between  sim vendors, and that should be
        //     something we can adjust to given the parameters sent to the
        //     reader class on creation.   Should  be configurable in
        //     a config file or other config database.

        val csvFileFormat = CSVFormat.DEFAULT
                .withQuote(null)
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines(true)
                .withTrim()
                .withIgnoreSurroundingSpaces()
                .withNullString("")
                .withDelimiter(',')

        BufferedReader(InputStreamReader(csvInputStream, Charset.forName(
                "ISO-8859-1"))).use { reader ->
            CSVParser(reader, csvFileFormat).use { csvParser ->
                for (row in csvParser) {
                    val iccid = row.get("ICCID")
                    val imsi = row.get("IMSI")
                    val msisdn = row.get("MSISDN")
                    val pin1 = row?.get("PIN1")
                    val pin2 = row?.get("PIN2")
                    val puk1 = row?.get("PUK1")
                    val puk2 = row?.get("PUK2")
                    val profile = row.get("PROFILE")

                    val value = SimEntry(
                            batch = batchId,
                            hssId = hssId,
                            profileVendorId = profileVendorId,
                            iccid = iccid,
                            imsi = imsi,
                            msisdn = msisdn,
                            pin1 = pin1,
                            puk1 = puk1,
                            puk2 = puk2,
                            pin2 = pin2,
                            profile = profile,
                            hssState =  initialHssState
                    )

                    values.add(value)
                    count.incrementAndGet()
                }
            }
        }
    }

    /**
     * Returns the next element in the iteration.
     */
    override operator fun next(): SimEntry {
        return values.removeLast()
    }

    /**
     * Returns `true` if the iteration has more elements.
     */
    override operator fun hasNext(): Boolean {
        return !values.isEmpty()
    }
}

/**
 * SIM DB DAO.
 */
class SimInventoryDAO(private val db: SimInventoryDBWrapperImpl) : SimInventoryDBWrapper by db {

    /**
     * Check if the  SIM vendor can be use for handling SIMs handled
     * by the given HLR.
     * @param profileVendorId  SIM profile vendor to check
     * @param hssId  HLR to check
     * @return true if permitted false otherwise
     */
    fun simVendorIsPermittedForHlr(profileVendorId: Long,
                                   hssId: Long): Either<SimManagerError, Boolean> =
            findSimVendorForHssPermissions(profileVendorId, hssId)
                    .flatMap {
                        Either.right(it.isNotEmpty())
                    }

    /**
     * Set permission for a SIM profile vendor to activate SIM profiles
     * with a specific HLR.
     * @param profileVendor  metricName of SIM profile vendor
     * @param hssName  metricName of HLR
     * @return true on successful update
     */
    @Transaction
    fun permitVendorForHssByNames(profileVendor: String, hssName: String): Either<SimManagerError, Boolean> =
            IO {
                Either.monad<SimManagerError>().binding {
                    val profileVendorAdapter = getProfileVendorAdapterDatumByName(profileVendor)
                            .bind()
                    val hlrAdapter = getHssEntryByName(hssName)
                            .bind()

                    storeSimVendorForHssPermission(profileVendorAdapter.id, hlrAdapter.id)
                            .bind() > 0
                }.fix()
            }.unsafeRunSync()

    //
    // Importing
    //

    override fun insertAll(entries: Iterator<SimEntry>): Either<SimManagerError, Unit> =
            db.insertAll(entries.iterator())

    override fun reserveGoldenNumbersForBatch(batchId: Long): Either<SimManagerError, Int> =
            db.reserveGoldenNumbersForBatch(batchId)

    @Transaction
    fun importSims(importer: String,
                   hlrId: Long,
                   profileVendorId: Long,
                   csvInputStream: InputStream,
                   initialHssState: HssState = HssState.NOT_ACTIVATED): Either<SimManagerError, SimImportBatch> =
            IO {
                Either.monad<SimManagerError>().binding {
                    createNewSimImportBatch(importer = importer,
                            hssId = hlrId,
                            profileVendorId = profileVendorId)
                            .bind()
                    val batchId = lastInsertedRowId()
                            .bind()
                    val values = SimEntryIterator(
                            profileVendorId = profileVendorId,
                            hssId = hlrId,
                            batchId = batchId,
                            initialHssState = initialHssState,
                            csvInputStream = csvInputStream)
                    insertAll(values)
                            .bind()
                    // Because "golden numbers" needs special handling, so we're simply marking them
                    // as reserved.
                    reserveGoldenNumbersForBatch(batchId)
                    updateBatchState(id = batchId,
                            size = values.count.get(),
                            status = "SUCCESS",  // TODO: Use enumeration, not naked string.
                            endedAt = System.currentTimeMillis())
                            .bind()
                    getBatchInfo(batchId)
                            .bind()
                }.fix()
            }.unsafeRunSync()

    //
    // Finding next free SIM card for a particular HLR.
    //

    /**
     * Get relevant statistics for a particular profile type for a particular HLR.
     */
    fun getProfileStats(@Bind("hssId") hssId: Long,
                        @Bind("simProfile") simProfile: String):
            Either<SimManagerError, SimProfileKeyStatistics> =
            IO {
                Either.monad<SimManagerError>().binding {

                    val keyValuePairs = mutableMapOf<String, Long>()

                    getProfileStatsAsKeyValuePairs(hssId = hssId, simProfile = simProfile).bind()
                            .forEach { keyValuePairs.put(it.key, it.value) }

                    fun lookup(key: String) = keyValuePairs[key]
                            ?.right()
                            ?: NotFoundError("Could not find key $key").left()

                    val noOfEntries =
                            lookup("NO_OF_ENTRIES").bind()
                    val noOfUnallocatedEntries =
                            lookup( "NO_OF_UNALLOCATED_ENTRIES").bind()
                    val noOfReleasedEntries =
                            lookup("NO_OF_RELEASED_ENTRIES").bind()
                    val noOfEntriesAvailableForImmediateUse =
                            lookup("NO_OF_ENTRIES_READY_FOR_IMMEDIATE_USE").bind()
                    val noOfReservedEntries =
                            lookup("NO_OF_RESERVED_ENTRIES").bind()

                    SimProfileKeyStatistics(
                            noOfEntries = noOfEntries,
                            noOfUnallocatedEntries = noOfUnallocatedEntries,
                            noOfEntriesAvailableForImmediateUse = noOfEntriesAvailableForImmediateUse,
                            noOfReleasedEntries = noOfReleasedEntries,
                            noOfReservedEntries = noOfReservedEntries)
                }.fix()
            }.unsafeRunSync()
}


data class SimProfileKeyStatistics(
        val noOfEntries: Long,
        val noOfUnallocatedEntries: Long,
        val noOfReleasedEntries: Long,
        val noOfEntriesAvailableForImmediateUse: Long,
        val noOfReservedEntries: Long)


class KeyValueMapper : RowMapper<KeyValuePair> {

    override fun map(row: ResultSet, ctx: StatementContext): KeyValuePair? {
        if (row.isAfterLast) {
            return null
        }

        val value = row.getLong("VALUE")
        val key = row.getString("KEY")
        return KeyValuePair(key = key, value = value)
    }
}

data class KeyValuePair(val key: String, val value: Long)

class HlrEntryMapper : RowMapper<HssEntry> {
    override fun map(row: ResultSet, ctx: StatementContext): HssEntry? {
        if (row.isAfterLast) {
            return null
        }

        val id = row.getLong("id")
        val name = row.getString("name")
        return HssEntry(id = id, name = name)
    }
}
