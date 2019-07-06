package org.ostelco.simcards.inventory

import org.ostelco.prime.getLogger
import org.ostelco.sim.es2plus.ES2NotificationPointStatus
import org.ostelco.sim.es2plus.ES2RequestHeader
import org.ostelco.sim.es2plus.FunctionExecutionStatusType
import org.ostelco.sim.es2plus.SmDpPlusCallbackService
import org.ostelco.simcards.admin.ApiRegistry.simProfileStatusUpdateCallback
import org.ostelco.simcards.admin.SimManagerSingleton.asSimProfileStatus

/**
 * ES2+ callbacks handling.
 */
class SimInventoryCallbackService(val dao: SimInventoryDAO) : SmDpPlusCallbackService {

    private val logger by getLogger()

    override fun handleDownloadProgressInfo(header: ES2RequestHeader,
                                            eid: String?,
                                            iccid: String,
                                            profileType: String,
                                            timestamp: String,
                                            notificationPointId: Int,
                                            notificationPointStatus: ES2NotificationPointStatus,
                                            resultData: String?,
                                            imei: String?) {
        if (notificationPointStatus.status == FunctionExecutionStatusType.ExecutedSuccess) {

            /* XXX To be removed or updated to debug. */
            logger.info("download-progress-info: Received message with status 'executed-success' for ICCID {}" +
                    "(notificationPointId: {}, profileType: {}, resultData: {})",
                    iccid, notificationPointId, profileType, resultData)

            /* Update EID. */
            if (!eid.isNullOrEmpty()) {
                /* XXX To be removed or updated to debug. */
                logger.info("download-progress-info: Updating EID to {} for ICCID {}",
                        eid, iccid)
                dao.setEidOfSimProfileByIccid(iccid, eid)
            }

            /**
             * Update SM-DP+ state.
             *      There is a somewhat more subtle failure mode, namly that the SM-DP+ for some reason
             *      is unable to signal back, in that case the state has actually changed, but that fact will not
             *      be picked up by the state as stored in the database, and if the user interface is dependent
             *      on that state, the user interface may suffer a failure.  These issues needs to be gamed out
             *      and fixed in some reasonable manner.
             */
            when (notificationPointId) {
                1 -> {
                    /* Eligibility and retry limit check. */
                }
                2 -> {
                    /* ConfirmationFailure. */
                }
                3 -> {
                    /* BPP download. */
                    gotoState(iccid, SmDpPlusState.DOWNLOADED)
                }
                4 -> {
                    /* BPP installation. */
                    gotoState(iccid, SmDpPlusState.INSTALLED)
                }
                else -> {
                    /* Unexpected check point value. */
                    logger.error("download-progress-info: Received message with unexpected 'notificationPointId' {} for ICCID {}" +
                            "(notificationPointStatus: {}, profileType: {}, resultData: {})",
                            notificationPointId, iccid, notificationPointStatus,
                            profileType, resultData)
                }
            }
        } else {
            /* XXX Update to handle other cases explicitly + review of logging. */
            logger.warn("download-progress-info: Received message with notificationPointStatus {} for ICCID {}" +
                    "(notificationPointId: {}, profileType: {}, resultData: {})",
                    notificationPointStatus, iccid, notificationPointId,
                    profileType, resultData)
        }
    }

    /**
     * This is in fact buggy, since it assumes that the transitions are legal, which they only are
     *       they are carried out on profiles that are in the database, and that the transitions that are
     *      being performed are valid state transitions.  None of these criteria are tested for, and
     *      errors are not si
     */
    private fun gotoState(iccid: String, targetSmdpPlusStatus: SmDpPlusState) {
        logger.info("Updating SM-DP+ state to {} with value from 'download-progress-info' message' for ICCID {}",
                SmDpPlusState.DOWNLOADED, iccid)
        dao.setSmDpPlusStateUsingIccid(iccid, targetSmdpPlusStatus)
        simProfileStatusUpdateCallback?.invoke(iccid, asSimProfileStatus(targetSmdpPlusStatus))
    }
}
