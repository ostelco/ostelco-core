package org.ostelco

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
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


/**
 *  Representing a single SIM card.
 */
// @JsonSchema("SimEntry")
data class SimEntry(
        @JsonProperty("id") val id: Long? = null,
        @JsonProperty("batch") val batch: Long,
        @JsonProperty("hlrId") val hlrId: String,
        @JsonProperty("smdpplus") val smdpplus: String? = null,
        @JsonProperty("msisdn") val msisdn: String? = null,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("imsi") val imsi: String,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("hlrActivation") val hlrActivation: Boolean = false,
        @JsonProperty("smdpPlusActivation") val smdpPlusActivation: Boolean = false,
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
        @JsonProperty("hlr") val hlr: String,
        @JsonProperty("profileVendor") val profileVendor: String
)



class SimEntryIterator(hlrId: String, batchId: Long, csvInputStream: InputStream) : Iterator<SimEntry> {

    var count = AtomicLong(0)
    // XXX The current implementation puts everything in a deque at startup.
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
        //     reader class on creation.   Should anyway be configurable in
        //     a config file or perhaps some config database.

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


    @SqlQuery("select * from sim_entries where id = :id")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun getSimProfileById(@Bind("id")id: Long): SimEntry

    @SqlQuery("select * from sim_entries where iccid = :iccid")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun getSimProfileByIccid(@Bind("iccid")iccid: String): SimEntry

    @SqlQuery("select * from sim_entries where imsi = :imsi")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun getSimProfileByImsi(@Bind("imsi")imsi: String): SimEntry

    @SqlQuery("select * from sim_entries where msisdn = :msisdn")
    @RegisterMapper(SimEntryMapper::class)
    abstract fun getSimProfileByMsisdn(@Bind("msisdn")msisdn: String): SimEntry


