package org.ostelco.simcards.inventory

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.*
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper
import org.skife.jdbi.v2.tweak.ResultSetMapper
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

enum class SmDpPlusState {
    NOT_ACTIVATED,
    ORDER_DOWNLOADED,
    ACTIVATED,         /* I.e. previously downloaded order is confirmed. */
}

/**
 *  Representing a single SIM card.
 */
data class SimEntry(
        @JsonProperty("id") val id: Long? = null,
        @JsonProperty("batch") val batch: Long,
        @JsonProperty("hlrId") val hlrId: Long,
        @JsonProperty("profileVendorId") val profileVendorId: Long,
        @JsonProperty("msisdn") val msisdn: String? = null,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("imsi") val imsi: String,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("hlrState") val hlrState: HlrState = HlrState.NOT_ACTIVATED,
        @JsonProperty("smdpPlusState") val smdpPlusState: SmDpPlusState = SmDpPlusState.NOT_ACTIVATED,
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

class SimEntryIterator(profileVendorId: Long, hlrId: Long, batchId: Long, csvInputStream: InputStream) : Iterator<SimEntry> {

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
                    val pin1 = record.get("PIN1")
                    val pin2 = record.get("PIN2")
                    val puk1 = record.get("PUK1")
                    val puk2 = record.get("PUK2")

                    val value = SimEntry(
                            batch = batchId,
                            profileVendorId = profileVendorId,
                            hlrId = hlrId,
                            iccid = iccid,
                            imsi = imsi,
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


/**
 * The DAO we're using to access the SIM inventory, and also the
 * pieces of SM-DP+/HLR infrastucture the SIM management needs to
 * be aware of.
 */
abstract class SimInventoryDAO {

    @SqlQuery("SELECT * FROM sim_entries WHERE id = :id")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun getSimProfileById(@Bind("id") id: Long): SimEntry

    @SqlQuery("SELECT * FROM sim_entries WHERE iccid = :iccid")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun getSimProfileByIccid(@Bind("iccid") iccid: String): SimEntry

    @SqlQuery("SELECT * FROM sim_entries WHERE imsi = :imsi")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun getSimProfileByImsi(@Bind("imsi") imsi: String): SimEntry

    @SqlQuery("SELECT * FROM sim_entries WHERE msisdn = :msisdn")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun getSimProfileByMsisdn(@Bind("msisdn") msisdn: String): SimEntry


    class SimEntryMapper : ResultSetMapper<SimEntry> {

        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): SimEntry? {
            if (r.isAfterLast) {
                return null
            }

            val id = r.getLong("id")
            val batch = r.getLong("batch")
            val profileVendorId = r.getLong("profileVendorId")
            val hlrId = r.getLong("hlrId")
            val msisdn = r.getString("msisdn")
            val iccid = r.getString("iccid")
            val imsi = r.getString("imsi")
            val eid = r.getString("eid")
            val smdpPlusState = r.getString("smdpPlusState")
            val hlrState = r.getString("hlrState")
            val pin1 = r.getString("pin1")
            val pin2 = r.getString("pin2")
            val puk1 = r.getString("puk1")
            val puk2 = r.getString("puk2")

            return SimEntry(
                    id = id,
                    batch = batch,
                    profileVendorId = profileVendorId,
                    hlrId = hlrId,
                    msisdn = msisdn,
                    iccid = iccid,
                    imsi = imsi,
                    eid = eid,
                    smdpPlusState = SmDpPlusState.valueOf(smdpPlusState.toUpperCase()),
                    hlrState = HlrState.valueOf(hlrState.toUpperCase()),
                    pin1 = pin1,
                    pin2 = pin2,
                    puk1 = puk1,
                    puk2 = puk2
            )
        }
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

    @Transaction
    fun permitVendorForHlrByNames(profileVendor: String, hlr: String) {
        val profileVendorAdapter = assertNonNull(getProfileVendorAdapterByName(profileVendor))
        val hlrAdapter = assertNonNull(getHlrAdapterByName(hlr))
        storeSimVendorForHlrPermission(profileVendorAdapter.id, hlrAdapter.id)
    }

    @SqlQuery("""SELECT id FROM sim_vendors_permitted_hlrs
                      WHERE profileVendorId = profileVendorId AND hlrId = :hlrId""")
    abstract fun findSimVendorForHlrPermissions(@Bind("profileVendorId") profileVendorId: Long, @Bind("hlrId") hlrId: Long): List<Long>

    /**
     * Check if the  SIM vendor can be use for handling SIMs handled
     * by the given HLR.
     */
    fun simVendorIsPermittedForHlr(@Bind("profileVendorId") profileVendorId: Long, @Bind("hlrId") hlrId: Long): Boolean {
        return (findSimVendorForHlrPermissions(profileVendorId, hlrId).isNotEmpty())
    }


    @SqlUpdate("""INSERT INTO sim_vendors_permitted_hlrs
                                   (profilevendorid,
                                    hlrid)
                       SELECT :profileVendorId,
                              :hlrId
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   sim_vendors_permitted_hlrs
                                          WHERE  profilevendorid = :profileVendorId
                                           AND hlrid = :hlrId)""")
    abstract fun storeSimVendorForHlrPermission(@Bind("profileVendorId") profileVendorId: Long,
                                                @Bind("hlrId") hlrId: Long)

    @SqlUpdate("""INSERT INTO hlr_adapters
                                   (NAME)
                       SELECT :name
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   hlr_adapters
                                          WHERE  NAME = :name)""")
    abstract fun addHlrAdapter(@Bind("name") name: String)

    @SqlQuery("SELECT * FROM hlr_adapters WHERE name = :name")
    @RegisterMapper(HlrAdapterMapper::class)
    abstract fun getHlrAdapterByName(@Bind("name") name: String): HlrAdapter

    @SqlQuery("SELECT * FROM hlr_adapters WHERE id = :id")
    @RegisterMapper(HlrAdapterMapper::class)
    abstract fun getHlrAdapterById(@Bind("id") id: Long): HlrAdapter

    class HlrAdapterMapper : ResultSetMapper<HlrAdapter> {

        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): HlrAdapter? {
            if (r.isAfterLast) {
                return null
            }

            val id = r.getLong("id")
            val name = r.getString("name")

            return HlrAdapter(id = id, name = name)
        }
    }

    @SqlUpdate("""INSERT INTO profile_vendor_adapters
                                   (NAME)
                       SELECT :name
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   profile_vendor_adapters
                                          WHERE  NAME = :name) """)
    abstract fun addProfileVendorAdapter(@Bind("name") name: String)

    @SqlQuery("SELECT * FROM profile_vendor_adapters WHERE name = :name")
    @RegisterMapper(ProfileVendorAdapterMapper::class)
    abstract fun getProfileVendorAdapterByName(@Bind("name") name: String): ProfileVendorAdapter

    @SqlQuery("SELECT * FROM profile_vendor_adapters WHERE id = :id")
    @RegisterMapper(ProfileVendorAdapterMapper::class)
    abstract fun getProfileVendorAdapterById(@Bind("id") id: Long): ProfileVendorAdapter

    class ProfileVendorAdapterMapper : ResultSetMapper<ProfileVendorAdapter> {

        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): ProfileVendorAdapter? {
            if (r.isAfterLast) {
                return null
            }

            val id = r.getLong("id")
            val name = r.getString("name")

            return ProfileVendorAdapter(id = id, name = name)
        }
    }


    //
    // Importing
    //

    @Transaction
    @SqlBatch("""INSERT INTO sim_entries
                                  (batch, profileVendorId, hlrid, hlrState, smdpplusstate, iccid, imsi, pin1, pin2, puk1, puk2)
                      VALUES (:batch, :profileVendorId, :hlrId, :hlrState, :smdpPlusState, :iccid, :imsi, :pin1, :pin2, :puk1, :puk2)""")
    @BatchChunkSize(1000)
    abstract fun insertAll(@BindBean entries: Iterator<SimEntry>)

    @SqlUpdate("""INSERT INTO sim_import_batches (status,  importer, hlrId, profileVendorId)
                       VALUES ('STARTED', :importer, :hlrId, :profileVendorId)""")
    abstract fun createNewSimImportBatch(
            @Bind("importer") importer: String,
            @Bind("hlrId") hlrId: Long,
            @Bind("profileVendorId") profileVendorId: Long)

    @SqlUpdate("""UPDATE sim_import_batches SET size = :size, status = :status, endedAt = :endedAt
                       WHERE id = :id""")
    abstract fun updateBatchState(
            @Bind("id") id: Long,
            @Bind("size") size: Long,
            @Bind("status") status: String,
            @Bind("endedAt") endedAt: Long)

    /* Getting the ID of the last insert, regardless of table.
     *
     * XXX: For postgresql replace with;
     *      SELECT lastval()
     */
    @SqlQuery("SELECT lastval()")
    abstract fun lastInsertedRowId(): Long

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

    @SqlQuery("""SELECT * FROM sim_import_batches
                      WHERE id = :id""")
    @RegisterMapper(SimImportBatchMapper::class)
    abstract fun getBatchInfo(@Bind("id") id: Long): SimImportBatch

    class SimImportBatchMapper : ResultSetMapper<SimImportBatch> {

        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): SimImportBatch? {
            if (r.isAfterLast) {
                return null
            }

            val id = r.getLong("id")
            val endedAt = r.getLong("endedAt")
            val status = r.getString("status")
            val profileVendorId = r.getLong("profileVendorId")
            val hlrId = r.getLong("hlrId")
            val size = r.getLong("size")

            return SimImportBatch(
                    id = id,
                    endedAt = endedAt,
                    status = status,
                    profileVendorId = profileVendorId,
                    hlrId = hlrId,
                    size = size,
                    importer = "XXX Replace with name of agent that facilitated the import")
        }
    }


    //
    // Setting activation statuses
    //

    @SqlUpdate("""UPDATE sim_entries SET hlrState = :hlrState
                       WHERE id = :id""")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun updateHlrState(
            @Bind("id") id: Long,
            @Bind("hlrState") hlrState: HlrState)

    /**
     * Set the entity to be marked as "active" in the HLR, then return the
     * SIM entry.
     *
     * XXX With postgresql this method can be removed provided that 'updateHlrState()'
     *     is updated to: "UPDATE ... :hlrState WHERE id = :id RETURNING *". Not
     *     sure if a 'list' or an object will be returned in cases where only one row is
     *     updated?
     */
    fun setHlrState(id: Long, state: HlrState): SimEntry? {
        updateHlrState(id, state)
        return getSimProfileById(id)
    }

    @SqlUpdate("""UPDATE sim_entries SET smdpPlusState = :smdpPlusState
                       WHERE id = :id""")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun updateSmDpPlusState(
            @Bind("id") id: Long,
            @Bind("smdpPlusState") smdpPlusState: SmDpPlusState)

    /**
     * Updates state of SIM profile and returns the updated profile.
     *
     * XXX With postgresql this method can be removed provided that 'updateSmDpPlusSate()'
     *     is updated to: "UPDATE ... :smdpPlusState WHERE id = :id RETURNING *". Not
     *     sure if a 'list' or an object will be returned in cases where only one row is
     *     updated?
     */
    fun setSmDpPlusState(id: Long, state: SmDpPlusState): SimEntry? {
        updateSmDpPlusState(id, state)
        return getSimProfileById(id)
    }


    //
    //  Binding a SIM card to a MSISDN
    //

    @SqlUpdate("""UPDATE sim_entries SET msisdn = :msisdn
                       WHERE id = :id""")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun updateMsisdnOfSimProfile(@Bind("id") id: Long, @Bind("msisdn") msisdn: String)

    //
    // Finding next free SIM card for a particular HLR.
    //
    @SqlQuery("""SELECT * FROM sim_entries
                      WHERE hlrId = :hlrId AND COALESCE(msisdn, '') = '' AND smdpPlusState = 'NOT_ACTIVATED'
                      LIMIT 1""")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun findNextFreeSimProfileForHlr(@Bind("hlrId") hlrId: Long): SimEntry?

    //
    // Allocating next free simcards in an HLR.
    //
    @Transaction
    fun allocateNextFreeSimProfileForMsisdn(hlrId: Long, msisdn: String): SimEntry? {
        val simEntry = findNextFreeSimProfileForHlr(hlrId)

        /* No SIM cards available. */
        if (simEntry == null) {
            return null
        }

        /* XXX With postgresql 'updateMsisdnOfSimProfile()' can be modified to return the updated
         *     row by updating the SQL expression to:
         *          UPDATE sim_entries SET msisdn = :msisdn WHERE id = :id RETURNING *
         *     removing the need for calling 'getSimProfileByMsisdn()'.
         */
        updateMsisdnOfSimProfile(simEntry.id!!, msisdn)
        return getSimProfileByMsisdn(msisdn)
    }


    @SqlUpdate("UPDATE sim_entries SET eid = :eid WHERE id = :id")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun updateEidOfSimProfile(@Bind("id") id: Long, @Bind("eid") eid: String)

    fun setEidOfSimProfile(id: Long, eid: String): SimEntry? {
        updateEidOfSimProfile(id, eid)
        return getSimProfileById(id)
    }
}
