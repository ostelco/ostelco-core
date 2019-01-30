package org.ostelco.ocsgw.datasource.grpc;

import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.model.*;
import org.ostelco.ocs.api.CreditControlRequestType;
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
}
