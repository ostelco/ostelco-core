package org.ostelco.simcards.inventory

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.ostelco.simcards.adapter.HssEntry
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.sql.ResultSet
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response


enum class HssState {
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
                            hssId = hssId,
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
class SimInventoryDAO(val db: SimInventoryDB) : SimInventoryDB by db {

    /**
     * Check if the  SIM vendor can be use for handling SIMs handled
     * by the given HLR.
     * @param profileVendorId  SIM profile vendor to check
     * @param hssId  HLR to check
     * @return true if permitted false otherwise
     */
    fun simVendorIsPermittedForHlr(profileVendorId: Long,
                                   hssId: Long): Boolean {
        return findSimVendorForHlrPermissions(profileVendorId, hssId)
                .isNotEmpty()
    }

    /**
     * Set permission for a SIM profile vendor to activate SIM profiles
     * with a specific HLR.
     * @param profileVendor  name of SIM profile vendor
     * @param hssName  name of HLR
     * @return true on successful update
     */
    @Transaction
    fun permitVendorForHlrByNames(profileVendor: String, hssName: String): Boolean {
        val profileVendorAdapter = assertNonNull(getProfileVendorAdapterByName(profileVendor))
        val hssEntry = assertNonNull(getHssEntryByName(hssName))
        return storeSimVendorForHlrPermission(profileVendorAdapter.id, hssEntry.id) > 0
    }

    //
    // Importing
    //

    override fun insertAll(entries: Iterator<SimEntry>) {
        db.insertAll(entries)
    }

    @Transaction
    fun importSims(
            importer: String,
            hssId: Long,
            profileVendorId: Long,
            csvInputStream: InputStream): SimImportBatch? {

        createNewSimImportBatch(
                importer = importer,
                hssId = hssId,
                profileVendorId = profileVendorId)
        val batchId = lastInsertedRowId()
        val values = SimEntryIterator(
                profileVendorId = profileVendorId,
                hssId = hssId,
                batchId = batchId,
                csvInputStream = csvInputStream)
        insertAll(values)
        updateBatchState(
                id = batchId,
                size = values.count.get(),
                status = "SUCCESS",
                endedAt = System.currentTimeMillis())
        return getBatchInfo(batchId)
    }

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
    fun setHlrState(id: Long, state: HssState): SimEntry? {
        return if (updateHlrState(id, state) > 0)
            getSimProfileById(id)
        else
            null
    }

    /**
     * Set the provision state of a SIM entry, then return the entry.
     * @param id row to update
     * @param state new state from HLR service interaction
     * @return updated row or null on no match
     */
    @Transaction
    fun setProvisionState(id: Long, state: ProvisionState): SimEntry? {
        return if (updateProvisionState(id, state) > 0)
            getSimProfileById(id)
        else
            null
    }

    /**
     * Set the entity to be marked as "active" in the HLR and the provision
     * state, then return the SIM entry.
     * @param id row to update
     * @param hssState new state from HLR service interaction
     * @param provisionState new provision state
     * @return updated row or null on no match
     */
    @Transaction
    fun setHlrStateAndProvisionState(id: Long, hssState: HssState, provisionState: ProvisionState): SimEntry? {
        return if (updateHlrStateAndProvisionState(id, hssState, provisionState) > 0)
            getSimProfileById(id)
        else
            null
    }

    /**
     * Updates state of SIM profile and returns the updated profile.
     * @param id  row to update
     * @param state  new state from SMDP+ service interaction
     * @return updated row or null on no match
     */
    @Transaction
    fun setSmDpPlusState(id: Long, state: SmDpPlusState): SimEntry? {
        return if (updateSmDpPlusState(id, state) > 0)
            getSimProfileById(id)
        else
            null
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
    fun setSmDpPlusStateAndMatchingId(id: Long, state: SmDpPlusState, matchingId: String): SimEntry? {
        return if (updateSmDpPlusStateAndMatchingId(id, state, matchingId) > 0)
            getSimProfileById(id)
        else
            null
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
    fun setEidOfSimProfile(id: Long, eid: String): SimEntry? {
        return if (updateEidOfSimProfile(id, eid) > 0)
            getSimProfileById(id)
        else
            null
    }

    /**
     * Get relevant statistics for a particular profile type for a particular HLR.
     */
    fun getProfileStats(
            @Bind("hssId") hssId: Long,
            @Bind("simProfile") simProfile: String): SimProfileKeyStatistics {

        val keyValuePairs = mutableMapOf<String, Long>()

        getProfileStatsAsKeyValuePairs(hssId = hssId, simProfile = simProfile)
                .forEach { keyValuePairs.put(it.key, it.value) }
        val noOfEntries = keyValuePairs.get("NO_OF_ENTRIES")!!
        val noOfUnallocatedEntries = keyValuePairs.get("NO_OF_UNALLOCATED_ENTRIES")!!
        val noOfReleasedEntries = keyValuePairs.get("NO_OF_RELEASED_ENTRIES")!!
        val noOfEntriesAvailableForImmediateUse = keyValuePairs.get("NO_OF_ENTRIES_READY_FOR_IMMEDIATE_USE")!!

        return SimProfileKeyStatistics(
                noOfEntries = noOfEntries,
                noOfUnallocatedEntries = noOfUnallocatedEntries,
                noOfEntriesAvailableForImmediateUse = noOfEntriesAvailableForImmediateUse,
                noOfReleasedEntries = noOfReleasedEntries)
    }

    companion object {
        private fun <T> assertNonNull(v: T?): T {
            if (v == null) {
                throw WebApplicationException(Response.Status.NOT_FOUND)
            } else {
                return v
            }
        }
    }
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

class HlrEntryMapper  : RowMapper<HssEntry> {
    override fun map(row: ResultSet, ctx: StatementContext): HssEntry? {
        if (row.isAfterLast) {
            return null
        }

        val id = row.getLong("id")
        val name = row.getString("name")
        return HssEntry(id = id, name = name)
    }
}

