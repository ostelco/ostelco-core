package com.telenordigital.ocsgw.diameter;

import java.net.InetAddress;

public class PsInformation {

    // 3GPP-Charging-Id (Avp 2)
    private byte[] chargingId;
    // 3GPP-PDP-Type ( Avp 3 )
    private int pdpType;
    // PDP-Address ( Avp 1227 )
    private InetAddress pdpAddress;
    // SGSN-Adress ( Avp 1228 )
    private InetAddress sgsnAddress;
    // GGSN-Address ( Avp 847 )
    private InetAddress ggsnAddress;
    // 3GPP-IMSI-MNC-MCC ( Avp 8 )
    private String imsiMccMnc;
    // 3GPP-GGSN-MCC-MNC ( Avp 9 )
    private String ggsnMccMnc;
    // 3GPP-NSAPI ( Avp 10 )
    private int nsapi;
    // Called-Station-Id ( Avp 30 )
    private String calledStationId;
    // 3GPP-Selection-Mode ( Avp 12 )
    private String selectionMode;
    // 3GPP-Charging-Characteristics ( Avp 13 )
    private String chargingCharacteristics;
    // 3GPP-SGSN-MCC-MNC ( Avp 18)
    private String sgsnMncMcc;
    // 3GPP-MS-TimeZone ( Avp 23 )
    private byte[] msTimezone;
    // Charging-Rule-Base-Name ( Avp 1004 )
    private String chargingRulebaseName;
    // 3GPP-RAT-Type ( Avp 21 )
    private byte[] ratType;

    // ToDo: Add the following missing AVPs
    // 3GPP-User-Location-Info ( Avp 21 )
    // 3GPP-GPRS-Negotiated-QoS-Profile ( Avp 5 )


    public void setChargingId(byte[] chargingId) {
        this.chargingId = chargingId;
    }

    public void setPdpType(int pdpType) {
        this.pdpType = pdpType;
    }

    public void setPdpAddress(InetAddress pdpAddress) {
        this.pdpAddress = pdpAddress;
    }

    public void setSgsnAddress(InetAddress sgsnAddress) {
        this.sgsnAddress = sgsnAddress;
    }

    public void setGgsnAddress(InetAddress ggsnAddress) {
        this.ggsnAddress = ggsnAddress;
    }

    public void setImsiMccMnc(String imsiMccMnc) {
        this.imsiMccMnc = imsiMccMnc;
    }

    public void setGgsnMccMnc(String ggsnMccMnc) {
        this.ggsnMccMnc = ggsnMccMnc;
    }

    public void setNsapi(int nsapi) {
        this.nsapi = nsapi;
    }

    public void setCalledStationId(String calledStationId) {
        this.calledStationId = calledStationId;
    }

    public void setSelectionMode(String selectionMode) {
        this.selectionMode = selectionMode;
    }

    public void setChargingCharacteristics(String chargingCharacteristics) {
        this.chargingCharacteristics = chargingCharacteristics;
    }

    public void setSgsnMncMcc(String sgsnMncMcc) {
        this.sgsnMncMcc = sgsnMncMcc;
    }

    public void setMsTimezone(byte[] msTimezone) {
        this.msTimezone = msTimezone;
    }

    public void setChargingRulebaseName(String chargingRulebaseName) {
        this.chargingRulebaseName = chargingRulebaseName;
    }

    public void setRatType(byte[] ratType) {
        this.ratType = ratType;
    }

    public byte[] getChargingId() {
        return chargingId;
    }

    public InetAddress getPdpAddress() {
        return pdpAddress;
    }

    public InetAddress getSgsnAddress() {
        return sgsnAddress;
    }

    public InetAddress getGgsnAddress() {
        return ggsnAddress;
    }

    public String getImsiMccMnc() {
        return imsiMccMnc;
    }

    public String getGgsnMccMnc() {
        return ggsnMccMnc;
    }

    public int getNsapi() {
        return nsapi;
    }

    public String getCalledStationId() {
        return calledStationId;
    }

    public String getSelectionMode() {
        return selectionMode;
    }

    public String getChargingCharacteristics() {
        return chargingCharacteristics;
    }

    public String getSgsnMncMcc() {
        return sgsnMncMcc;
    }

    public byte[] getMsTimezone() {
        return msTimezone;
    }

    public String getChargingRulebaseName() {
        return chargingRulebaseName;
    }

    public byte[] getRatType() {
        return ratType;
    }

    public int getPdpType() {
        return pdpType;
    }
}
