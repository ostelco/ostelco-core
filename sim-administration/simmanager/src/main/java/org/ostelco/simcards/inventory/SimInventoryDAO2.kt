package org.ostelco.simcards.inventory

import arrow.core.Either
import org.jdbi.v3.core.JdbiException
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter
import org.postgresql.util.PSQLException


class SimInventoryDAO2(val db: SimInventoryDB) {

    fun getSimProfileById(id: Long): Either<DatabaseError, SimEntry> =
            either(NotFoundError("Found no SIM profile for id ${id}")) {
                db.getSimProfileById(id)!!
            }

    fun getSimProfileByIccid(iccid: String): Either<DatabaseError, SimEntry> =
            either(NotFoundError("Found no SIM profile for ICCID ${iccid}")) {
                db.getSimProfileByIccid(iccid)!!
            }

    fun getSimProfileByImsi(imsi: String): Either<DatabaseError, SimEntry> =
            either(NotFoundError("Found no SIM profile for IMSI ${imsi}")) {
                db.getSimProfileByImsi(imsi)!!
            }

    fun getSimProfileByMsisdn(msisdn: String): Either<DatabaseError, SimEntry> =
            either(NotFoundError("Found no SIM profile for MSISDN ${msisdn}")) {
                db.getSimProfileByMsisdn(msisdn)!!
            }

    fun findNextNonProvisionedSimProfileForHlr(hlrId: Long, profile: String): Either<DatabaseError, SimEntry> =
            either(NotFoundError("No unprovisioned SIM available for BSS ${hlrId} with profile ${profile}")) {
                db.findNextNonProvisionedSimProfileForHlr(hlrId, profile)!!
            }

    fun findNextReadyToUseSimProfileForHlr(hlrId: Long, profile: String): Either<DatabaseError, SimEntry> =
            either(NotFoundError("No SIM available for BSS ${hlrId} with profile ${profile}")) {
                db.findNextReadyToUseSimProfileForHlr(hlrId, profile)!!
            }


    fun updateEidOfSimProfile(id: Long, eid: String): Either<DatabaseError, Int> =
            either {
                db.updateEidOfSimProfile(id, eid)
            }

    /*
     * State information.
     */

    fun updateHlrState(id: Long, hlrState: HlrState): Either<DatabaseError, Int> =
            either {
                db.updateHlrState(id, hlrState)
            }

    fun updateProvisionState(id: Long, provisionState: ProvisionState): Either<DatabaseError, Int> =
            either {
                db.updateProvisionState(id, provisionState)
            }

    fun updateHlrStateAndProvisionState(id: Long, hlrState: HlrState, provisionState: ProvisionState): Either<DatabaseError, Int> =
            either {
                db.updateHlrStateAndProvisionState(id, hlrState, provisionState)

            }

    fun updateSmDpPlusState(id: Long, smdpPlusState: SmDpPlusState): Either<DatabaseError, Int> =
            either {
                db.updateSmDpPlusState(id, smdpPlusState)
            }

    fun updateSmDpPlusStateAndMatchingId(id: Long, smdpPlusState: SmDpPlusState, matchingId: String): Either<DatabaseError, Int> =
            either {
                db.updateSmDpPlusStateAndMatchingId(id, smdpPlusState, matchingId)
            }

    /*
     * HLR and SM-DP+ 'adapters'.
     */

    fun findSimVendorForHlrPermissions(profileVendorId: Long, hlrId: Long): Either<DatabaseError, List<Long>> =
            either(NotFoundError("Found no SIM vendor premission for vendor id ${profileVendorId} and HLR id ${hlrId}")) {
                db.findSimVendorForHlrPermissions(profileVendorId, hlrId)
            }

    fun storeSimVendorForHlrPermission(profileVendorId: Long, hlrId: Long): Either<DatabaseError, Int> =
            either {
                db.storeSimVendorForHlrPermission(profileVendorId, hlrId)
            }

    fun addHlrAdapter(name: String): Either<DatabaseError, Int> =
            either {
                db.addHlrAdapter(name)
            }

