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
                userEquipmentInfo.setUserEquipmentInfoType(set.getAvp(Avp.USER_EQUIPMENT_INFO_TYPE).getInteger32());
                userEquipmentInfo.setGetUserEquipmentInfoValue(set.getAvp(Avp.USER_EQUIPMENT_INFO_VALUE).getOctetString());
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

                Optional<Avp> ratingGroup = Optional.ofNullable(serviceControl.getAvp(Avp.RATING_GROUP));
                if (ratingGroup.isPresent()) {
                    mscc.setRatingGroup(ratingGroup.get().getInteger32());
                }

                Optional<Avp> serviceIdentifier = Optional.ofNullable(serviceControl.getAvp(Avp.SERVICE_IDENTIFIER));
                if (serviceIdentifier.isPresent()) {
                    mscc.setServiceIdentifier(serviceIdentifier.get().getInteger32());
                }

                Optional<Avp> requestedServiceUnits = Optional.ofNullable(serviceControl.getAvp(Avp.REQUESTED_SERVICE_UNIT));
                if (requestedServiceUnits.isPresent()) {
                    mscc.setRequestedUnits(requestedServiceUnits.get().getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
                }

                Optional<AvpSet> usedServiceUnitsSet = Optional.ofNullable(serviceControl.getAvps(Avp.USED_SERVICE_UNIT));
                if (usedServiceUnitsSet.isPresent()) {
                    for (Avp nextUsedServiceUnit : usedServiceUnitsSet.get()) {
                        AvpSet usedServiceUnit = nextUsedServiceUnit.getGrouped();

                        Optional<Avp> totalOctets = Optional.ofNullable(usedServiceUnit.getAvp(Avp.CC_TOTAL_OCTETS));
                        if (totalOctets.isPresent()) {
                            mscc.setUsedUnitsTotal(totalOctets.get().getUnsigned64());
                        }

                        Optional<Avp> InputOctets = Optional.ofNullable(usedServiceUnit.getAvp(Avp.CC_INPUT_OCTETS));
                        if (InputOctets.isPresent()) {
                            mscc.setUsedUnitsInput(InputOctets.get().getUnsigned64());
                        }

                        Optional<Avp> outputOctets = Optional.ofNullable(usedServiceUnit.getAvp(Avp.CC_OUTPUT_OCTETS));
                        if (outputOctets.isPresent()) {
                            mscc.setUsedUnitsOutput(outputOctets.get().getUnsigned64());
                        }
                    }
                }
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
                Optional<Avp> psInformationAvp = Optional.ofNullable(serviceInformationAvp.get().getGrouped().getAvp(Avp.PS_INFORMATION));
                if (psInformationAvp.isPresent()) {
                    PsInformation psInformation = this.serviceInformation.getPsinformation();
                    AvpSet psInformationAvps = psInformationAvp.get().getGrouped();
                    Optional<byte[]> chargingId = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_CHARGING_ID).getOctetString());
                    if (chargingId.isPresent()) {
                        psInformation.setChargingId(chargingId.get());
                    } else {
                        psInformation.setCalledStationId(null);
                    }
                    Optional<Avp> pdpType = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_PDP_TYPE));
                    if (pdpType.isPresent()) {
                        psInformation.setPdpType(psInformationAvps.getAvp(Avp.TGPP_PDP_TYPE).getInteger32());
                    } else {
                        psInformation.setPdpType(0);
                    }
                    psInformation.setPdpAddress(psInformationAvps.getAvp(Avp.PDP_ADDRESS).getAddress());
                    psInformation.setSgsnAddress(psInformationAvps.getAvp(Avp.SGSN_ADDRESS).getAddress());
                    psInformation.setGgsnAddress(psInformationAvps.getAvp(Avp.GGSN_ADDRESS).getAddress());
                    psInformation.setImsiMccMnc(psInformationAvps.getAvp(Avp.TGPP_IMSI_MCC_MNC).getUTF8String());
                    psInformation.setGgsnMccMnc(psInformationAvps.getAvp(Avp.TGPP_GGSN_MCC_MNC).getUTF8String());
                    psInformation.setCalledStationId(psInformationAvps.getAvp(30).getUTF8String()); // CALLED_STATION_ID (Avp 30)
                    psInformation.setSelectionMode(psInformationAvps.getAvp(Avp.TGPP_SELECTION_MODE).getUTF8String());
                    psInformation.setChargingCharacteristics(psInformationAvps.getAvp(Avp.TGPP_CHARGING_CHARACTERISTICS).getUTF8String());
                    psInformation.setSgsnMncMcc(psInformationAvps.getAvp(Avp.GPP_SGSN_MCC_MNC).getUTF8String());
                    psInformation.setMsTimezone(psInformationAvps.getAvp(Avp.TGPP_MS_TIMEZONE).getOctetString());
                    psInformation.setChargingRulebaseName(psInformationAvps.getAvp(Avp.CHARGING_RULE_BASE_NAME).getUTF8String());
                    psInformation.setRatType(psInformationAvps.getAvp(Avp.TGPP_RAT_TYPE).getOctetString());
                } else {
                    logger.info("No PS-Information");
                }
            } else {
                logger.info("No Service-Information");
            }
        } catch (AvpDataException | NullPointerException e) {
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
