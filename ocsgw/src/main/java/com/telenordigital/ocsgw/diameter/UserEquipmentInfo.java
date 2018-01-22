package com.telenordigital.ocsgw.diameter;

// https://tools.ietf.org/html/rfc4006#page-78

class UserEquipmentInfo {

    private int userEquipmentInfoType = UserEquipmentInfoType.IMEISV;
    private byte[] getUserEquipmentInfoValue;

    public int getUserEquipmentInfoType() {
        return userEquipmentInfoType;
    }
    public void setUserEquipmentInfoType(int userEquipmentInfoType) { this.userEquipmentInfoType = userEquipmentInfoType; }

    public byte[] getGetUserEquipmentInfoValue() {
        return getUserEquipmentInfoValue;
    }
    public void setGetUserEquipmentInfoValue(byte[] getUserEquipmentInfoValue) { this.getUserEquipmentInfoValue = getUserEquipmentInfoValue; }
}