    fun getHlrAdapterByName(name: String): Either<DatabaseError, HlrAdapter> =
            either(NotFoundError("Found no HLR adapter with name ${name}")) {
                db.getHlrAdapterByName(name)!!
            }

    fun getHlrAdapterById(id: Long): Either<DatabaseError, HlrAdapter> =
            either(NotFoundError("Found no HLR adapter with id ${id}")) {
                db.getHlrAdapterById(id)!!
            }

    fun addProfileVendorAdapter(name: String): Either<DatabaseError, Int> =
            either {
                db.addProfileVendorAdapter(name)
            }

    fun getProfileVendorAdapterByName(name: String): Either<DatabaseError, ProfileVendorAdapter> =
            either(NotFoundError("Found no SIM vendor adapter with name ${name}")) {
                db.getProfileVendorAdapterByName(name)!!
            }

    fun getProfileVendorAdapterById(id: Long): Either<DatabaseError, ProfileVendorAdapter> =
            either(NotFoundError("Found no SIM vendor adapter with id ${id}")) {
                db.getProfileVendorAdapterById(id)!!
            }

    /*
     * Batch handling.
     */

    fun insertAll(entries: Iterator<SimEntry>): Either<DatabaseError, Unit> =
            either {
                db.insertAll(entries)
            }

    fun createNewSimImportBatch(importer: String, hlrId: Long, profileVendorId: Long): Either<DatabaseError, Int> =
            either {
                db.createNewSimImportBatch(importer, hlrId, profileVendorId)
            }

    fun updateBatchState(id: Long, size: Long, status: String, endedAt: Long): Either<DatabaseError, Int> =
            either {
                db.updateBatchState(id, size, status, endedAt)
            }

    fun getBatchInfo(id: Long): Either<DatabaseError, SimImportBatch> =
            either(NotFoundError("Found no info for batch id ${id}")) {
                db.getBatchInfo(id)!!
            }
    /*
     * Returns the 'id' of the last insert, regardless of table.
     */

    fun lastInsertedRowId(): Either<DatabaseError, Long> =
            either {
                db.lastInsertedRowId()
            }

    /**
     * Find all the different HLRs that are present.
     */

    fun getHlrAdapters(): Either<DatabaseError, List<HlrAdapter>> =
            either(NotFoundError("Found no HLR adapters")) {
                db.getHlrAdapters()
            }

    /**
     * Find the names of profiles that are associated with
     * a particular HLR.
     */

    fun getProfileNamesForHlr(hlrId: Long): Either<DatabaseError, List<String>> =
            either(NotFoundError("Found no profile names for HLR id ${hlrId}")) {
                db.getProfileNamesForHlr(hlrId)
            }

    /**
     * Get key numbers from a particular named Sim profile.
     * NOTE: This method is intended as an internal helper method for getProfileStats, its signature
     * can change at any time, so don't use it unless you really know what you're doing.
     */

    fun getProfileStatsAsKeyValuePairs(hlrId: Long, simProfile: String): Either<DatabaseError, List<KeyValuePair>> =
            either(NotFoundError("Cound not generate statistics for HLR id ${hlrId} and SIM profile ${simProfile}")) {
                db.getProfileStatsAsKeyValuePairs(hlrId, simProfile)
            }


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
                .isRight()
    }

    private fun <R> either(action: () -> R): Either<DatabaseError, R> =
            try {
                Either.right(action())
            } catch (e: Exception) {
                Either.left(when (e) {
                    is JdbiException, is PSQLException -> {
                        InternalError("", e.message!!)
                    }
                    else -> {
                        Error("", e.message!!)
                    }
                })
            }

    private fun <R> either(error: DatabaseError, action: () -> R): Either<DatabaseError, R> =
            try {
                action()?.let {
                    Either.right(it)
                } ?: Either.left(error)
            } catch (e: Exception) {
                Either.left(when (e) {
                    is JdbiException, is PSQLException -> {
                        InternalError("", e.message!!)
                    }
                    else -> {
                        Error("", e.message!!)
                    }
                })
            }
}