package org.ostelco.simcards.inventory

import arrow.core.Either
import arrow.core.fix
import arrow.core.flatMap
import arrow.effects.IO
import arrow.instances.either.monad.monad
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.StoreError
import org.ostelco.simcards.adapter.HlrAdapter
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.sql.ResultSet
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong


enum class HlrState {
    NOT_ACTIVATED,
    ACTIVATED,
}

/* ES2+ interface description - GSMA states forward transition. */
enum class SmDpPlusState {
    /* ES2+ protocol - between SM-DP+ servcie and backend. */
    AVAILABLE,
    ALLOCATED,
    CONFIRMED,         /* Not used as 'releaseFlag' is set to true in 'confirm-order' message. */
    RELEASED,
    /* ES9+ protocol - between SM-DP+ service and handset. */
    DOWNLOADED,
    INSTALLED,
    ENABLED,
}

enum class ProvisionState {
    AVAILABLE,
    PROVISIONED,       /* The SIM profile has been taken into use (by a subscriber). */
    RESERVED           /* Reserved SIM profile (f.ex. used for testing). */
}


/**
 *  Representing a single SIM card.
 */
data class SimEntry(
        @JsonProperty("id") val id: Long? = null,
        @JsonProperty("batch") val batch: Long,
        @JsonProperty("hlrId") val hlrId: Long,
        @JsonProperty("profileVendorId") val profileVendorId: Long,
        @JsonProperty("msisdn") val msisdn: String,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("imsi") val imsi: String,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("profile") val profile: String,
        @JsonProperty("hlrState") val hlrState: HlrState = HlrState.NOT_ACTIVATED,
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
        @JsonProperty("hlrId") val hlrId: Long,
        @JsonProperty("profileVendorId") val profileVendorId: Long
)


