package org.ostelco.simcards.inventory

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter
import org.jdbi.v3.core.mapper.RowMapper
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response


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
        @JsonProperty("pin1") val pin1: String,
        @JsonProperty("pin2") val pin2: String,
        @JsonProperty("puk1") val puk1: String,
        @JsonProperty("puk2") val puk2: String
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
                .withDelimiter(',')

        BufferedReader(InputStreamReader(csvInputStream, Charset.forName(
                "ISO-8859-1"))).use { reader ->
            CSVParser(reader, csvFileFormat).use { csvParser ->
                for (record in csvParser) {
                    val iccid = record.get("ICCID")
                    val imsi = record.get("IMSI")
                    val msisdn = record.get("MSISDN")
                    val pin1 = record.get("PIN1")
                    val pin2 = record.get("PIN2")
                    val puk1 = record.get("PUK1")
                    val puk2 = record.get("PUK2")
                    val profile = record.get("PROFILE")

                    val value = SimEntry(
                            batch = batchId,
                            profileVendorId = profileVendorId,
                            profile = profile,
                            hlrId = hlrId,
                            iccid = iccid,
                            imsi = imsi,
                            msisdn = msisdn,
                            pin1 = pin1,
                            puk1 = puk1,
                            puk2 = puk2,
                            pin2 = pin2
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


class SimInventoryDAO(val db: SimInventoryDB) : SimInventoryDB by db {

    /**
     * Check if the  SIM vendor can be use for handling SIMs handled
     * by the given HLR.
     * @param profileVendorId  SIM profile vendor to check
     * @param hlrId  HLR to check
     * @return true if permitted false otherwise
     */
    fun simVendorIsPermittedForHlr(profileVendorId: Long,
                                   hlrId: Long): Boolean {
        return findSimVendorForHlrPermissions(profileVendorId, hlrId)
                .isNotEmpty()
    }

    /**
     * Set permission for a SIM profile vendor to activate SIM profiles
     * with a specific HLR.
     * @param profileVendor  name of SIM profile vendor
     * @param hlr  name of HLR
     * @return true on successful update
     */
    @Transaction
    fun permitVendorForHlrByNames(profileVendor: String, hlr: String): Boolean {
        val profileVendorAdapter = assertNonNull(getProfileVendorAdapterByName(profileVendor))
        val hlrAdapter = assertNonNull(getHlrAdapterByName(hlr))
        return storeSimVendorForHlrPermission(profileVendorAdapter.id, hlrAdapter.id) > 0
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
            hlrId: Long,
            profileVendorId: Long,
            csvInputStream: InputStream): SimImportBatch {

        createNewSimImportBatch(
                importer = importer,
                hlrId = hlrId,
                profileVendorId = profileVendorId)
        val batchId = lastInsertedRowId()
        val values = SimEntryIterator(
                profileVendorId = profileVendorId,
                hlrId = hlrId,
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
    fun setHlrState(id: Long, state: HlrState): SimEntry? {
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
     * @param hlrState new state from HLR service interaction
     * @param provisionState new provision state
     * @return updated row or null on no match
     */
    @Transaction
    fun setHlrStateAndProvisionState(id: Long, hlrState: HlrState, provisionState: ProvisionState): SimEntry? {
        return if (updateHlrStateAndProvisionState(id, hlrState, provisionState) > 0)
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