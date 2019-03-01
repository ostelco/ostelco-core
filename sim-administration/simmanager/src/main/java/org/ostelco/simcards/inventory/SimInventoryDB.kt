package org.ostelco.simcards.inventory

import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.BatchChunkSize
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter

/**
 * Low-level SIM DB interface.
 * Note: Postgresql specific SQL statements (I think).
 */
interface SimInventoryDB {

    @SqlQuery("""SELECT * FROM sim_entries
                      WHERE id = :id""")
    fun getSimProfileById(id: Long): SimEntry?

    @SqlQuery("""SELECT * FROM sim_entries
                      WHERE iccid = :iccid""")
    fun getSimProfileByIccid(iccid: String): SimEntry?

    @SqlQuery("""SELECT * FROM sim_entries
                      WHERE imsi = :imsi""")
    fun getSimProfileByImsi(imsi: String): SimEntry?

    @SqlQuery("""SELECT * FROM sim_entries
                      WHERE msisdn = :msisdn""")
    fun getSimProfileByMsisdn(msisdn: String): SimEntry?

    /*
     * Find next available SIM card for a particular HLR ready
     * to be 'provisioned' with the SM-DP+ and HLR vendors.
     */
    @SqlQuery("""SELECT a.*
                      FROM   sim_entries a
                             JOIN (SELECT id,
                                          CASE
                                            WHEN hlrstate = 'NOT_ACTIVATED'
                                                 AND smdpplusstate = 'AVAILABLE' THEN 1
                                            WHEN hlrstate = 'NOT_ACTIVATED'
                                                 AND smdpplusstate = 'RELEASED' THEN 2
                                            WHEN hlrstate = 'ACTIVATED'
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
    fun findNextNonProvisionedSimProfileForHlr(hlrId: Long,
                                               profile: String): SimEntry?

    /*
     * Find next ready to use SIM card for a particular HLR
     * and profile (phone type).
     */
    @SqlQuery("""SELECT *
                      FROM   sim_entries
                      WHERE  hlrState = 'ACTIVATED'
                             AND smdpplusstate = 'RELEASED'
                             AND provisionState = 'AVAILABLE'
                             AND hlrId = :hlrId
                             AND profile = :profile
                      LIMIT  1""")
    fun findNextReadyToUseSimProfileForHlr(hlrId: Long,
                                           profile: String): SimEntry?


    @SqlUpdate("""UPDATE sim_entries SET eid = :eid
                       WHERE id = :id""")
    fun updateEidOfSimProfile(id: Long,
                              eid: String): Int

    /*
     * State information.
     */

    @SqlUpdate("""UPDATE sim_entries SET hlrState = :hlrState
                       WHERE id = :id""")
    fun updateHlrState(id: Long,
                       hlrState: HlrState): Int

    @SqlUpdate("""UPDATE sim_entries SET provisionState = :provisionState
                       WHERE id = :id""")
    fun updateProvisionState(id: Long,
                             provisionState: ProvisionState): Int

    @SqlUpdate("""UPDATE sim_entries SET hlrState = :hlrState,
                                              provisionState = :provisionState
                       WHERE id = :id""")
    fun updateHlrStateAndProvisionState(id: Long,
                                        hlrState: HlrState,
                                        provisionState: ProvisionState): Int

    @SqlUpdate("""UPDATE sim_entries SET smdpPlusState = :smdpPlusState
                       WHERE id = :id""")
    fun updateSmDpPlusState(id: Long, smdpPlusState: SmDpPlusState): Int

    @SqlUpdate("""UPDATE sim_entries SET smdpPlusState = :smdpPlusState,
                                              matchingId = :matchingId
                       WHERE id = :id""")
    fun updateSmDpPlusStateAndMatchingId(id: Long,
                                         smdpPlusState: SmDpPlusState,
                                         matchingId: String): Int

    /*
     * HLR and SM-DP+ 'adapters'.
     */

    @SqlQuery("""SELECT id FROM sim_vendors_permitted_hlrs
                      WHERE profileVendorId = profileVendorId
                            AND hlrId = :hlrId""")
    fun findSimVendorForHlrPermissions(profileVendorId: Long,
                                       hlrId: Long): List<Long>

    @SqlUpdate("""INSERT INTO sim_vendors_permitted_hlrs
                                   (profilevendorid,
                                    hlrid)
                       SELECT :profileVendorId,
                              :hlrId
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   sim_vendors_permitted_hlrs
                                          WHERE  profilevendorid = :profileVendorId
                                                 AND hlrid = :hlrId)""")
    fun storeSimVendorForHlrPermission(profileVendorId: Long,
                                       hlrId: Long): Int

    @SqlUpdate("""INSERT INTO hlr_adapters
                                   (name)
                       SELECT :name
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   hlr_adapters
                                          WHERE  name = :name)""")
    fun addHlrAdapter(name: String): Int

    @SqlQuery("""SELECT * FROM hlr_adapters
                      WHERE name = :name""")
    fun getHlrAdapterByName(name: String): HlrAdapter?

    @SqlQuery("""SELECT * FROM hlr_adapters
                      WHERE id = :id""")
    fun getHlrAdapterById(id: Long): HlrAdapter?

    @SqlUpdate("""INSERT INTO profile_vendor_adapters
                                   (name)
                       SELECT :name
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   profile_vendor_adapters
                                          WHERE  name = :name) """)
    fun addProfileVendorAdapter(name: String): Int

    @SqlQuery("""SELECT * FROM profile_vendor_adapters
                       WHERE name = :name""")
    fun getProfileVendorAdapterByName(name: String): ProfileVendorAdapter?

    @SqlQuery("""SELECT * FROM profile_vendor_adapters
                      WHERE id = :id""")
    fun getProfileVendorAdapterById(id: Long): ProfileVendorAdapter?

    /*
     * Batch handling.
     */

    @Transaction
    @SqlBatch("""INSERT INTO sim_entries
                                  (batch, profileVendorId, hlrid, hlrState, smdpplusstate, provisionState, matchingId, profile, iccid, imsi, msisdn, pin1, pin2, puk1, puk2)
                      VALUES (:batch, :profileVendorId, :hlrId, :hlrState, :smdpPlusState, :provisionState, :matchingId, :profile, :iccid, :imsi, :msisdn, :pin1, :pin2, :puk1, :puk2)""")
    @BatchChunkSize(1000)
    fun insertAll(@BindBean entries: Iterator<SimEntry>)

    @SqlUpdate("""INSERT INTO sim_import_batches (status,  importer, hlrId, profileVendorId)
                       VALUES ('STARTED', :importer, :hlrId, :profileVendorId)""")
    fun createNewSimImportBatch(importer: String,
                                hlrId: Long,
                                profileVendorId: Long): Int

    @SqlUpdate("""UPDATE sim_import_batches SET size = :size,
                                                     status = :status,
                                                     endedAt = :endedAt
                       WHERE id = :id""")
    fun updateBatchState(id: Long,
                         size: Long,
                         status: String,
                         endedAt: Long): Int

    @SqlQuery("""SELECT * FROM sim_import_batches
                      WHERE id = :id""")
    fun getBatchInfo(id: Long): SimImportBatch?

    /*
     * Returns the 'id' of the last insert, regardless of table.
     */
    @SqlQuery("SELECT lastval()")
    fun lastInsertedRowId(): Long

    /**
     * Find all the different HLRs that are present.
     */
    @SqlQuery("SELECT * FROM hlr_adapters")
    // TODO(RMZ): @RegisterMapper(HlrAdapterMapper::class)
    @RegisterRowMapper(HlrAdapterMapper::class)
    fun getHlrAdapters(): List<HlrAdapter>


    /**
     * Find the names of profiles that are associated with
     * a particular HLR.
     */
    @SqlQuery("""SELECT DISTINCT profile  FROM sim_entries
                      WHERE hlrId = :hlrId""")
    fun getProfileNamesForHlr(hlrId: Long): List<String>


    /**
     * Get key numbers from a particular named Sim profile.
     * NOTE: This method is intended as an internal helper method for getProfileStats, its signature
     * can change at any time, so don't use it unless you really know what you're doing.
     */
    @SqlQuery("""
        SELECT 'NO_OF_ENTRIES' AS KEY,  count(*)  AS VALUE  FROM sim_entries WHERE hlrId = :hlrId AND profile = :simProfile
        UNION
        SELECT 'NO_OF_UNALLOCATED_ENTRIES' AS KEY,  count(*)  AS VALUE  FROM sim_entries
                   WHERE hlrId = :hlrId AND profile = :simProfile AND
                         smdpPlusState =  :smdpUnallocatedState AND
                         hlrState = :hlrUnallocatedState
        UNION
        SELECT 'NO_OF_RELEASED_ENTRIES' AS KEY,  count(*)  AS VALUE  FROM sim_entries
                   WHERE hlrId = :hlrId AND profile = :simProfile AND
                         smdpPlusState =  :smdpReleasedState AND
                         hlrState = :hlrAllocatedState
        UNION
        SELECT 'NO_OF_ENTRIES_READY_FOR_IMMEDIATE_USE' AS KEY,  count(*)  AS VALUE  FROM sim_entries
                   WHERE hlrId = :hlrId AND profile = :simProfile AND
                         smdpPlusState =  :smdpReleasedState AND
                         hlrState = :hlrAllocatedState
    """)
    @RegisterRowMapper(KeyValueMapper::class)
    fun getProfileStatsAsKeyValuePairs(
            hlrId: Long,
            simProfile: String,
            smdpReleasedState: String = SmDpPlusState.RELEASED.name,
            hlrUnallocatedState: String = HlrState.NOT_ACTIVATED.name,
            smdpUnallocatedState: String = SmDpPlusState.AVAILABLE.name,
            hlrAllocatedState: String = HlrState.ACTIVATED.name,
            smdpAllocatedState: String = SmDpPlusState.ALLOCATED.name,
            smdpDownloadedState: String = SmDpPlusState.DOWNLOADED.name): List<KeyValuePair>
}
