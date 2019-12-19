package org.ostelco.ocsgw.converter;

import com.google.protobuf.ByteString
import org.ostelco.diameter.CreditControlContext
import org.ostelco.diameter.getLogger
import org.ostelco.diameter.model.FinalUnitAction
import org.ostelco.diameter.model.FinalUnitIndication
import org.ostelco.diameter.model.MultipleServiceCreditControl
import org.ostelco.diameter.model.RedirectAddressType
import org.ostelco.diameter.model.RedirectServer
import org.ostelco.diameter.model.ResultCode
import org.ostelco.diameter.model.ServiceUnit
import org.ostelco.diameter.model.RequestType
import org.ostelco.ocs.api.*
import org.ostelco.ocs.api.PsInformation
import org.ostelco.ocs.api.ReportingReason


class ProtobufToDiameterConverter {

    companion object {

        private val logger by getLogger()

        fun convertRequestToProtobuf(context: CreditControlContext, topicId: String?): CreditControlRequestInfo? {
            try {
                val builder = CreditControlRequestInfo
                        .newBuilder()
                        .setType(getRequestType(context))
                if (topicId != null) {
                    builder.topicId = topicId
                }
                builder.requestNumber = context.creditControlRequest.ccRequestNumber!!.integer32
                addMultipleServiceCreditControl(context, builder)
                builder.setRequestId(context.sessionId)
                        .setMsisdn(context.creditControlRequest.msisdn)
                        .setImsi(context.creditControlRequest.imsi)
                addPsInformation(context, builder)
                return builder.build()
            } catch (e: Exception) {
                logger.error("Failed to create CreditControlRequestInfo [{}] [{}]", context.creditControlRequest.msisdn, context.sessionId, e)
            }
            return null
        }

        /**
         * Convert MultipleServiceCreditControl in protobuf format to diameter format
         */
        fun convertMSCC(msccGRPC: org.ostelco.ocs.api.MultipleServiceCreditControl): MultipleServiceCreditControl {
            return MultipleServiceCreditControl(
                    msccGRPC.ratingGroup,
                    msccGRPC.serviceIdentifier,
                    listOf(ServiceUnit()),
                    listOf(ServiceUnit()),
                    ServiceUnit(msccGRPC.granted.totalOctets, 0, 0),
                    msccGRPC.validityTime,
                    msccGRPC.quotaHoldingTime,
                    msccGRPC.volumeQuotaThreshold,
                    convertFinalUnitIndication(msccGRPC.finalUnitIndication),
                    convertResultCode(msccGRPC.resultCode))
        }

        // We match the error codes on names in protobuf and internal model
        fun convertResultCode(resultCode: org.ostelco.ocs.api.ResultCode): ResultCode {
            try {
                return ResultCode.valueOf(resultCode.name)
            } catch (e: IllegalArgumentException) {
                logger.warn("Unknown ResultCode $resultCode")
            }
            return ResultCode.DIAMETER_UNABLE_TO_COMPLY
        }

        /**
         * Convert Diameter request type to protobuf
         */
        fun getRequestType(context: CreditControlContext): CreditControlRequestType {
            return when (context.originalCreditControlRequest.requestTypeAVPValue) {
                RequestType.INITIAL_REQUEST -> CreditControlRequestType.INITIAL_REQUEST
                RequestType.UPDATE_REQUEST -> CreditControlRequestType.UPDATE_REQUEST
                RequestType.TERMINATION_REQUEST -> CreditControlRequestType.TERMINATION_REQUEST
                RequestType.EVENT_REQUEST -> CreditControlRequestType.EVENT_REQUEST
                else -> {
                    logger.warn("Unknown request type")
                    CreditControlRequestType.NONE
                }
            }
        }

        private fun addPsInformation(context: CreditControlContext, builder: CreditControlRequestInfo.Builder) {
            if (!context.creditControlRequest.serviceInformation.isEmpty()) {
                val psInformation = context.creditControlRequest.serviceInformation[0].psInformation[0]
                val psInformationBuilder = PsInformation.newBuilder()
                if (psInformation.calledStationId != null) {
                    psInformationBuilder.calledStationId = psInformation.calledStationId
                }
                if (psInformation.sgsnMccMnc != null) {
                    psInformationBuilder.sgsnMccMnc = psInformation.sgsnMccMnc
                }
                if (psInformation.imsiMccMnc != null) {
                    psInformationBuilder.imsiMccMnc = psInformation.imsiMccMnc
                }

                psInformation.userLocationInfo?.let { psInformationBuilder.userLocationInfo = ByteString.copyFrom(it) }

                psInformation.pdpType?.let { psInformationBuilder.pdpType = PdpType.forNumber(it) }

                psInformation.pdpAddress?.let { psInformationBuilder.pdpAddress = ByteString.copyFrom(it.address) }

                builder.setServiceInformation(ServiceInfo.newBuilder().setPsInformation(psInformationBuilder))
            }
        }

        private fun addMultipleServiceCreditControl(context: CreditControlContext, builder: CreditControlRequestInfo.Builder) {
            for (mscc in context.creditControlRequest.multipleServiceCreditControls) {
                val msccBuilder = org.ostelco.ocs.api.MultipleServiceCreditControl.newBuilder()
                if (!mscc.requested.isEmpty()) {
                    val requested = mscc.requested[0]
                    msccBuilder.setRequested(org.ostelco.ocs.api.ServiceUnit.newBuilder()
                            .setTotalOctets(requested.total)
                            .setInputOctets(0L)
                            .setOutputOctets(0L))
                }
                for (used in mscc.used) { // We do not track CC-Service-Specific-Units or CC-Time
                    if (used.total > 0) {
                        msccBuilder.setUsed(org.ostelco.ocs.api.ServiceUnit.newBuilder()
                                .setInputOctets(used.input)
                                .setOutputOctets(used.output)
                                .setTotalOctets(used.total))
                    }
                }
                msccBuilder.ratingGroup = mscc.ratingGroup
                msccBuilder.serviceIdentifier = mscc.serviceIdentifier

                val reportingReason = mscc.reportingReason
                if (reportingReason != null) {
                    msccBuilder.reportingReasonValue = reportingReason.ordinal
                } else {
                    msccBuilder.reportingReasonValue = ReportingReason.UNRECOGNIZED.ordinal
                }
                builder.addMscc(msccBuilder)
            }
        }

        private fun convertFinalUnitIndication(fuiGrpc: org.ostelco.ocs.api.FinalUnitIndication): FinalUnitIndication? {
            return if (!fuiGrpc.isSet) {
                null
            } else FinalUnitIndication(
                    FinalUnitAction.values()[fuiGrpc.finalUnitAction.number],
                    fuiGrpc.restrictionFilterRuleList,
                    fuiGrpc.filterIdList,
                    RedirectServer(
                            RedirectAddressType.values()[fuiGrpc.redirectServer.redirectAddressType.number],
                            fuiGrpc.redirectServer.redirectServerAddress
                    )
            )
        }
    }
}