class SimEntryIterator(profileVendorId: Long,
                       hlrId: Long,
                       batchId: Long,
                       csvInputStream: InputStream): Iterator<SimEntry> {

    var count = AtomicLong(0)
    // TODO: The current implementation puts everything in a deque at startup.
    //     This is correct, but inefficient, in partricular for large
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
        //     a config file or other  config database.

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
                            hlrId = hlrId,
                            profileVendorId = profileVendorId,
                            iccid = iccid,
                            imsi = imsi,
                            msisdn = msisdn,
                            pin1 = pin1,
                            puk1 = puk1,
                            puk2 = puk2,
                            pin2 = pin2,
                            profile = profile
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
class SimInventoryDAO(val db: SimInventoryDBWrapperImpl) : SimInventoryDBWrapper by db {

    /**
     * Check if the  SIM vendor can be use for handling SIMs handled
     * by the given HLR.
     * @param profileVendorId  SIM profile vendor to check
     * @param hlrId  HLR to check
     * @return true if permitted false otherwise
     */
    fun simVendorIsPermittedForHlr(profileVendorId: Long,
                                   hlrId: Long): Either<StoreError, Boolean> =
            findSimVendorForHlrPermissions(profileVendorId, hlrId)
                    .flatMap {
                        Either.right(it.isNotEmpty())
                    }

    /**
     * Set permission for a SIM profile vendor to activate SIM profiles
     * with a specific HLR.
     * @param profileVendor  name of SIM profile vendor
     * @param hlr  name of HLR
     * @return true on successful update
     */
    @Transaction
    fun permitVendorForHlrByNames(profileVendor: String, hlr: String): Either<StoreError, Boolean> =
            IO {
                Either.monad<StoreError>().binding {
                    val profileVendorAdapter = getProfileVendorAdapterByName(profileVendor)
                            .bind()
                    val hlrAdapter = getHlrAdapterByName(hlr)
                            .bind()

                    storeSimVendorForHlrPermission(profileVendorAdapter.id, hlrAdapter.id)
                            .bind() > 0
                }.fix()
            }.unsafeRunSync()

    //
    // Importing
    //

    override fun insertAll(entries: Iterator<SimEntry>): Either<StoreError, Unit> =
        db.insertAll(entries)

    @Transaction
    fun importSims(importer: String,
                   hlrId: Long,
                   profileVendorId: Long,
                   csvInputStream: InputStream): Either<StoreError, SimImportBatch> =
            IO {
                Either.monad<StoreError>().binding {
                    createNewSimImportBatch(importer = importer,
                            hlrId = hlrId,
                            profileVendorId = profileVendorId)
                            .bind()
                    val batchId = lastInsertedRowId()
                            .bind()
                    val values = SimEntryIterator(profileVendorId = profileVendorId,
                            hlrId = hlrId,
                            batchId = batchId,
                            csvInputStream = csvInputStream)
                    insertAll(values)
                            .bind()
                    updateBatchState(id = batchId,
                            size = values.count.get(),
                            status = "SUCCESS",
                            endedAt = System.currentTimeMillis())
                            .bind()
                    getBatchInfo(batchId)
                            .bind()
                }.fix()
            }.unsafeRunSync()

    //
    // Setting activation statuses
    //

    /**
     * Set the entity to be marked as "active" in the HLR, then return the
     * SIM entry.
     * @param id row to update
     * @param state new state from HLR service interaction
     * @return updated row or null on no match
     */
    @Transaction
    fun setHlrState(id: Long, state: HlrState): Either<StoreError, SimEntry> =
            updateHlrState(id, state)
                    .flatMap { count ->
                        if (count > 0)
                         getSimProfileById(id)
                        else
                            Either.left(NotFoundError("", "Found no HLR adapter with id ${id} update of HLR state failed"))
                    }

    /**
     * Set the provision state of a SIM entry, then return the entry.
     * @param id row to update
     * @param state new state from HLR service interaction
     * @return updated row or null on no match
     */
    @Transaction
    fun setProvisionState(id: Long, state: ProvisionState): Either<StoreError, SimEntry> =
            updateProvisionState(id, state)
                    .flatMap { count ->
                        if (count > 0)
                            getSimProfileById(id)
                        else
                            Either.left(NotFoundError(
                                    "", "Found no SIM profile with id ${id} update of provision state failed"))
                    }

    /**
     * Set the entity to be marked as "active" in the HLR and the provision
     * state, then return the SIM entry.
     * @param id row to update
     * @param hlrState new state from HLR service interaction
     * @param provisionState new provision state
     * @return updated row or null on no match
     */
    @Transaction
    fun setHlrStateAndProvisionState(id: Long, hlrState: HlrState, provisionState: ProvisionState): Either<StoreError, SimEntry> =
            updateHlrStateAndProvisionState(id, hlrState, provisionState)
                    .flatMap { count ->
                        if (count > 0)
                            getSimProfileById(id)
                        else
                            Either.left(NotFoundError(
                                    "","Found no SIM profile with id ${id} update of HLR and provision state failed"))
                    }


    /**
     * Updates state of SIM profile and returns the updated profile.
     * @param id  row to update
     * @param state  new state from SMDP+ service interaction
     * @return updated row or null on no match
     */
    @Transaction
    fun setSmDpPlusState(id: Long, state: SmDpPlusState): Either<StoreError, SimEntry> =
            updateSmDpPlusState(id, state)
                    .flatMap { count ->
                        if (count > 0)
                            getSimProfileById(id)
                        else
                            Either.left(NotFoundError(
                                    "", "Found no SIM profile with id ${id} update of SM-DP+ state failed"))
                    }

    /**
     * Updates state of SIM profile and returns the updated profile.
     * Updates state and the 'matching-id' of a SIM profile and return
     * the updated profile.
     * @param id  row to update
     * @param state  new state from SMDP+ service interaction
     * @param matchingId  SM-DP+ ES2 'matching-id' to be sent to handset
     * @return updated row or null on no match
     */
    @Transaction
    fun setSmDpPlusStateAndMatchingId(id: Long, state: SmDpPlusState, matchingId: String): Either<StoreError, SimEntry> =
            updateSmDpPlusStateAndMatchingId(id, state, matchingId)
                    .flatMap {  count ->
                        if (count > 0)
                            getSimProfileById(id)
                        else
                            Either.left(NotFoundError(
                                    "", "Found no SIM profile with id ${id} update of SM-DP+ state and 'matching-id' failed"))
                    }

    //
    // Finding next free SIM card for a particular HLR.
    //

    /**
     * Sets the EID value of a SIM entry (profile).
     * @param id  SIM entry to update
     * @param eid  the eid value
     * @return updated SIM entry
     */
    @Transaction
    fun setEidOfSimProfile(id: Long, eid: String): Either<StoreError, SimEntry> =
            updateEidOfSimProfile(id, eid)
                    .flatMap {  count ->
                        if (count > 0)
                            getSimProfileById(id)
                        else
                            Either.left(NotFoundError(
                                    "", "Found no SIM profile with id ${id} update of EID failed"))
                    }

    /**
     * Get relevant statistics for a particular profile type for a particular HLR.
     */
    fun getProfileStats(@Bind("hlrId") hlrId: Long,
                        @Bind("simProfile") simProfile: String): Either<StoreError, SimProfileKeyStatistics> =
            IO {
                Either.monad<StoreError>().binding {

                    val keyValuePairs = mutableMapOf<String, Long>()

                    getProfileStatsAsKeyValuePairs(hlrId = hlrId, simProfile = simProfile)
                            .bind()
                            .forEach { keyValuePairs.put(it.key, it.value) }

                    val noOfEntries = keyValuePairs.get("NO_OF_ENTRIES")!!
                    val noOfUnallocatedEntries = keyValuePairs.get("NO_OF_UNALLOCATED_ENTRIES")!!
                    val noOfReleasedEntries = keyValuePairs.get("NO_OF_RELEASED_ENTRIES")!!
                    val noOfEntriesAvailableForImmediateUse = keyValuePairs.get("NO_OF_ENTRIES_READY_FOR_IMMEDIATE_USE")!!

                    SimProfileKeyStatistics(
                            noOfEntries = noOfEntries,
                            noOfUnallocatedEntries = noOfUnallocatedEntries,
                            noOfEntriesAvailableForImmediateUse = noOfEntriesAvailableForImmediateUse,
                            noOfReleasedEntries = noOfReleasedEntries)
                }.fix()
            }.unsafeRunSync()
}


class SimProfileKeyStatistics(
        val noOfEntries: Long,
        val noOfUnallocatedEntries: Long,
        val noOfReleasedEntries: Long,
        val noOfEntriesAvailableForImmediateUse: Long)


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

class HlrAdapterMapper  : RowMapper<HlrAdapter> {
    override fun map(row: ResultSet, ctx: StatementContext): HlrAdapter? {
        if (row.isAfterLast) {
            return null
        }

        val id = row.getLong("id")
        val name = row.getString("name")
        return HlrAdapter(id = id, name = name)
    }
}
