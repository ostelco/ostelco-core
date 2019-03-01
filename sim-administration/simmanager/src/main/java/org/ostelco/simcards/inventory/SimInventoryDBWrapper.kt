package org.ostelco.simcards.inventory

import arrow.core.Either
import org.jdbi.v3.core.JdbiException
import org.ostelco.prime.storage.StoreError
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter
import org.postgresql.util.PSQLException


interface SimInventoryDBWrapper {

    fun getSimProfileById(id: Long): Either<StoreError, SimEntry>

    fun getSimProfileByIccid(iccid: String): Either<StoreError, SimEntry>

    fun getSimProfileByImsi(imsi: String): Either<StoreError, SimEntry>

    fun getSimProfileByMsisdn(msisdn: String): Either<StoreError, SimEntry>

    fun findNextNonProvisionedSimProfileForHlr(hlrId: Long, profile: String): Either<StoreError, SimEntry>

    fun findNextReadyToUseSimProfileForHlr(hlrId: Long, profile: String): Either<StoreError, SimEntry>

    fun updateEidOfSimProfile(id: Long, eid: String): Either<StoreError, Int>

    /*
     * State information.
     */

    fun updateHlrState(id: Long, hlrState: HlrState): Either<StoreError, Int>

    fun updateProvisionState(id: Long, provisionState: ProvisionState): Either<StoreError, Int>

    fun updateHlrStateAndProvisionState(id: Long, hlrState: HlrState, provisionState: ProvisionState): Either<StoreError, Int>

    fun updateSmDpPlusState(id: Long, smdpPlusState: SmDpPlusState): Either<StoreError, Int>

    fun updateSmDpPlusStateAndMatchingId(id: Long, smdpPlusState: SmDpPlusState, matchingId: String): Either<StoreError, Int>

    /*
     * HLR and SM-DP+ 'adapters'.
     */

    fun findSimVendorForHlrPermissions(profileVendorId: Long, hlrId: Long): Either<StoreError, List<Long>>

    fun storeSimVendorForHlrPermission(profileVendorId: Long, hlrId: Long): Either<StoreError, Int>

    fun addHlrAdapter(name: String): Either<StoreError, Int>

    fun getHlrAdapterByName(name: String): Either<StoreError, HlrAdapter>

    fun getHlrAdapterById(id: Long): Either<StoreError, HlrAdapter>

    fun addProfileVendorAdapter(name: String): Either<StoreError, Int>

    fun getProfileVendorAdapterByName(name: String): Either<StoreError, ProfileVendorAdapter>

    fun getProfileVendorAdapterById(id: Long): Either<StoreError, ProfileVendorAdapter>

    /*
     * Batch handling.
     */

    fun insertAll(entries: Iterator<SimEntry>): Either<StoreError, Unit>

    fun createNewSimImportBatch(importer: String, hlrId: Long, profileVendorId: Long): Either<StoreError, Int>

    fun updateBatchState(id: Long, size: Long, status: String, endedAt: Long): Either<StoreError, Int>

    fun getBatchInfo(id: Long): Either<StoreError, SimImportBatch>

    /*
     * Returns the 'id' of the last insert, regardless of table.
     */

    fun lastInsertedRowId(): Either<StoreError, Long>

    /**
     * Find all the different HLRs that are present.
     */

    fun getHlrAdapters(): Either<StoreError, List<HlrAdapter>>

    /**
     * Find the names of profiles that are associated with
     * a particular HLR.
     */

    fun getProfileNamesForHlr(hlrId: Long): Either<StoreError, List<String>>

    /**
     * Get key numbers from a particular named Sim profile.
     * NOTE: This method is intended as an internal helper method for getProfileStats, its signature
     * can change at any time, so don't use it unless you really know what you're doing.
     */

    fun getProfileStatsAsKeyValuePairs(hlrId: Long, simProfile: String): Either<StoreError, List<KeyValuePair>>
}