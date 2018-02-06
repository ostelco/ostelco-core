package com.telenordigital.ocsgw.diameter;

// https://tools.ietf.org/html/rfc4006#page-78

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserEquipmentInfo {

    private static final Logger LOG = LoggerFactory.getLogger(UserEquipmentInfo.class);

    private int userEquipmentInfoType = UserEquipmentInfoType.IMEISV;
    private byte[] getUserEquipmentInfoValue;

    public int getUserEquipmentInfoType() {
        return userEquipmentInfoType;
    }

    public byte[] getGetUserEquipmentInfoValue() {
        return getUserEquipmentInfoValue;
    }

    public void parseAvp(AvpSet set) {
        try {
            this.userEquipmentInfoType = set.getAvp(Avp.USER_EQUIPMENT_INFO_TYPE).getInteger32();
            this.getUserEquipmentInfoValue = set.getAvp(Avp.USER_EQUIPMENT_INFO_VALUE).getOctetString();
        } catch (AvpDataException e) {
            LOG.error("Failed to parse User-Equipment-Info", e);
        }
    }
}
