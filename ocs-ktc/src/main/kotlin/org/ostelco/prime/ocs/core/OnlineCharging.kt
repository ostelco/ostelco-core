package org.ostelco.prime.ocs.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ostelco.ocs.api.*
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.analytics.AnalyticsReporter
import org.ostelco.prime.ocs.consumption.OcsAsyncRequestConsumer
import org.ostelco.prime.storage.ClientDataSource
import org.ostelco.prime.storage.ConsumptionResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


object OnlineCharging : OcsAsyncRequestConsumer {

    var loadUnitTest = false
    private val loadAcceptanceTest = System.getenv("LOAD_TESTING") == "true"

    private val storage: ClientDataSource = getResource()

    private val logger by getLogger()

    override fun creditControlRequestEvent(
            request: CreditControlRequestInfo,
            returnCreditControlAnswer: (CreditControlAnswerInfo) -> Unit) {

        val msisdn = request.msisdn

        if (msisdn != null) {

            val responseBuilder = CreditControlAnswerInfo.newBuilder()

            // these are keepalives to keep latency low
            if (msisdn.equals("keepalive")) {
                responseBuilder.setRequestId(request.requestId).setMsisdn("keepalive").setResultCode(ResultCode.UNKNOWN)
                returnCreditControlAnswer(responseBuilder.buildPartial())
            } else {

                CoroutineScope(Dispatchers.Default).launch {

                    responseBuilder.setRequestId(request.requestId)
                            .setMsisdn(msisdn).setResultCode(ResultCode.DIAMETER_SUCCESS)

                    val doneSignal = CountDownLatch(request.msccList.size)

                    request.msccList.forEach { mscc ->

                        val requested = mscc.requested?.totalOctets ?: 0
                        val used = mscc.used?.totalOctets ?: 0
                        if (shouldConsume(mscc.ratingGroup, mscc.serviceIdentifier)) {
                            storage.consume(msisdn, used, requested) { storeResult ->
                                storeResult.fold(
                                        {
                                            responseBuilder.resultCode = ResultCode.DIAMETER_USER_UNKNOWN
                                            doneSignal.countDown()
                                        },
                                        { consumptionResult ->
                                            if (requested > 0) {
                                                addGrantedQuota(consumptionResult.granted, mscc, responseBuilder)
                                                addInfo(consumptionResult.balance, mscc, responseBuilder)
                                            }
                                            reportAnalytics(consumptionResult, request)
                                            doneSignal.countDown()
                                        }
                                )
                            }
                        } else { // zeroRate
                            if (requested > 0) {
                                addGrantedQuota(requested, mscc, responseBuilder)
                                // adding by 100 just to set it high
                                addInfo(mscc.requested.totalOctets * 100, mscc, responseBuilder)
                            }
                            doneSignal.countDown()
                        }
                    }

                    doneSignal.await(2, TimeUnit.SECONDS)

                    if (responseBuilder.msccCount == 0) {
                        responseBuilder.setValidityTime(86400)
                    }

                    synchronized(OnlineCharging) {
                        returnCreditControlAnswer(responseBuilder.build())
                    }
                }
            }
        }
    }

    private fun reportAnalytics(consumptionResult : ConsumptionResult, request: CreditControlRequestInfo) {
        if (!loadUnitTest && !loadAcceptanceTest) {
            CoroutineScope(Dispatchers.Default).launch {
                AnalyticsReporter.report(
                        msisdnAnalyticsId = consumptionResult.msisdnAnalyticsId,
                        request = request,
                        bundleBytes = consumptionResult.balance)
            }

            // FIXME vihang: get customerId for MSISDN
            /*launch {
            Notifications.lowBalanceAlert(
                customerId = msisdn,
                reserved = consumptionResult.granted,
                balance = consumptionResult.balance)
        }*/
        }
    }

    private fun addInfo(balance: Long, mscc: MultipleServiceCreditControl, response: CreditControlAnswerInfo.Builder) {
        response.extraInfoBuilder.addMsccInfo(
                MultipleServiceCreditControlInfo
                        .newBuilder()
                        .setBalance(balance)
                        .setRatingGroup(mscc.ratingGroup)
                        .setServiceIdentifier(mscc.serviceIdentifier)
                        .build()
        )
    }

    private fun addGrantedQuota(granted: Long, mscc: MultipleServiceCreditControl, response: CreditControlAnswerInfo.Builder) {

        val responseMscc = MultipleServiceCreditControl
                .newBuilder(mscc)
                .setValidityTime(86400)

        val grantedTotalOctets = if (mscc.reportingReason != ReportingReason.FINAL && mscc.requested.totalOctets > 0) {

            granted
        } else {
            // Use -1 to indicate no granted service unit should be included in the answer
            -1
        }

        responseMscc.granted = ServiceUnit.newBuilder().setTotalOctets(grantedTotalOctets).build()

        if (grantedTotalOctets > 0) {

            responseMscc.quotaHoldingTime = 7200

            if (granted < mscc.requested.totalOctets) {
                responseMscc.volumeQuotaThreshold = 0L  // No point in putting a threshold on the last grant
            } else {
                responseMscc.volumeQuotaThreshold = (grantedTotalOctets * 0.2).toLong() // When client has 20% left
            }
            responseMscc.resultCode = ResultCode.DIAMETER_SUCCESS
        } else if (mscc.requested.totalOctets > 0) {
            responseMscc.resultCode = ResultCode.DIAMETER_CREDIT_LIMIT_REACHED
        } else {
            responseMscc.resultCode = ResultCode.DIAMETER_SUCCESS
        }

        synchronized(OnlineCharging) {
            response.addMscc(responseMscc.build())
        }
    }

    private fun shouldConsume(ratingGroup: Long, serviceIdentifier: Long): Boolean {

        // FixMe : Fetch list from somewhere ™
        // For now hardcoded to known combinations

        if (arrayOf(600L).contains(ratingGroup)) {
            return true
        }

        if (arrayOf(1L, 400L).contains(serviceIdentifier)) {
            return true
        }

        return false
    }
}