package org.ostelco.prime.sim

import arrow.core.Either
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.model.SimProfileStatus

/**
 * This is the external interface for sim management.
 * XXX TODO:   Error situations should be reported in a fashion that makes it possible for user agents to
 *             provide sensible error messages.   This is happening accoring to some protocol that is described
 *             somewhere, but not here.  That is obviously an error that should be amended.
 */
interface SimManager {

    /**
     * hlr: Unique name (from configuration) of HLR from which the profile should be found. The hlr is
     *   closely associated with the operator the profile is permitting access for.  The typical case
     *   is for an operator only to have a single HLR, but it is  not unheard of for operators to
     *   have multiple HLRs.
     * phoneType:  A string describing the phone type.    The default value is "generic", which should
     *   give a profile that is usable for all types of phones, but may not fully utilise all capabilities
     *   available for esim management for the particular  phone being provisioned.
     *
     *  Error situations: Not described (XXX TODO)
     */
    fun allocateNextEsimProfile(hlr: String, phoneType: String?) : Either<String, SimEntry>

    /**
     * Get a sim profile with a particular ICCID from an HLR.
     * Error situations:
     *    * No corresponding profile found: Not described (XXX TODO).
     *    * Illegal HLR: Not described (XXX TODO).
     *    * Incorrect ICCID: (XXX TODO)
     *         * Subcases:
     *               - Illegal syntax (not numeric, wrong magic numbers etc.)
     *               - Mismatch between country code/operator code from HLR and from ICCID
     */
    fun getSimProfile(hlr: String, iccId:String) : Either<String, SimEntry>

    /**
     * Register a callback function that will be invoked when a status change is detected.
     * The status changes will typically originate from  ES2+ callbacks sent from the sim
     * profile vendor's SM-DP+ to the  sim manager instance.
     */
    fun getSimProfileStatusUpdates(onUpdate:(iccId:String, status: SimProfileStatus) -> Unit)
}