package org.ostelco.prime.ocs.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.FinalUnitAction
import org.ostelco.ocs.api.FinalUnitIndication
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.ReportingReason
import org.ostelco.ocs.api.ResultCode
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.analytics.AnalyticsReporter
import org.ostelco.prime.ocs.consumption.OcsAsyncRequestConsumer
import org.ostelco.prime.ocs.notifications.Notifications
import org.ostelco.prime.storage.ClientDataSource

object OnlineCharging : OcsAsyncRequestConsumer {

    var loadUnitTest = false
    private val loadAcceptanceTest = System.getenv("LOAD_TESTING") == "true"

    private val storage: ClientDataSource = getResource()

    override fun creditControlRequestEvent(
            request: CreditControlRequestInfo,
            returnCreditControlAnswer: (CreditControlAnswerInfo) -> Unit) {

        val msisdn = request.msisdn

        if (msisdn != null) {

            CoroutineScope(Dispatchers.Default).launch {

                val response = CreditControlAnswerInfo.newBuilder()
                        .setRequestId(request.requestId)
                        .setMsisdn(msisdn)
                        .setResultCode(ResultCode.DIAMETER_SUCCESS)

                if (request.msccCount > 0) {
                    val mscc = request.getMscc(0)
                    val requested = mscc?.requested?.totalOctets ?: 0
                    val used = mscc?.used?.totalOctets ?: 0

                    val responseMscc = MultipleServiceCreditControl
                            .newBuilder(mscc)
                            .setValidityTime(86400)


                    storage.consume(msisdn, used, requested) { storeResult ->
                        storeResult.fold(
                            {
                                // TODO martin : Should we handle all errors as NotFoundError?
                                response.resultCode = ResultCode.DIAMETER_USER_UNKNOWN
                                synchronized(OnlineCharging) {
                                    returnCreditControlAnswer(response.build())
                                }
                            },
                            {
                                consumptionResult ->

                                val grantedTotalOctets = if (mscc.reportingReason != ReportingReason.FINAL
                                        && mscc.requested.totalOctets > 0) {

                                    if (consumptionResult.granted < mscc.requested.totalOctets) {
                                        responseMscc.finalUnitIndication = FinalUnitIndication.newBuilder()
                                                .setFinalUnitAction(FinalUnitAction.TERMINATE)
                                                .setIsSet(true)
                                                .build()
                                    }

                                    consumptionResult.granted

                                } else {
                                    // Use -1 to indicate no granted service unit should be included in the answer
                                    -1
                                }

                                responseMscc.granted = ServiceUnit.newBuilder().setTotalOctets(grantedTotalOctets).build()

                                if (grantedTotalOctets > 0) {
                                    responseMscc.quotaHoldingTime = 7200
                                    responseMscc.volumeQuotaThreshold = (grantedTotalOctets * 0.8).toLong() // 80%
                                }

                                responseMscc.resultCode = ResultCode.DIAMETER_SUCCESS

                                if (!loadUnitTest && !loadAcceptanceTest) {
                                    launch {
                                        AnalyticsReporter.report(
                                                msisdnAnalyticsId = consumptionResult.msisdnAnalyticsId,
                                                request = request,
                                                bundleBytes = consumptionResult.balance)
                                    }

                                    launch {
                                        Notifications.lowBalanceAlert(
                                                msisdn = msisdn,
                                                reserved = consumptionResult.granted,
                                                balance = consumptionResult.balance)
                                    }
                                }
                                response.addMscc(responseMscc)
                                synchronized(OnlineCharging) {
                                    returnCreditControlAnswer(response.build())
                                }
                            })
                    }
                }
                else {
                    synchronized(OnlineCharging) {
                        returnCreditControlAnswer(response.build())
                    }
                }
            }
        }
    }
}