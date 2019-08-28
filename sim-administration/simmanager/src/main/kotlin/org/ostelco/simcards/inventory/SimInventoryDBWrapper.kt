package org.ostelco.simcards.inventory

import arrow.core.Either
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.hss.HssEntry
import org.ostelco.simcards.profilevendors.ProfileVendorAdapter


interface SimInventoryDBWrapper {

    fun getSimProfileById(id: Long): Either<SimManagerError, SimEntry>

    fun getSimProfileByIccid(iccid: String): Either<SimManagerError, SimEntry>

    fun getSimProfileByImsi(imsi: String): Either<SimManagerError, SimEntry>

    fun getSimProfileByMsisdn(msisdn: String): Either<SimManagerError, SimEntry>

    fun findNextNonProvisionedSimProfileForHss(hssId: Long, profile: String): Either<SimManagerError, SimEntry>

    fun findNextReadyToUseSimProfileForHss(hssId: Long, profile: String): Either<SimManagerError, SimEntry>

    fun getAllProfileVendors(): Either<SimManagerError, List<ProfileVendorAdapter>>

    /**
     * Sets the EID value of a SIM entry (profile).
     * @param iccid  SIM entry to update
     * @param eid  the eid value
     * @return updated SIM entry
     */
    fun setEidOfSimProfileByIccid(iccid: String, eid: String): Either<SimManagerError, SimEntry>

    /**
     * Sets the EID value of a SIM entry (profile).
     * @param id  row to update
     * @param eid  the eid value
     * @return updated SIM entry
     */
    fun setEidOfSimProfile(id: Long, eid: String): Either<SimManagerError, SimEntry>

    /*
     * State information.
     */

    /**
     * Set the entity to be marked as "active" in the HSS, then return the
     * SIM entry.
     * @param id row to update
     * @param state new state from HSS service interaction
     * @return updated row or null on no match
     */
    fun setHssState(id: Long, state: HssState): Either<SimManagerError, SimEntry>

    /**
     * Set the provision state of a SIM entry, then return the entry.
     * @param id row to update
     * @param state new state from HSS service interaction
     * @return updated row or null on no match
     */
    fun setProvisionState(id: Long, state: ProvisionState): Either<SimManagerError, SimEntry>

    /**
     * Updates state of SIM profile and returns the updated profile.
     * @param id  row to update
     * @param state  new state from SMDP+ service interaction
     * @return updated row or null on no match
     */
    fun setSmDpPlusState(id: Long, state: SmDpPlusState): Either<SimManagerError, SimEntry>

    /**
     * Updates state of SIM profile and returns the updated profile.
     * @param iccid  SIM entry to update
     * @param state  new state from SMDP+ service interaction
     * @return updated row or null on no match
     */
    fun setSmDpPlusStateUsingIccid(iccid: String, state: SmDpPlusState): Either<SimManagerError, SimEntry>

    /**
     * Updates state of SIM profile and returns the updated profile.
     * Updates state and the 'matching-id' of a SIM profile and return
     * the updated profile.
     * @param id  row to update
     * @param state  new state from SMDP+ service interaction
     * @param matchingId  SM-DP+ ES2 'matching-id' to be sent to handset
     * @return updated row or null on no match
     */
    fun setSmDpPlusStateAndMatchingId(id: Long, state: SmDpPlusState, matchingId: String): Either<SimManagerError, SimEntry>

    /*
     * HSS and SM-DP+ 'adapters'.
     */

    fun findSimVendorForHssPermissions(profileVendorId: Long, hssId: Long): Either<SimManagerError, List<Long>>

    fun storeSimVendorForHssPermission(profileVendorId: Long, hssId: Long): Either<SimManagerError, Int>

    fun addHssEntry(name: String): Either<SimManagerError, Int>

    fun getHssEntryByName(name: String): Either<SimManagerError, HssEntry>

    fun getHssEntryById(id: Long): Either<SimManagerError, HssEntry>

    fun addProfileVendorAdapter(name: String): Either<SimManagerError, Int>

    fun getProfileVendorAdapterByName(name: String): Either<SimManagerError, ProfileVendorAdapter>

    fun getProfileVendorAdapterById(id: Long): Either<SimManagerError, ProfileVendorAdapter>

    /*
     * Batch handling.
     */

    fun insertAll(entries: Iterator<SimEntry>): Either<SimManagerError, Unit>

    fun createNewSimImportBatch(importer: String, hssId: Long, profileVendorId: Long): Either<SimManagerError, Int>

    fun updateBatchState(id: Long, size: Long, status: String, endedAt: Long): Either<SimManagerError, Int>

    fun getBatchInfo(id: Long): Either<SimManagerError, SimImportBatch>

    /*
     * Returns the 'id' of the last insert, regardless of table.
     */

    fun lastInsertedRowId(): Either<SimManagerError, Long>

    /**
     * Find all the different HSSes that are present.
     */

    fun getHssEntries(): Either<SimManagerError, List<HssEntry>>

    /**
     * Find the names of profiles that are associated with
     * a particular HSS.
     */

    fun getProfileNamesForHssById(hssId: Long): Either<SimManagerError, List<String>>

    /**
     * Get key numbers from a particular named Sim profile.
     * NOTE: This method is intended as an internal helper method for getProfileStats, its signature
     * can change at any time, so don't use it unless you really know what you're doing.
     */

    fun getProfileStatsAsKeyValuePairs(hssId: Long, simProfile: String): Either<SimManagerError, List<KeyValuePair>>

    /**
     * Reserve numbers ending in "0000" and "9999" as they are "golden numbers" that
     * require special handling in some jurisdictions.
     */
    fun reserveGoldenNumbersForBatch(batchId: Long): Either<SimManagerError, Int>

    /**
     * Return a list of sim Profile names associated with HSSes.  Return both the
     * HSSId (database internal ID), and the public name of the HSS.
     */
    fun getHssProfileNamePairs():  Either<SimManagerError, List<HssProfileIdName>>
}

/**
 * A data class used to list sim profile names and  database IDs of the HSSes they are associated with.
 */
data class HssProfileIdName(val hssId:Long, val hssName: String, val simProfileName: String)
