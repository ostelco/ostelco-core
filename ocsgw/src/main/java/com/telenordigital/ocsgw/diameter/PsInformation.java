package com.telenordigital.ocsgw.diameter;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Optional;

public class PsInformation {

    private static final Logger LOG = LoggerFactory.getLogger(PsInformation.class);

    // 3GPP-Charging-Id (Avp 2)
    private byte[] chargingId = null;
    // 3GPP-PDP-Type ( Avp 3 )
    private int pdpType = 0;
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

    public void parseAvps(AvpSet psInformationAvps) {
        try {
            Optional<byte[]> chargingId = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_CHARGING_ID).getOctetString());
            if (chargingId.isPresent()) {
                this.chargingId = chargingId.get();
            }
            Optional<Avp> pdpType = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_PDP_TYPE));
            if (pdpType.isPresent()) {
                this.pdpType = psInformationAvps.getAvp(Avp.TGPP_PDP_TYPE).getInteger32();
            }
            Optional<Avp> pdpAddress = Optional.ofNullable(psInformationAvps.getAvp(Avp.PDP_ADDRESS));
            if (pdpAddress.isPresent()) {
                this.pdpAddress = psInformationAvps.getAvp(Avp.PDP_ADDRESS).getAddress();
            }
            Optional<Avp> sgsnAddress = Optional.ofNullable(psInformationAvps.getAvp(Avp.SGSN_ADDRESS));
            if (sgsnAddress.isPresent()) {
                this.sgsnAddress = psInformationAvps.getAvp(Avp.SGSN_ADDRESS).getAddress();
            }
            Optional<Avp> ggsnAddress = Optional.ofNullable(psInformationAvps.getAvp(Avp.GGSN_ADDRESS));
            if (ggsnAddress.isPresent()) {
                this.ggsnAddress = psInformationAvps.getAvp(Avp.GGSN_ADDRESS).getAddress();
            }
            Optional<Avp> imsiMccMnc = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_IMSI_MCC_MNC));
            if (imsiMccMnc.isPresent()) {
                this.imsiMccMnc = psInformationAvps.getAvp(Avp.TGPP_IMSI_MCC_MNC).getUTF8String();
            }
            Optional<Avp> ggsnMccMnc = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_GGSN_MCC_MNC));
            if (ggsnMccMnc.isPresent()) {
                this.ggsnMccMnc = psInformationAvps.getAvp(Avp.TGPP_GGSN_MCC_MNC).getUTF8String();
            }
            Optional<Avp> calledStationId = Optional.ofNullable(psInformationAvps.getAvp(30)); // CALLED_STATION_ID (Avp 30)
            if (calledStationId.isPresent()) {
                this.calledStationId = psInformationAvps.getAvp(30).getUTF8String();
            }
            Optional<Avp> selectionMode = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_SELECTION_MODE));
            if (selectionMode.isPresent()) {
                this.selectionMode = psInformationAvps.getAvp(Avp.TGPP_SELECTION_MODE).getUTF8String();
            }
            Optional<Avp> chargingCharacteristics = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_CHARGING_CHARACTERISTICS));
            if (chargingCharacteristics.isPresent()) {
                this.chargingCharacteristics = psInformationAvps.getAvp(Avp.TGPP_CHARGING_CHARACTERISTICS).getUTF8String();
            }
            Optional<Avp> sgsnMncMcc = Optional.ofNullable(psInformationAvps.getAvp(Avp.GPP_SGSN_MCC_MNC));
            if (sgsnMncMcc.isPresent()) {
                this.sgsnMncMcc = psInformationAvps.getAvp(Avp.GPP_SGSN_MCC_MNC).getUTF8String();
            }
            Optional<Avp> msTimezone = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_MS_TIMEZONE));
            if (msTimezone.isPresent()) {
                this.msTimezone = psInformationAvps.getAvp(Avp.TGPP_MS_TIMEZONE).getOctetString();
            }
            Optional<Avp> chargingRulebaseName = Optional.ofNullable(psInformationAvps.getAvp(Avp.CHARGING_RULE_BASE_NAME));
            if (chargingRulebaseName.isPresent()) {
                this.chargingRulebaseName = psInformationAvps.getAvp(Avp.CHARGING_RULE_BASE_NAME).getUTF8String();
            }
            Optional<Avp> ratType = Optional.ofNullable(psInformationAvps.getAvp(Avp.TGPP_RAT_TYPE));
            if (ratType.isPresent()) {
                this.ratType = psInformationAvps.getAvp(Avp.TGPP_RAT_TYPE).getOctetString();
            }
        } catch (AvpDataException e) {
            LOG.error("Failed to parse PS-Information", e);
        }
    }
}
