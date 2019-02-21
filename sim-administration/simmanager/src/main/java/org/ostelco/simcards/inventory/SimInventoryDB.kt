package org.ostelco.simcards.inventory

import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.BatchChunkSize
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter


interface SimInventoryDB {

    @SqlQuery("SELECT * FROM sim_entries WHERE id = :id")
    @RegisterRowMapper(SimEntryMapper::class)
    fun getSimProfileById(@Bind("id") id: Long): SimEntry

    @SqlQuery("SELECT * FROM sim_entries WHERE iccid = :iccid")
    @RegisterRowMapper(SimEntryMapper::class)
    fun getSimProfileByIccid(@Bind("iccid") iccid: String): SimEntry

    @SqlQuery("SELECT * FROM sim_entries WHERE imsi = :imsi")
    @RegisterRowMapper(SimEntryMapper::class)
    fun getSimProfileByImsi(@Bind("imsi") imsi: String): SimEntry

    @SqlQuery("SELECT * FROM sim_entries WHERE msisdn = :msisdn")
    @RegisterRowMapper(SimEntryMapper::class)
    fun getSimProfileByMsisdn(@Bind("msisdn") msisdn: String): SimEntry

    @SqlQuery("""SELECT id FROM sim_vendors_permitted_hlrs
                      WHERE profileVendorId = profileVendorId AND hlrId = :hlrId""")
    fun findSimVendorForHlrPermissions(@Bind("profileVendorId") profileVendorId: Long,
                                       @Bind("hlrId") hlrId: Long): List<Long>

    @SqlUpdate("""INSERT INTO sim_vendors_permitted_hlrs
                                   (profilevendorid,
                                    hlrid)
                       SELECT :profileVendorId,
                              :hlrId
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   sim_vendors_permitted_hlrs
                                          WHERE  profilevendorid = :profileVendorId
                                           AND hlrid = :hlrId)""")
    fun storeSimVendorForHlrPermission(@Bind("profileVendorId") profileVendorId: Long,
                                       @Bind("hlrId") hlrId: Long): Int

    @SqlUpdate("""INSERT INTO hlr_adapters
                                   (name)
                       SELECT :name
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   hlr_adapters
                                          WHERE  name = :name)""")
    fun addHlrAdapter(@Bind("name") name: String): Int

    @SqlQuery("SELECT * FROM hlr_adapters WHERE name = :name")
    @RegisterRowMapper(HlrAdapterMapper::class)
    fun getHlrAdapterByName(@Bind("name") name: String): HlrAdapter

    @SqlQuery("SELECT * FROM hlr_adapters WHERE id = :id")
    @RegisterRowMapper(HlrAdapterMapper::class)
    fun getHlrAdapterById(@Bind("id") id: Long): HlrAdapter

    @SqlUpdate("""INSERT INTO profile_vendor_adapters
                                   (name)
                       SELECT :name
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   profile_vendor_adapters
                                          WHERE  name = :name) """)
    fun addProfileVendorAdapter(@Bind("name") name: String): Int

    @SqlQuery("SELECT * FROM profile_vendor_adapters WHERE name = :name")
    @RegisterRowMapper(ProfileVendorAdapterMapper::class)
    fun getProfileVendorAdapterByName(@Bind("name") name: String): ProfileVendorAdapter

    @SqlQuery("SELECT * FROM profile_vendor_adapters WHERE id = :id")
    @RegisterRowMapper(ProfileVendorAdapterMapper::class)
    fun getProfileVendorAdapterById(@Bind("id") id: Long): ProfileVendorAdapter

    //
    // Importing
    //

    @Transaction
    @SqlBatch("""INSERT INTO sim_entries
                                  (batch, profileVendorId, hlrid, hlrState, smdpplusstate, provisionState, matchingId, profile, iccid, imsi, msisdn, pin1, pin2, puk1, puk2)
                      VALUES (:batch, :profileVendorId, :hlrId, :hlrState, :smdpPlusState, :provisionState, :matchingId, :profile, :iccid, :imsi, :msisdn, :pin1, :pin2, :puk1, :puk2)""")
    @BatchChunkSize(1000)
    fun insertAll(@BindBean entries: Iterator<SimEntry>)

