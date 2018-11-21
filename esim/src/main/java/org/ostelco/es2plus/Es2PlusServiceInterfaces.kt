package org.ostelco.es2plus



class SmDpPlusException(val statusCodeData: StatusCodeData) : Exception()


interface SmDpPlusService {

    @Throws(SmDpPlusException::class)
    fun downloadOrder(eid: String?, iccid: String?, profileType: String?): String

    @Throws(SmDpPlusException::class)
    fun confirmOrder(eid: String, smdsAddress: String?, machingId: String?, confirmationCode: String?)

    @Throws(SmDpPlusException::class)
    fun cancelOrder(eid: String, iccid: String?, matchingId: String?, finalProfileStatusIndicator: String?)

    @Throws(SmDpPlusException::class)
    fun releaseProfile(iccid: String)
}

interface  SmDpPlusCallbackService {
    @Throws(SmDpPlusException::class)
    fun handleDownloadProgressInfo(
            eid: String?,
            iccid: String?,
            notificationPointId: String?,
            profileType: String?,
            resultData: ES2StatusCodeData?, timestamp: String?)
}
