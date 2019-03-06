package org.ostelco.simcards.inventory

import org.ostelco.prime.getLogger
import org.ostelco.sim.es2plus.ES2NotificationPointStatus
import org.ostelco.sim.es2plus.ES2RequestHeader
import org.ostelco.sim.es2plus.FunctionExecutionStatusType
import org.ostelco.sim.es2plus.SmDpPlusCallbackService

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

            /* Update SM-DP+ state. */
            when (notificationPointId) {
                1 -> {
                    /* Eligibility and retry limit check. */
                }
                2 -> {
                    /* ConfirmationFailure. */
                }
                3 -> {
                    /* BPP download. */
                    logger.info("Updating SM-DP+ state to {} with value from 'download-progress-info' message' for ICCID {}",
                            SmDpPlusState.DOWNLOADED, iccid)
                    dao.setSmDpPlusStateUsingIccid(iccid, SmDpPlusState.DOWNLOADED)
                }
                4 -> {
                    /* BPP installation. */
                    logger.info("Updating SM-DP+ state to {} with value from 'download-progress-info' message' for ICCID {}",
                            SmDpPlusState.INSTALLED, iccid)
                    dao.setSmDpPlusStateUsingIccid(iccid, SmDpPlusState.INSTALLED)
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
}
