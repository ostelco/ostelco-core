package com.telenordigital.ocsgw.diameter;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Optional;

public class CreditControlRequest {

    private static final Logger logger = LoggerFactory.getLogger(CreditControlRequestContext.class);
    private AvpSet ccrAvps;

    private final LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = new LinkedList<>();
    private final ServiceInformation serviceInformation = new ServiceInformation();
    private final UserEquipmentInfo userEquipmentInfo = new UserEquipmentInfo();
    private String msisdn = null;
    private String imsi = null;
    private Avp ccRequestType;
    private Avp ccRequestNumber;

    public CreditControlRequest(JCreditControlRequest request) {
        parseRequest(request);
    }

    public Avp getCcRequestType() {
        return ccRequestType;
    }

    public Avp getCcRequestNumber() {
        return ccRequestNumber;
    }

    public LinkedList<MultipleServiceCreditControl> getMultipleServiceCreditControls() {
        return multipleServiceCreditControls;
    }

    public ServiceInformation getServiceInformation() {
        return serviceInformation;
    }

    private void parseRequest(JCreditControlRequest request) {
        try {
            ccrAvps = request.getMessage().getAvps();
            ccRequestType = ccrAvps.getAvp(Avp.CC_REQUEST_TYPE);
            ccRequestNumber = ccrAvps.getAvp(Avp.CC_REQUEST_NUMBER);
            parseMultipleServiceCreditControl();
            parseSubscriptionId();
            parseServiceInformation();
            parseUserEquipmentInfo();
        } catch (InternalException e) {
            logger.error("Failed to parse CCR", e);
        }
    }

    // User-Equipment-Info (AVP 458)
    private void parseUserEquipmentInfo() {
        try {
            Optional<Avp> requestUserEquipmentInfo = Optional.ofNullable(ccrAvps.getAvp(Avp.USER_EQUIPMENT_INFO));
            if (requestUserEquipmentInfo.isPresent()) {
                AvpSet set = requestUserEquipmentInfo.get().getGrouped();
                userEquipmentInfo.parseAvp(set);
            }
        } catch (AvpDataException e) {
            logger.error("Failed to parse User-Equipment-Info", e);
        }
    }

    // Multiple-Service-Credit-Control (AVP 456)
    private void parseMultipleServiceCreditControl() {
        try {
            AvpSet requestMsccSet = ccrAvps.getAvps(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            for (Avp msccAvp : requestMsccSet) {
                AvpSet serviceControl = msccAvp.getGrouped();

                MultipleServiceCreditControl mscc = new MultipleServiceCreditControl();
                mscc.parseAvps(serviceControl);

                multipleServiceCreditControls.add(mscc);
            }
        } catch (AvpDataException e) {
            logger.error("parseMultipleServiceCreditControl failed ", e);
        }
    }

    // Subscription-Id (AVP 443)
    private void parseSubscriptionId() {

        try {
            AvpSet subscriptionAvps = ccrAvps.getAvps(Avp.SUBSCRIPTION_ID);

            for (Avp sidP : subscriptionAvps) {
                AvpSet sid = sidP.getGrouped();
                int subscriptionType = sid.getAvp(Avp.SUBSCRIPTION_ID_TYPE).getInteger32();

                switch (subscriptionType) {
                    case SubscriptionType.END_USER_E164:
                        msisdn = sid.getAvp(Avp.SUBSCRIPTION_ID_DATA).getUTF8String();
                        break;
                    case SubscriptionType.END_USER_IMSI:
                        imsi = sid.getAvp(Avp.SUBSCRIPTION_ID_DATA).getUTF8String();
                        break;
                    case SubscriptionType.END_USER_SIP_URI:
                    case SubscriptionType.END_USER_NAI:
                    case SubscriptionType.END_USER_PRIVATE:
                    default:
                        break;
                }
            }
        } catch (AvpDataException e) {
            logger.error("parseSubscriptionId failed", e);
        }
    }

    // Service-Information (AVP 873)
    private void parseServiceInformation() {
        try {
            Optional<Avp> serviceInformationAvp = Optional.ofNullable(ccrAvps.getAvp(Avp.SERVICE_INFORMATION));
            if (serviceInformationAvp.isPresent()) {
                serviceInformation.parseApvs(serviceInformationAvp.get());
            } else {
                logger.info("No Service-Information");
            }
        } catch (NullPointerException e) {
            logger.error("Failed to parse ServiceInformation" , e);
        }
    }

    public long getRequestedUnits() {
        // ToDo: This should be connected to rating groups
        return this.multipleServiceCreditControls.getFirst().getRequestedUnits();
    }

    public long getUsedUnits() {
        // ToDo: This only get the total. There is also input/output if needed
        return this.multipleServiceCreditControls.getFirst().getUsedUnitsTotal();
    }

    public String getMsisdn() {
        return msisdn;
    }

    public String getImsi() {
        return imsi;
    }
}
