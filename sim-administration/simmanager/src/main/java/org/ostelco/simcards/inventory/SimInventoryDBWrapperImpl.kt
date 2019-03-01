package org.ostelco.simcards.inventory

import arrow.core.Either
import org.jdbi.v3.core.JdbiException
import org.ostelco.prime.storage.DatabaseError
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.SystemError
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter
import org.postgresql.util.PSQLException


class SimInventoryDBWrapperImpl(val db: SimInventoryDB) : SimInventoryDBWrapper {

    override fun getSimProfileById(id: Long): Either<StoreError, SimEntry> =
            either(NotFoundError("sim_entries", "${id}")) {
                db.getSimProfileById(id)!!
            }

    override fun getSimProfileByIccid(iccid: String): Either<StoreError, SimEntry> =
            either(NotFoundError("sim_entries", iccid)) {
                db.getSimProfileByIccid(iccid)!!
            }

    override fun getSimProfileByImsi(imsi: String): Either<StoreError, SimEntry> =
            either(NotFoundError("sim_entries", imsi)) {
                db.getSimProfileByImsi(imsi)!!
            }

    override fun getSimProfileByMsisdn(msisdn: String): Either<StoreError, SimEntry> =
            either(NotFoundError("sim_entries", msisdn)) {
                db.getSimProfileByMsisdn(msisdn)!!
            }

    override fun findNextNonProvisionedSimProfileForHlr(hlrId: Long, profile: String): Either<StoreError, SimEntry> =
            either(NotFoundError("sim_entries", "${hlrId} - ${profile}")) {
                db.findNextNonProvisionedSimProfileForHlr(hlrId, profile)!!
            }

    override fun findNextReadyToUseSimProfileForHlr(hlrId: Long, profile: String): Either<StoreError, SimEntry> =
            either(NotFoundError("sim_entries", "${hlrId} - ${profile}")) {
                db.findNextReadyToUseSimProfileForHlr(hlrId, profile)!!
            }


    override fun updateEidOfSimProfile(id: Long, eid: String): Either<StoreError, Int> =
            either {
                db.updateEidOfSimProfile(id, eid)
            }

    /*
     * State information.
     */

    override fun updateHlrState(id: Long, hlrState: HlrState): Either<StoreError, Int> =
            either {
                db.updateHlrState(id, hlrState)
            }

    override fun updateProvisionState(id: Long, provisionState: ProvisionState): Either<StoreError, Int> =
            either {
                db.updateProvisionState(id, provisionState)
            }

    override fun updateHlrStateAndProvisionState(id: Long, hlrState: HlrState, provisionState: ProvisionState): Either<StoreError, Int> =
            either {
                db.updateHlrStateAndProvisionState(id, hlrState, provisionState)

            }

    override fun updateSmDpPlusState(id: Long, smdpPlusState: SmDpPlusState): Either<StoreError, Int> =
            either {
                db.updateSmDpPlusState(id, smdpPlusState)
            }

    override fun updateSmDpPlusStateAndMatchingId(id: Long, smdpPlusState: SmDpPlusState, matchingId: String): Either<StoreError, Int> =
            either {
                db.updateSmDpPlusStateAndMatchingId(id, smdpPlusState, matchingId)
            }

    /*
     * HLR and SM-DP+ 'adapters'.
     */

    override fun findSimVendorForHlrPermissions(profileVendorId: Long, hlrId: Long): Either<StoreError, List<Long>> =
            either(NotFoundError("sim_vendors_permitted_hlrs","${profileVendorId} - ${hlrId}")) {
                db.findSimVendorForHlrPermissions(profileVendorId, hlrId)
            }

    override fun storeSimVendorForHlrPermission(profileVendorId: Long, hlrId: Long): Either<StoreError, Int> =
            either {
                db.storeSimVendorForHlrPermission(profileVendorId, hlrId)
            }

    override fun addHlrAdapter(name: String): Either<StoreError, Int> =
            either {
                db.addHlrAdapter(name)
            }

