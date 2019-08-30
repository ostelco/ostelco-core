package org.ostelco.sim.es2plus



class SmDpPlusException(val statusCodeData: StatusCodeData) : Exception()


interface SmDpPlusService {

    @Throws(SmDpPlusException::class)
    fun downloadOrder(eid: String?, iccid: String?, profileType: String?): Es2DownloadOrderResponse

    @Throws(SmDpPlusException::class)
    fun confirmOrder(eid: String?, iccid: String?, smdsAddress: String?, machingId: String?, confirmationCode: String?, releaseFlag:Boolean): Es2ConfirmOrderResponse

    @Throws(SmDpPlusException::class)
    fun cancelOrder(eid: String?, iccid: String?, matchingId: String?, finalProfileStatusIndicator: String?)


    @Throws(SmDpPlusException::class)
    fun getProfileStatus(iccid: String): Es2ProfileStatusResponse

    @Throws(SmDpPlusException::class)
    fun releaseProfile(iccid: String)
}

interface  SmDpPlusCallbackService {

    @Throws(SmDpPlusException::class)
    fun handleDownloadProgressInfo(
            header: ES2RequestHeader,
            eid: String?,
            iccid: String,
            profileType: String,
            timestamp: String,
            notificationPointId: Int,
            notificationPointStatus: ES2NotificationPointStatus,
            resultData: String?,
            imei: String?
    )
}
