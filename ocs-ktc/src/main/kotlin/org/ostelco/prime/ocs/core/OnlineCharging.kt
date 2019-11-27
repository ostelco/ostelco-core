package org.ostelco.prime.ocs.core

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
import org.ostelco.prime.ocs.ConfigRegistry
import org.ostelco.prime.ocs.analytics.AnalyticsReporter
import org.ostelco.prime.ocs.consumption.OcsAsyncRequestConsumer
import org.ostelco.prime.ocs.notifications.Notifications
import org.ostelco.prime.ocs.parser.UserLocationParser
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.ConsumptionResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExperimentalUnsignedTypes
object OnlineCharging : OcsAsyncRequestConsumer {

    var loadUnitTest = false
    val keepAliveMsisdn = "keepalive"
    private val loadAcceptanceTest = System.getenv("LOAD_TESTING") == "true"

    private val logger by getLogger()

    private val storage: AdminDataSource = getResource()
    private val consumptionPolicy by lazy {
        ConfigRegistry.config.consumptionPolicyService.getKtsService<ConsumptionPolicy>()
    }

    override fun creditControlRequestEvent(
            request: CreditControlRequestInfo,
            returnCreditControlAnswer: (CreditControlAnswerInfo) -> Unit) {

        val msisdn = request.msisdn

        if (msisdn != null) {
            if (isKeepAlive(request)) {
                handleKeepAlive(request, returnCreditControlAnswer)
            } else {
                chargeRequest(request, msisdn, returnCreditControlAnswer)
            }
        }
    }


    private fun isKeepAlive(request: CreditControlRequestInfo): Boolean = request.msisdn == keepAliveMsisdn

    private fun handleKeepAlive(request: CreditControlRequestInfo, returnCreditControlAnswer: (CreditControlAnswerInfo) -> Unit) {
        val responseBuilder = CreditControlAnswerInfo.newBuilder()
                .setRequestNumber(request.requestNumber)
                .setRequestId(request.requestId)
                .setMsisdn(keepAliveMsisdn)
                .setResultCode(ResultCode.UNKNOWN)

        returnCreditControlAnswer(responseBuilder.buildPartial())
    }

    private fun chargeRequest(request: CreditControlRequestInfo,
                              msisdn: String,
                              returnCreditControlAnswer: (CreditControlAnswerInfo) -> Unit) {

        CoroutineScope(Dispatchers.Default).launch {

            val responseBuilder = CreditControlAnswerInfo.newBuilder()
            responseBuilder.requestNumber = request.requestNumber
            responseBuilder.setRequestId(request.requestId)
                    .setMsisdn(msisdn)
                    .resultCode = ResultCode.DIAMETER_SUCCESS

            if (request.msccCount == 0) {
                responseBuilder.validityTime = 86400
                storage.consume(msisdn, 0L, 0L) { storeResult ->
                    responseBuilder.resultCode = storeResult.fold(
                            { ResultCode.DIAMETER_USER_UNKNOWN },
                            { ResultCode.DIAMETER_SUCCESS })
                    sendCreditControlAnswer(returnCreditControlAnswer, responseBuilder)
                }
            } else {
                chargeMSCCs(request, msisdn, responseBuilder)
                sendCreditControlAnswer(returnCreditControlAnswer, responseBuilder)
            }
        }
    }

    private fun sendCreditControlAnswer(returnCreditControlAnswer: (CreditControlAnswerInfo) -> kotlin.Unit,
                                        responseBuilder: CreditControlAnswerInfo.Builder) {
        synchronized(OnlineCharging) {
            returnCreditControlAnswer(responseBuilder.build())
        }
    }

    private suspend fun chargeMSCCs(request: CreditControlRequestInfo,
                                    msisdn: String,
                                    responseBuilder: CreditControlAnswerInfo.Builder) {

        val doneSignal = CountDownLatch(request.msccList.size)

        var reservationCounter = 0

        request.msccList.forEach { mscc ->

            fun consumptionResultHandler(consumptionResult: ConsumptionResult) {
                addGrantedQuota(consumptionResult.granted, mscc, responseBuilder)
                addInfo(consumptionResult.balance, mscc, responseBuilder)
                reportAnalytics(consumptionResult, request)
                Notifications.lowBalanceAlert(msisdn, consumptionResult.granted, consumptionResult.balance)
                reservationCounter++
                doneSignal.countDown()
            }

            suspend fun consumeRequestHandler(consumptionRequest: ConsumptionRequest) {
                storage.consume(
                        msisdn = consumptionRequest.msisdn,
                        usedBytes = consumptionRequest.usedBytes,
                        requestedBytes = consumptionRequest.requestedBytes) { storeResult ->

                    storeResult
                            .fold(
                                    { storeError ->
                                        // FixMe : should all store errors be unknown user?
                                        logger.error(storeError.message)
                                        responseBuilder.resultCode = ResultCode.DIAMETER_USER_UNKNOWN
                                        doneSignal.countDown()
                                    },
                                    { consumptionResult ->  consumptionResultHandler(consumptionResult) }
                            )
                }
            }

            val requested = mscc.requested?.totalOctets ?: 0
            if (requested > 0) {
                consumptionPolicy.checkConsumption(
                        msisdn = msisdn,
                        multipleServiceCreditControl = mscc,
                        sgsnMccMnc = getUserLocationMccMnc(request),
                        apn = request.serviceInformation.psInformation.calledStationId,
                        imsiMccMnc = request.serviceInformation.psInformation.imsiMccMnc)
                        .bimap(
                                { consumptionResult -> consumptionResultHandler(consumptionResult) },
                                { consumptionRequest ->  consumeRequestHandler(consumptionRequest) }
                        )
            } else {
                doneSignal.countDown()
            }
        }
        doneSignal.await(2, TimeUnit.SECONDS)

        // In case there was no granted reservations the Validity-Time is set on base level, else it is set in each MSCC
        if (reservationCounter == 0) {
            responseBuilder.validityTime = 86400
        }
    }

    private fun getUserLocationMccMnc(request: CreditControlRequestInfo) : String {

        val sgsnMccMnc: String? = request.serviceInformation.psInformation.sgsnMccMnc?.trim()
        if (sgsnMccMnc != null && sgsnMccMnc.length >= 3) {
            return sgsnMccMnc
        }

        val userLocationInfo = UserLocationParser.getParsedUserLocation(request.serviceInformation.psInformation.userLocationInfo.toByteArray())
        if (userLocationInfo != null) {
            return userLocationInfo.mcc + userLocationInfo.mnc
        }

        return ""
    }

    private fun reportAnalytics(consumptionResult: ConsumptionResult, request: CreditControlRequestInfo) {
        if (!loadUnitTest && !loadAcceptanceTest) {
            CoroutineScope(Dispatchers.Default).launch {
                AnalyticsReporter.report(
                        subscriptionAnalyticsId = consumptionResult.msisdnAnalyticsId,
                        request = request,
                        bundleBytes = consumptionResult.balance,
                        mccMnc = getUserLocationMccMnc(request))
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
}
