package org.ostelco.simcards.inventory

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.BatchChunkSize
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import org.ostelco.simcards.hss.HssEntry
import org.ostelco.simcards.profilevendors.ProfileVendorAdapter
import java.sql.ResultSet

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

    /**
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
                                                 AND smdpplusstate = 'RELEASED'  THEN 2
                                            WHEN hlrstate = 'ACTIVATED'
                                                 AND smdpplusstate = 'AVAILABLE' THEN 3
                                            ELSE 9999
                                          END AS position
                                  FROM   sim_entries
                                  WHERE  provisionState = 'AVAILABLE'
                                         AND hlrId = :hssId
                                         AND profile = :profile
                                  ORDER  BY position ASC,
                                           id ASC) b
                             ON ( a.id = b.id
                                  AND b.position < 9999 )
                      LIMIT  1""")
    fun findNextNonProvisionedSimProfileForHss(hssId: Long,
                                               profile: String): SimEntry?

    /**
     * Find next ready to use SIM card for a particular HLR
     * and profile (phone type).
     */
    @SqlQuery("""SELECT *
                      FROM   sim_entries
                      WHERE  hlrState = 'ACTIVATED'
                             AND smdpplusstate = 'RELEASED'
                             AND provisionState = 'AVAILABLE'
                             AND hlrId = :hssId
                             AND profile = :profile
                      LIMIT  1""")
    fun findNextReadyToUseSimProfileForHlr(hssId: Long,
                                           profile: String): SimEntry?

    @SqlUpdate("""UPDATE sim_entries SET eid = :eid
                       WHERE iccid = :iccid""")
    fun updateEidOfSimProfileByIccid(iccid: String,
                                     eid: String): Int

    @SqlUpdate("""UPDATE sim_entries SET eid = :eid
                       WHERE id = :id""")
    fun updateEidOfSimProfile(id: Long,
                              eid: String): Int

    /**
     * State information.
     */

    @SqlUpdate("""UPDATE sim_entries SET hlrState = :hssState
                       WHERE id = :id""")
    fun updateHlrState(id: Long,
                       hssState: HssState): Int

    @SqlUpdate("""UPDATE sim_entries SET provisionState = :provisionState
                       WHERE id = :id""")
    fun updateProvisionState(id: Long,
                             provisionState: ProvisionState): Int

    @SqlUpdate("""UPDATE sim_entries SET hlrState = :hssState,
                                              provisionState = :provisionState
                       WHERE id = :id""")
    fun updateHlrStateAndProvisionState(id: Long,
                                        hssState: HssState,
                                        provisionState: ProvisionState): Int

    @SqlUpdate("""UPDATE sim_entries SET smdpPlusState = :smdpPlusState
                       WHERE id = :id""")
    fun updateSmDpPlusState(id: Long, smdpPlusState: SmDpPlusState): Int

    @SqlUpdate("""UPDATE sim_entries SET smdpPlusState = :smdpPlusState
                       WHERE iccid = :iccid""")
    fun updateSmDpPlusStateUsingIccid(iccid: String, smdpPlusState: SmDpPlusState): Int

    @SqlUpdate("""UPDATE sim_entries SET smdpPlusState = :smdpPlusState,
                                              matchingId = :matchingId
                       WHERE id = :id""")
    fun updateSmDpPlusStateAndMatchingId(id: Long,
                                         smdpPlusState: SmDpPlusState,
                                         matchingId: String): Int

    /**
     * HLR and SM-DP+ 'adapters'.
     */

    @SqlQuery("""SELECT id FROM sim_vendors_permitted_hlrs
                      WHERE profileVendorId = profileVendorId
                            AND hlrId = :hssId""")
    fun findSimVendorForHssPermissions(profileVendorId: Long,
                                       hssId: Long): List<Long>

    @SqlUpdate("""INSERT INTO sim_vendors_permitted_hlrs
                                   (profilevendorid,
                                    hlrid)
                       SELECT :profileVendorId,
                              :hssId
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   sim_vendors_permitted_hlrs
                                          WHERE  profilevendorid = :profileVendorId
                                                 AND hlrid = :hssId)""")
    fun storeSimVendorForHssPermission(profileVendorId: Long,
                                       hssId: Long): Int

    @SqlUpdate("""INSERT INTO hlr_adapters
                                   (name)
                       SELECT :name
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   hlr_adapters
                                          WHERE  name = :name)""")
    fun addHssAdapter(name: String): Int

    @SqlQuery("""SELECT * FROM hlr_adapters
                      WHERE name = :name""")
    fun getHssEntryByName(name: String): HssEntry

    @SqlQuery("""SELECT * FROM hlr_adapters
                      WHERE id = :id""")
    fun getHssEntryById(id: Long): HssEntry

    @SqlUpdate("""INSERT INTO profile_vendor_adapters
                                   (name)
                       SELECT :name
                       WHERE  NOT EXISTS (SELECT 1
                                          FROM   profile_vendor_adapters
                                          WHERE  name = :name) """)
    fun addProfileVendorAdapter(name: String): Int

    @SqlQuery("""SELECT * FROM profile_vendor_adapters""")
    fun getAllProfileVendors(): List<ProfileVendorAdapter>

    @SqlQuery("""SELECT * FROM profile_vendor_adapters
                       WHERE name = :name""")
    fun getProfileVendorAdapterByName(name: String): ProfileVendorAdapter?

    @SqlQuery("""SELECT * FROM profile_vendor_adapters
                      WHERE id = :id""")
    fun getProfileVendorAdapterById(id: Long): ProfileVendorAdapter?

    /**
     * Batch handling.
     */

    @Transaction
    @SqlBatch("""INSERT INTO sim_entries
                                  (batch, profileVendorId, hlrid, hlrState, smdpplusstate, provisionState, matchingId, profile, iccid, imsi, msisdn, pin1, pin2, puk1, puk2)
                      VALUES (:batch, :profileVendorId, :hssId, :hssState, :smdpPlusState, :provisionState, :matchingId, :profile, :iccid, :imsi, :msisdn, :pin1, :pin2, :puk1, :puk2)""")
    @BatchChunkSize(1000)
    fun insertAll(@BindBean entries: Iterator<SimEntry>)

    @SqlUpdate("""INSERT INTO sim_import_batches (status,  importer, hlrId, profileVendorId)
                       VALUES ('STARTED', :importer, :hssId, :profileVendorId)""")
    fun createNewSimImportBatch(importer: String,
                                hssId: Long,
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
    @RegisterRowMapper(HlrEntryMapper::class)
    fun getHssEntries(): List<HssEntry>


    /**
     * Find the names of profiles that are associated with
     * a particular HLR.
     */
    @SqlQuery("""SELECT DISTINCT profile  FROM sim_entries
                      WHERE hlrId = :hssId""")
    fun getProfileNamesForHss(hssId: Long): List<String>



    // WHERE hlrId = 2 AND profile = 'OYA_M1_BF76' AND
    // smdpPlusState <>  'DOWNLOADED' AND smdpPlusState <>  'INSTALLED' AND smdpPlusState <>  'ENABLED' AND  provisionState = 'AVAILABLE' AND matchingid is null

    /**
     * Get key numbers from a particular named Sim profile.
     * NOTE: This method is intended as an internal helper method for getProfileStats, its signature
     * can change at any time, so don't use it unless you really know what you're doing.
     */
    @SqlQuery("""
        SELECT 'NO_OF_ENTRIES' AS KEY,  count(*)  AS VALUE  FROM sim_entries WHERE hlrId = :hssId AND profile = :simProfile
        UNION
        SELECT 'NO_OF_UNALLOCATED_ENTRIES' AS KEY,  count(*)  AS VALUE  FROM sim_entries
                   WHERE hlrId = :hssId AND profile = :simProfile AND
                         smdpPlusState <>  :smdpDownloadedState AND smdpPlusState <>  :smdpDownloadedState  AND smdpPlusState <>  :smdpEnabledState AND matchingid IS null AND
                         provisionState = :provisionedAvailableState
        UNION
        SELECT 'NO_OF_RELEASED_ENTRIES' AS KEY,  count(*)  AS VALUE  FROM sim_entries
                   WHERE hlrId = :hssId AND profile = :simProfile AND
                         smdpPlusState =  :smdpReleasedState AND
                         hlrState = :hssAllocatedState
        UNION
           SELECT 'NO_OF_RESERVED_ENTRIES' AS KEY,  count(*)  AS VALUE  FROM sim_entries
                   WHERE hlrId = :hssId AND profile = :simProfile AND
                         provisionState = :provisionReservedState
        UNION
        
        SELECT 'NO_OF_ENTRIES_READY_FOR_IMMEDIATE_USE' AS KEY,  count(*)  AS VALUE  FROM sim_entries
                   WHERE hlrId = :hssId AND profile = :simProfile AND
                         smdpPlusState =  :smdpReleasedState AND
                         hlrState = :hssAllocatedState AND
                         provisionState = :provisionedAvailableState
    """)
    @RegisterRowMapper(KeyValueMapper::class)
    fun getProfileStatsAsKeyValuePairs(
            hssId: Long,
            simProfile: String,
            provisionReservedState: ProvisionState = ProvisionState.RESERVED,
            smdpReleasedState: String = SmDpPlusState.RELEASED.name,
            hlrUnallocatedState: String = HssState.NOT_ACTIVATED.name,
            smdpUnallocatedState: String = SmDpPlusState.AVAILABLE.name,
            hssAllocatedState: String = HssState.ACTIVATED.name,
            smdpAllocatedState: String = SmDpPlusState.ALLOCATED.name,
            smdpDownloadedState: String = SmDpPlusState.DOWNLOADED.name,
            smdpInstalledState: String = SmDpPlusState.INSTALLED.name,
            smdpEnabledState: String = SmDpPlusState.ENABLED.name,
            provisionedAvailableState: String = ProvisionState.AVAILABLE.name): List<KeyValuePair>



    @SqlQuery("""
        SELECT DISTINCT profile AS simprofilename, hlrid  AS hssid, hlr_adapters.name AS hssname FROM sim_entries, hlr_adapters WHERE hlrid=hlr_adapters.id
    """)
    @RegisterRowMapper(HssProfileNameMapper::class)
    fun getHssProfileNamePairs(): List<HssProfileIdName>


    /**
     * Golden numbers are numbers ending in either "0000" or "9999", and they have to be
     * treated specially.
     */
    @SqlUpdate("""UPDATE sim_entries SET provisionState = :provisionReservedState
                       WHERE batch = :batchId AND msisdn ~ '[0-9]*(0000|9999)$'
                       """)
    fun reserveGoldenNumbersForBatch(batchId: Long, provisionReservedState: ProvisionState = ProvisionState.RESERVED): Int
}


class HssProfileNameMapper : RowMapper<HssProfileIdName> {
    override fun map(row: ResultSet, ctx: StatementContext): HssProfileIdName? {
        if (row.isAfterLast) {
            return null
        }

        val hssId = row.getLong("hssid")
        val hssName = row.getString("hssname")
        val simProfileName = row.getString("simprofilename")
        return HssProfileIdName(hssId = hssId, hssName = hssName, simProfileName = simProfileName)
    }
}
