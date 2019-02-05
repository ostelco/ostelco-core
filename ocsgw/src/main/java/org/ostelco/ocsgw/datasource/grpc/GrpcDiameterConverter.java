package org.ostelco.ocsgw.datasource.grpc;

import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.model.*;
import org.ostelco.ocs.api.CreditControlRequestInfo;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.ostelco.ocs.api.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.ostelco.diameter.model.RequestType.*;

class GrpcDiameterConverter {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcDataSource.class);

    /**
     * Convert MultipleServiceCreditControl in gRPC format to diameter format
     */
    static MultipleServiceCreditControl convertMSCC(org.ostelco.ocs.api.MultipleServiceCreditControl msccGRPC) {
        return new MultipleServiceCreditControl(
                msccGRPC.getRatingGroup(),
                (int) msccGRPC.getServiceIdentifier(),
                Collections.singletonList(new ServiceUnit()), new ServiceUnit(), new ServiceUnit(msccGRPC.getGranted().getTotalOctets(), 0, 0),
                msccGRPC.getValidityTime(),
                convertFinalUnitIndication(msccGRPC.getFinalUnitIndication()),
                convertResultCode(msccGRPC.getResultCode()));
    }

    /**
     * Convert Diameter request type to gRPC
     */
    static CreditControlRequestType getRequestType(CreditControlContext context) {
        switch (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()) {
            case INITIAL_REQUEST:
                return CreditControlRequestType.INITIAL_REQUEST;
            case UPDATE_REQUEST:
                return CreditControlRequestType.UPDATE_REQUEST;
            case TERMINATION_REQUEST:
                return CreditControlRequestType.TERMINATION_REQUEST;
            case EVENT_REQUEST:
                return CreditControlRequestType.EVENT_REQUEST;
            default:
                LOG.warn("Unknown request type");
                return CreditControlRequestType.NONE;
        }
    }

    private static FinalUnitIndication convertFinalUnitIndication(org.ostelco.ocs.api.FinalUnitIndication fuiGrpc) {
        if (!fuiGrpc.getIsSet()) {
            return null;
        }
        return new FinalUnitIndication(
                FinalUnitAction.values()[fuiGrpc.getFinalUnitAction().getNumber()],
                fuiGrpc.getRestrictionFilterRuleList(),
                fuiGrpc.getFilterIdList(),
                new RedirectServer(
                        RedirectAddressType.values()[fuiGrpc.getRedirectServer().getRedirectAddressType().getNumber()],
                        fuiGrpc.getRedirectServer().getRedirectServerAddress()
                )
        );
    }

    // We match the error codes on names in gRPC and internal model
    static ResultCode convertResultCode(org.ostelco.ocs.api.ResultCode resultCode) {
        return ResultCode.valueOf(resultCode.name());
    }

    static CreditControlRequestInfo convertRequestToGrpc(final CreditControlContext context) {

        try {
            CreditControlRequestInfo.Builder builder = CreditControlRequestInfo
                    .newBuilder()
                    .setType(getRequestType(context));

            for (MultipleServiceCreditControl mscc : context.getCreditControlRequest().getMultipleServiceCreditControls()) {

                org.ostelco.ocs.api.MultipleServiceCreditControl.Builder protoMscc = org.ostelco.ocs.api.MultipleServiceCreditControl.newBuilder();

                if (!mscc.getRequested().isEmpty()) {

                    org.ostelco.diameter.model.ServiceUnit requested = mscc.getRequested().get(0);

                    protoMscc.setRequested(org.ostelco.ocs.api.ServiceUnit.newBuilder()
                            .setInputOctets(0L)
                            .setOutputOctetes(0L)
                            .setTotalOctets(requested.getTotal())
                            .build());
                }

                org.ostelco.diameter.model.ServiceUnit used = mscc.getUsed();

                protoMscc.setUsed(org.ostelco.ocs.api.ServiceUnit.newBuilder()
                        .setInputOctets(used.getInput())
                        .setOutputOctetes(used.getOutput())
                        .setTotalOctets(used.getTotal())
                        .build());

                protoMscc.setRatingGroup(mscc.getRatingGroup());
                protoMscc.setServiceIdentifier(mscc.getServiceIdentifier());

                if (mscc.getReportingReason() != null) {
                    protoMscc.setReportingReasonValue(mscc.getReportingReason().ordinal());
                } else {
                    protoMscc.setReportingReasonValue(org.ostelco.ocs.api.ReportingReason.UNRECOGNIZED.ordinal());
                }
                builder.addMscc(protoMscc.build());
            }

            builder.setRequestId(context.getSessionId())
                    .setMsisdn(context.getCreditControlRequest().getMsisdn())
                    .setImsi(context.getCreditControlRequest().getImsi());

            if (!context.getCreditControlRequest().getServiceInformation().isEmpty()) {
                final org.ostelco.diameter.model.PsInformation psInformation
                        = context.getCreditControlRequest().getServiceInformation().get(0).getPsInformation().get(0);

                if (psInformation != null
                        && psInformation.getCalledStationId() != null
                        && psInformation.getSgsnMccMnc() != null) {

                    builder.setServiceInformation(
                            ServiceInfo.newBuilder()
                                    .setPsInformation(org.ostelco.ocs.api.PsInformation.newBuilder()
                                            .setCalledStationId(psInformation.getCalledStationId())
                                            .setSgsnMccMnc(psInformation.getSgsnMccMnc())
                                            .build()).build());
                }
            }
            return builder.build();

        } catch (Exception e) {
            LOG.error("Failed to create CreditControlRequestInfo", e);
        }
        return null;
    }
}
