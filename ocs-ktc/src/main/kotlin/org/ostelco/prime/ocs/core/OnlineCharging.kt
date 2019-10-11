package org.ostelco.prime.ocs.core

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.MultipleServiceCreditControlInfo
import org.ostelco.ocs.api.ReportingReason
import org.ostelco.ocs.api.ResultCode
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.analytics.AnalyticsReporter
import org.ostelco.prime.ocs.consumption.OcsAsyncRequestConsumer
import org.ostelco.prime.ocs.notifications.Notifications
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.ConsumptionResult
import org.ostelco.prime.storage.StoreError
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


object OnlineCharging : OcsAsyncRequestConsumer {

    var loadUnitTest = false
    private val loadAcceptanceTest = System.getenv("LOAD_TESTING") == "true"

    private val storage: AdminDataSource = getResource()

    private val logger by getLogger()

    override fun creditControlRequestEvent(
            request: CreditControlRequestInfo,
            returnCreditControlAnswer: (CreditControlAnswerInfo) -> Unit) {

        val msisdn = request.msisdn

        if (msisdn != null) {

            val responseBuilder = CreditControlAnswerInfo.newBuilder()

            responseBuilder.setRequestNumber(request.requestNumber)

            // these are keepalives to keep latency low
            if (msisdn == "keepalive") {
                responseBuilder.setRequestId(request.requestId).setMsisdn("keepalive").resultCode = ResultCode.UNKNOWN
                returnCreditControlAnswer(responseBuilder.buildPartial())
            } else {

                CoroutineScope(Dispatchers.Default).launch {

                    responseBuilder.setRequestId(request.requestId)
                            .setMsisdn(msisdn).setResultCode(ResultCode.DIAMETER_SUCCESS)

                    if (request.msccCount == 0) {
                        responseBuilder.validityTime = 86400
                        storage.consume(msisdn, 0L, 0L) {
                            storeResult -> storeResult.fold(
                                {responseBuilder.resultCode = ResultCode.DIAMETER_USER_UNKNOWN},
                                {responseBuilder.resultCode = ResultCode.DIAMETER_SUCCESS})
                        }
                    } else {
                        val doneSignal = CountDownLatch(request.msccList.size)
                        request.msccList.forEach { mscc ->

                            val requested = mscc.requested?.totalOctets ?: 0
                            if (requested > 0) {
                                charge(msisdn, mscc, request.serviceInformation.psInformation.sgsnMccMnc) { storeResult ->
                                    storeResult.fold(
                                            {
                                                // FixMe
                                                responseBuilder.resultCode = ResultCode.DIAMETER_USER_UNKNOWN
                                                doneSignal.countDown()
                                            },
                                            { consumptionResult ->
                                                addGrantedQuota(consumptionResult.granted, mscc, responseBuilder)
                                                addInfo(consumptionResult.balance, mscc, responseBuilder)
                                                reportAnalytics(consumptionResult, request)
                                                Notifications.lowBalanceAlert(msisdn, consumptionResult.granted, consumptionResult.balance)
                                                doneSignal.countDown()
                                            }
                                    )
                                }
                            } else {
                                doneSignal.countDown()
                            }
                        }
                        doneSignal.await(2, TimeUnit.SECONDS)
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
                        subscriptionAnalyticsId = consumptionResult.msisdnAnalyticsId,
                        request = request,
                        bundleBytes = consumptionResult.balance)
            }
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

    private suspend fun charge(msisdn: String, multipleServiceCreditControl: MultipleServiceCreditControl, mccmnc: String, callback: (Either<StoreError, ConsumptionResult>) -> Unit) {

        val requested = multipleServiceCreditControl.requested?.totalOctets ?: 0
        val used = multipleServiceCreditControl.used?.totalOctets ?: 0

        when (Rating.getRate(msisdn, multipleServiceCreditControl.serviceIdentifier, multipleServiceCreditControl.ratingGroup, mccmnc)) {
            Rating.Rate.ZERO -> callback(ConsumptionResult(msisdn, multipleServiceCreditControl.requested.totalOctets, multipleServiceCreditControl.requested.totalOctets * 100).right())
            Rating.Rate.NORMAL -> storage.consume(msisdn, used, requested, callback)
            Rating.Rate.BLOCKED -> callback(ConsumptionResult(msisdn, 0L, 0L).right())
        }
    }
}