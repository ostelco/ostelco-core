package com.telenordigital.ocsgw.diameter;

// https://tools.ietf.org/html/rfc4006#section-8.16

import com.telenordigital.ocsgw.diameter.FinalUnitIndication;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MultipleServiceCreditControl {
    private int ratingGroup = -1;
    private int serviceIdentifier = -1;

    private long requestedUnits = 0L;
    private long usedUnitsTotal = 0L;
    private long usedUnitsInput = 0L;
    private long usedUnitsOutput = 0L;
    private long reservedUnits = 0L;
    private long grantedServiceUnit = 0L;
    private int validityTime = 86400;
    private FinalUnitIndication finalUnitIndication;

    private static final Logger LOG = LoggerFactory.getLogger(MultipleServiceCreditControl.class);


    public long getRequestedUnits() {
        return requestedUnits;
    }

    public long getReservedUnits() {
        return reservedUnits;
    }

    public long getUsedUnitsTotal() {
        return usedUnitsTotal;
    }

    public long getUsedUnitsInput() {
        return usedUnitsInput;
    }

    public long getUsedUnitsOutput() {
        return usedUnitsOutput;
    }

    public int getRatingGroup() {
        return ratingGroup;
    }

    public void setRatingGroup(int ratingGroup) {
        this.ratingGroup = ratingGroup;
    }

    public void setServiceIdentifier(int serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public int getServiceIdentifier() {
        return serviceIdentifier;
    }

    public long getGrantedServiceUnit() {
        return grantedServiceUnit;
    }

    public int getValidityTime() {
        return validityTime;
    }

    public void setValidityTime(int validityTime) {
        this.validityTime = validityTime;
    }


    public void parseAvps(AvpSet serviceControl) {
        try {
            Optional<Avp> ratingGroup = Optional.ofNullable(serviceControl.getAvp(Avp.RATING_GROUP));
            if (ratingGroup.isPresent()) {
                this.ratingGroup = ratingGroup.get().getInteger32();
            }

            Optional<Avp> serviceIdentifier = Optional.ofNullable(serviceControl.getAvp(Avp.SERVICE_IDENTIFIER_CCA));
            if (serviceIdentifier.isPresent()) {
                this.serviceIdentifier = serviceIdentifier.get().getInteger32();
            }

            Optional<Avp> requestedServiceUnits = Optional.ofNullable(serviceControl.getAvp(Avp.REQUESTED_SERVICE_UNIT));
            if (requestedServiceUnits.isPresent()) {
                this.requestedUnits = requestedServiceUnits.get().getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64();
            }

            parseUsedServiceUnit(serviceControl);
        } catch (AvpDataException e) {
            LOG.warn("Failed to parse Multiple-Service-Credit-Control", e);
        }
    }

    private void parseUsedServiceUnit(AvpSet serviceControl) {
        try {
            Optional<AvpSet> usedServiceUnitsSet = Optional.ofNullable(serviceControl.getAvps(Avp.USED_SERVICE_UNIT));
            if (usedServiceUnitsSet.isPresent()) {
                for (Avp nextUsedServiceUnit : usedServiceUnitsSet.get()) {
                    AvpSet usedServiceUnit = nextUsedServiceUnit.getGrouped();

                    Optional<Avp> totalOctets = Optional.ofNullable(usedServiceUnit.getAvp(Avp.CC_TOTAL_OCTETS));
                    if (totalOctets.isPresent()) {
                        this.usedUnitsTotal = totalOctets.get().getUnsigned64();
                    }

                    Optional<Avp> InputOctets = Optional.ofNullable(usedServiceUnit.getAvp(Avp.CC_INPUT_OCTETS));
                    if (InputOctets.isPresent()) {
                        this.usedUnitsInput = InputOctets.get().getUnsigned64();
                    }

                    Optional<Avp> outputOctets = Optional.ofNullable(usedServiceUnit.getAvp(Avp.CC_OUTPUT_OCTETS));
                    if (outputOctets.isPresent()) {
                        this.usedUnitsOutput = outputOctets.get().getUnsigned64();
                    }
                }
            }
        } catch (AvpDataException e) {
            LOG.warn("Failed to parse Used-Service-Unit", e);
        }
    }

    @Override
    public String toString() {
        return "MultipleServiceCreditControl[" +
                "; Requested-Service-Unit=" + requestedUnits +
                "; Reserved-Service-Unit=" + reservedUnits +
                "; usedUnitsTotal=" + usedUnitsTotal +
                "; usedUnitsInput=" + usedUnitsInput +
                "; usedUnitsOutput=" + usedUnitsOutput +
                "; Rating-Group=" + ratingGroup +
                "; Service-Identifier=" + serviceIdentifier +
                "; Granted-Service-Unit" + grantedServiceUnit +
                "; Validity-Time" + validityTime +
                "]";
    }

    public void setGrantedServiceUnit(long bytes) {
        this.grantedServiceUnit = bytes;
    }

    public void setFinalUnitIndication(com.telenordigital.ocsgw.diameter.FinalUnitIndication finalUnitIndication) {
        this.finalUnitIndication = finalUnitIndication;
    }
}