    @SqlUpdate("""INSERT INTO sim_import_batches (status,  importer, hlrId, profileVendorId)
                       VALUES ('STARTED', :importer, :hlrId, :profileVendorId)""")
    fun createNewSimImportBatch(
            @Bind("importer") importer: String,
            @Bind("hlrId") hlrId: Long,
            @Bind("profileVendorId") profileVendorId: Long): Int

    @SqlUpdate("""UPDATE sim_import_batches SET size = :size,
                                                     status = :status,
                                                     endedAt = :endedAt
                       WHERE id = :id""")
    fun updateBatchState(
            @Bind("id") id: Long,
            @Bind("size") size: Long,
            @Bind("status") status: String,
            @Bind("endedAt") endedAt: Long): Int

    /* Getting the ID of the last insert, regardless of table. */
    @SqlQuery("SELECT lastval()")
    fun lastInsertedRowId(): Long

    @SqlQuery("""SELECT * FROM sim_import_batches
                      WHERE id = :id""")
    @RegisterRowMapper(SimImportBatchMapper::class)
    fun getBatchInfo(@Bind("id") id: Long): SimImportBatch

    //
    // Setting activation statuses
    //

    @SqlUpdate("""UPDATE sim_entries SET hlrState = :hlrState
                       WHERE id = :id""")
    @RegisterRowMapper(SimEntryMapper::class)
    fun updateHlrState(
            @Bind("id") id: Long,
            @Bind("hlrState") hlrState: HlrState): Int

    @SqlUpdate("""UPDATE sim_entries SET provisionState = :provisionState
                       WHERE id = :id""")
    @RegisterRowMapper(SimEntryMapper::class)
    fun updateProvisionState(
            @Bind("id") id: Long,
            @Bind("provisionState") provisionState: ProvisionState): Int


    @SqlUpdate("""UPDATE sim_entries SET hlrState = :hlrState,
                                              provisionState = :provisionState
                       WHERE id = :id""")
    @RegisterRowMapper(SimEntryMapper::class)
    fun updateHlrStateAndProvisionState(
            @Bind("id") id: Long,
            @Bind("hlrState") hlrState: HlrState,
            @Bind("provisionState") provisionState: ProvisionState): Int


    @SqlUpdate("""UPDATE sim_entries SET smdpPlusState = :smdpPlusState
                       WHERE id = :id""")
    @RegisterRowMapper(SimEntryMapper::class)
    fun updateSmDpPlusState(
            @Bind("id") id: Long,
            @Bind("smdpPlusState") smdpPlusState: SmDpPlusState): Int

    @SqlUpdate("""UPDATE sim_entries SET smdpPlusState = :smdpPlusState,
                                              matchingId = :matchingId
                       WHERE id = :id""")
    @RegisterRowMapper(SimEntryMapper::class)
    fun updateSmDpPlusStateAndMatchingId(
            @Bind("id") id: Long,
            @Bind("smdpPlusState") smdpPlusState: SmDpPlusState,
            @Bind("matchingId") matchingId: String): Int

    //
    // Finding next free SIM card for a particular HLR.
    //
    @SqlQuery("""SELECT a.*
                      FROM   sim_entries a
                             JOIN (SELECT id,
                                          CASE
                                            WHEN hlrstate = 'ACTIVATED'
                                                 AND smdpplusstate = 'ALLOCATED' THEN 1
                                            WHEN hlrstate = 'NOT_ACTIVATED'
                                                 AND smdpplusstate = 'ALLOCATED' THEN 2
                                            WHEN hlrstate = 'NOT_ACTIVATED'
                                                 AND smdpplusstate = 'AVAILABLE' THEN 3
                                            ELSE 9999
                                          END AS position
                                  FROM   sim_entries
                                  WHERE  provisionState = 'AVAILABLE'
                                         AND hlrId = :hlrId
                                         AND profile = :profile
                                  ORDER  BY position ASC,
                                           id ASC) b
                             ON ( a.id = b.id
                                  AND b.position < 9999 )
                      LIMIT  1""")
    @RegisterRowMapper(SimEntryMapper::class)
    fun findNextFreeSimProfileForHlr(@Bind("hlrId") hlrId: Long,
                                     @Bind("profile") profile: String): SimEntry?


    @SqlUpdate("UPDATE sim_entries SET eid = :eid WHERE id = :id")
    @RegisterRowMapper(SimEntryMapper::class)
    fun updateEidOfSimProfile(@Bind("id") id: Long,
                              @Bind("eid") eid: String): Int
}