    class SimEntryMapper : ResultSetMapper<SimEntry> {

        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): SimEntry? {
            if (r.isAfterLast) {
                return null
            }

            val id = r.getLong("id")
            val batch = r.getLong("batch")
            val hlrId = r.getString("hlrId")
            val smdpplus = r.getString("smdpplus")
            val msisdn = r.getString("msisdn")
            val iccid = r.getString("iccid")
            val imsi = r.getString("imsi")
            val eid = r.getString("eid")
            val smdpPlusActivation = r.getBoolean("smdpPlusActivation")
            val hlrActivation = r.getBoolean("hlrActivation")
            val pin1 = r.getString("pin1")
            val pin2 = r.getString("pin2")
            val puk1 = r.getString("puk1")
            val puk2 = r.getString("puk2")

            return SimEntry(
                    id = id,
                    batch = batch,
                    hlrId = hlrId,
                    smdpplus = smdpplus,
                    msisdn = msisdn,
                    iccid = iccid,
                    imsi = imsi,
                    eid = eid,
                    smdpPlusActivation = smdpPlusActivation,
                    hlrActivation = hlrActivation,
                    pin1 = pin1,
                    pin2 = pin2,
                    puk1 = puk1,
                    puk2 = puk2
            )
        }
    }



    data class SimProfileVendor(val id:Long, val name:String) {
        fun isAuthorizedForHlr(hlr: String): Boolean {
            return true // Placeholder for an actoal database lookup.
        }
    }


    class SimProfileVendorMapper : ResultSetMapper<SimProfileVendor> {

        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): SimProfileVendor? {
            if (r.isAfterLast) {
                return null
            }

            val id = r.getLong("id")
            val name = r.getString("name")

            return SimProfileVendor(
                    id = id,
                    name = name)
        }
    }


    @SqlQuery("select * from sim_profile_vendor where name = :name")
    @RegisterMapper(SimProfileVendorMapper::class)
    abstract fun getProfilevendorByName(@Bind("name")name: String): SimProfileVendor



    @SqlQuery("select * from sim_profile_vendor where id = :id")
    @RegisterMapper(SimProfileVendorMapper::class)
    abstract fun getProfilevendorById(@Bind("id")id: Long): SimProfileVendor




    @SqlQuery("select * from hlr_adapters where name = :name")
    @RegisterMapper(HlrAdapterMapper::class)
    abstract fun getHlrAdapterByName(@Bind("name")name: String): HlrAdapter


    @SqlQuery("select * from hlr_adapters where id = :id")
    @RegisterMapper(HlrAdapterMapper::class)
    abstract fun getHlrAdapterByName(@Bind("id")id: Long): HlrAdapter


    class HlrAdapterMapper : ResultSetMapper<HlrAdapter> {

        @Throws(SQLException::class)
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): HlrAdapter? {
            if (r.isAfterLast) {
                return null
            }

            val id = r.getLong("id")
            val name = r.getString("name")

            return HlrAdapter(
                    id = id,
                    name = name)
        }
    }

    abstract fun getSmdpPlusAdapterByName(name: String): SmdpPlusAdapter?

    //
    // Getting the ID of the last insert, regardless of table
    //
    @SqlQuery("select last_insert_rowid()")
    abstract fun lastInsertRowid(): Long

    //
    // Importing
    //
    @Transaction
    @SqlBatch("INSERT INTO sim_entries (iccid, imsi, pin1, pin2, puk1, puk2) VALUES (:iccid, :imsi, :pin1, :pin2, :puk1, :puk2)")
    @BatchChunkSize(1000)
    abstract fun insertAll(@BindBean entries: Iterator<SimEntry>);


    @SqlUpdate("INSERT INTO sim_import_batches (status,  importer, hlr, profileVendor) VALUES ('STARTED', :importer, :hlr, :profileVendor)")
    abstract fun createNewSimImportBatch(
            @Bind("importer") importer: String,
            @Bind("hlr") hlr: String,
            @Bind("profileVendor") profileVendor: String)

    abstract fun getIdOfBatchCreatedLast(): Long

    @SqlUpdate("UPDATE sim_import_batches SET size = :size, status = :status, endedAt = :endedAt  WHERE id = :id")
    abstract fun updateBatchState(
            @Bind("id") id: Long,
            @Bind("size") size: Long,
            @Bind("status") status: String,
            @Bind("endedAt") endedAt: Long)

    @Transaction
    fun importSims(
            importer: String,
            hlr: String,
            profileVendor: String,
            csvInputStream: InputStream): SimImportBatch {

        createNewSimImportBatch(
                importer = importer,
                hlr = hlr,
                profileVendor = profileVendor)
        val batchId = lastInsertRowid()
        val values = SimEntryIterator(
                hlrId = hlr,
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

    @SqlQuery("select * from sim_import_batches where id = :id")
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
            val hlr = r.getString("hlr")
            val profileVendor = r.getString("profileVendor")
            val size = r.getLong("size")

            return SimImportBatch(
                    id = id,
                    endedAt = endedAt,
                    status = status,
                    hlr = hlr,
                    profileVendor = profileVendor,
                    size = size,
                    importer = "XXX Replace with name of agent that facilitated the import")
        }
    }

    //
    // Setting activation statuses
    //

    @SqlUpdate("UPDATE sim_entries SET hlrActivation = :hlrActivation  WHERE id = :id")
    abstract fun setHlrActivation(
            @Bind("id") id:Long,
            @Bind("hlrActivation") hlrActivation:Boolean)


    @SqlUpdate("UPDATE sim_entries SET smdpPlusActivation = :smdpPlusActivation  WHERE id = :id")
    abstract fun setSmdpPlusActivation(
            @Bind("id") id:Long,
            @Bind("smdpPlusActivation") smdpPlusActivation:Boolean)


    /**
     * Set the entity to be marked as "active" in the HLR, then return the
     * SIM entry.
     */
    fun setActivatedInHlr(id: Long): SimEntry? {
        setHlrActivation(id, true)
        return getSimProfileById(id)
    }


    fun setActivatedInSmdpPlus(id: Long): SimEntry? {
        setSmdpPlusActivation(id, true)
        return getSimProfileById(id)
    }

    //
    //  Binding a SIM card to an MSISDN
    //

    @SqlUpdate("UPDATE sim_entries SET msisdn = :msisdn  WHERE id = :id")
    abstract fun setMsisdnOfSim(@Bind("id") id:Long, @Bind("msisdn") msisdn:String)

    //
    // Finding next free SIM card for a particular HLR.
    //
    @SqlQuery("SELECT * FROM sim_entries WHERE hlr = :hlr AND msisdn = null limit 1")
    @RegisterMapper(SimImportBatchMapper::class)
    abstract fun findNextFreeSimForMsisdn(@Bind("hlr") hlr:String): SimEntry

    //
    // Allocating next free simcards in an HLR.
    //
    @Transaction
    fun allocateNextFreeSimForMsisdn(hlr:String, msisdn: String): SimEntry? {
        val sim : SimEntry= findNextFreeSimForMsisdn(hlr)
        if (sim == null) {
            return null // No sim cards available
        }

        setMsisdnOfSim(sim.id!!, msisdn)

        // This is an inefficient way of getting an updated profile,
        // but it will work, and we can optimize it away if the need ever
        // arises.
        return getSimProfileByMsisdn(msisdn)
    }


    //
    // Creating and deleting tables (XXX only used for testing, and should be moved to
    // a test only DAO eventually)
    //
    @SqlUpdate("create table sim_import_batches (id integer primary key autoincrement, status text, endedAt integer, importer text, size integer, hlr text, profileVendor text)")
    abstract fun createImportBatchesTable();

    @SqlUpdate("drop  table sim_import_batches")
    abstract fun dropImportBatchesTable();


    @SqlUpdate("create table sim_entries (id integer primary key autoincrement, hlrid text, smdpplus text, msisdn text, eid text, hlrActivation boolean, smdpPlusActivation boolean, batch integer, imsi varchar(15), iccid varchar(22), pin1 varchar(4), pin2 varchar(4), puk1 varchar(80), puk2 varchar(80), CONSTRAINT Unique_Imsi UNIQUE (imsi), CONSTRAINT Unique_Iccid UNIQUE (iccid))")
    abstract fun createSimEntryTable();

    @SqlUpdate("drop  table sim_entries")
    abstract fun dropSimEntryTable();


    @SqlUpdate("create table hlr_adapters (id integer primary key autoincrement, name text,  CONSTRAINT Unique_Name UNIQUE (name))")
    abstract fun createHlrAdapterTable();

    @SqlUpdate("drop  table hlr_adapters")
    abstract fun dropHlrAdapterTable();


    @SqlUpdate("create table smdp_plus_adapters (id integer primary key autoincrement, name text,  CONSTRAINT Unique_Name UNIQUE (name))")
    abstract fun createSmdpPlusTable();

    @SqlUpdate("drop  table smdp_plus_adapters")
    abstract fun dropSmdpPlusTable();



    @SqlUpdate("create table sim_profile_vendor (id integer primary key autoincrement, name text,  CONSTRAINT Unique_Name UNIQUE (name))")
    abstract fun createSimProfileVendorTable();

    @SqlUpdate("drop  table sim_profile_vendor")
    abstract fun dropSimProfileVendorTable();
}