    override fun getHlrAdapterByName(name: String): Either<StoreError, HlrAdapter> =
            either(NotFoundError("hlr_adapters", name)) {
                db.getHlrAdapterByName(name)!!
            }

    override fun getHlrAdapterById(id: Long): Either<StoreError, HlrAdapter> =
            either(NotFoundError("hlr_adapters", "${id}")) {
                db.getHlrAdapterById(id)!!
            }

    override fun addProfileVendorAdapter(name: String): Either<StoreError, Int> =
            either {
                db.addProfileVendorAdapter(name)
            }

    override fun getProfileVendorAdapterByName(name: String): Either<StoreError, ProfileVendorAdapter> =
            either(NotFoundError("profile_vendor_adapters", name)) {
                db.getProfileVendorAdapterByName(name)!!
            }

    override fun getProfileVendorAdapterById(id: Long): Either<StoreError, ProfileVendorAdapter> =
            either(NotFoundError("profile_vendor_adapters", "${id}")) {
                db.getProfileVendorAdapterById(id)!!
            }

    /*
     * Batch handling.
     */

    override fun insertAll(entries: Iterator<SimEntry>): Either<StoreError, Unit> =
            either {
                db.insertAll(entries)
            }

    override fun createNewSimImportBatch(importer: String, hlrId: Long, profileVendorId: Long): Either<StoreError, Int> =
            either {
                db.createNewSimImportBatch(importer, hlrId, profileVendorId)
            }

    override fun updateBatchState(id: Long, size: Long, status: String, endedAt: Long): Either<StoreError, Int> =
            either {
                db.updateBatchState(id, size, status, endedAt)
            }

    override fun getBatchInfo(id: Long): Either<StoreError, SimImportBatch> =
            either(NotFoundError("sim_import_batches", "${id}")) {
                db.getBatchInfo(id)!!
            }
    /*
     * Returns the 'id' of the last insert, regardless of table.
     */

    override fun lastInsertedRowId(): Either<StoreError, Long> =
            either {
                db.lastInsertedRowId()
            }

    /**
     * Find all the different HLRs that are present.
     */

    override fun getHlrAdapters(): Either<StoreError, List<HlrAdapter>> =
            either(NotFoundError("hlr_adapters","")) {
                db.getHlrAdapters()
            }

    /**
     * Find the names of profiles that are associated with
     * a particular HLR.
     */

    override fun getProfileNamesForHlr(hlrId: Long): Either<StoreError, List<String>> =
            either(NotFoundError("sim_entries", "${hlrId}")) {
                db.getProfileNamesForHlr(hlrId)
            }

    /**
     * Get key numbers from a particular named Sim profile.
     * NOTE: This method is intended as an internal helper method for getProfileStats, its signature
     * can change at any time, so don't use it unless you really know what you're doing.
     */

    override fun getProfileStatsAsKeyValuePairs(hlrId: Long, simProfile: String): Either<StoreError, List<KeyValuePair>> =
            either(NotFoundError("sim_entries", "${hlrId} - ${simProfile}")) {
                db.getProfileStatsAsKeyValuePairs(hlrId, simProfile)
            }

    /* Convenience functions. */

    private fun <R> either(action: () -> R): Either<StoreError, R> =
            try {
                Either.right(action())
            } catch (e: Exception) {
                Either.left(when (e) {
                    is JdbiException, is PSQLException -> {
                        DatabaseError("sim-manager", "", e.message!!)
                    }
                    else -> {
                        SystemError("sim-manager", "", e.message!!)
                    }
                })
            }

    private fun <R> either(error: StoreError, action: () -> R): Either<StoreError, R> =
            try {
                action()?.let {
                    Either.right(it)
                } ?: Either.left(error)
            } catch (e: Exception) {
                Either.left(when (e) {
                    is JdbiException, is PSQLException -> {
                        DatabaseError("sim-manager", "", e.message!!)
                    }
                    else -> {
                        SystemError("sim-manager", "", e.message!!)
                    }
                })
            }
}