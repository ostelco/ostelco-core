package com.telenordigital.ocsgw.diameter;

// https://tools.ietf.org/html/rfc4006#section-8.16

public class MultipleServiceCreditControl {
    private long ratingGroup = -1;
    private int serviceIdentifier = -1;

    private long requestedUnits = 0;
    private long usedUnitsTotal = 0;
    private long usedUnitsInput = 0;
    private long usedUnitsOutput = 0;
    private long reservedUnits = 0;
    private long grantedServiceUnit = 0;


    public long getRequestedUnits() {
        return requestedUnits;
    }
    public void setRequestedUnits(long requestedUnits) {
        this.requestedUnits = requestedUnits;
    }

    public long getReservedUnits() {
        return reservedUnits;
    }
    public void setReservedUnits(long reservedUnits) {
        this.reservedUnits = reservedUnits;
    }

    public long getUsedUnitsTotal() { return usedUnitsTotal; }
    public void setUsedUnitsTotal(long usedUnitsTotal) { this.usedUnitsTotal = usedUnitsTotal; }

    public long getUsedUnitsInput() { return usedUnitsInput; }
    public void setUsedUnitsInput(long usedUnitsInput) { this.usedUnitsInput = usedUnitsInput; }

    public long getUsedUnitsOutput() { return usedUnitsOutput; }
    public void setUsedUnitsOutput(long usedUnitsOutput) { this.usedUnitsOutput = usedUnitsOutput; }

    public long getRatingGroup() { return ratingGroup; }
    public void setRatingGroup(long ratingGroup) { this.ratingGroup = ratingGroup; }

    public int getServiceIdentifier() { return serviceIdentifier; }
    public void setServiceIdentifier(int serviceIdentifier) { this.serviceIdentifier = serviceIdentifier; }

    public long getGrantedServiceUnit() { return grantedServiceUnit; }
    public void setGrantedServiceUnit(long grantedServiceUnit) { this.grantedServiceUnit = grantedServiceUnit; }


    @Override
    public String toString() {
        String s = "MultipleServiceCreditControl[" +
                "; Requested-Service-Unit=" + requestedUnits +
                "; Reserved-Service-Unit=" + reservedUnits +
                "; usedUnitsTotal=" + usedUnitsTotal +
                "; usedUnitsInput=" + usedUnitsInput +
                "; usedUnitsOutput=" + usedUnitsOutput +
                "; Rating-Group=" + ratingGroup +
                "; Service-Identifier=" + serviceIdentifier +
                "; Granted-Service-Unit" +
                "]";
        return s;
    }
}
