package com.telenordigital.ostelco.diameter.parser

import com.telenordigital.ostelco.diameter.logger
import com.telenordigital.ostelco.diameter.model.PsInformation
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet
import java.net.InetAddress

class PsInformationParser(psInformationAvps: AvpSet) : Parser<PsInformation> {

    private val LOG by logger()

    // 3GPP-Charging-Id (Avp 2)
    // ToDo : Add the following AVPs
    // 3GPP-GPRS-Negotiated-QoS-Profile ( Avp 5 )

    private var chargingId: ByteArray? = null
    // 3GPP-PDP-Type ( Avp 3 )
    private var pdpType = 0
    // PDP-Address ( Avp 1227 )
    private var pdpAddress: InetAddress? = null
    // SGSN-Adress ( Avp 1228 )
    private var sgsnAddress: InetAddress? = null
    // GGSN-Address ( Avp 847 )
    private var ggsnAddress: InetAddress? = null
    // 3GPP-IMSI-MNC-MCC ( Avp 8 )
    private var imsiMccMnc: String? = null
    // 3GPP-GGSN-MCC-MNC ( Avp 9 )
    private var ggsnMccMnc: String? = null
    // 3GPP-NSAPI ( Avp 10 )
    val nsapi: Int = 0
    // Called-Station-Id ( Avp 30 )
    private var calledStationId: String? = null
    // 3GPP-Selection-Mode ( Avp 12 )
    private var selectionMode: String? = null
    // 3GPP-Charging-Characteristics ( Avp 13 )
    private var chargingCharacteristics: String? = null
    // 3GPP-SGSN-MCC-MNC ( Avp 18)
    private var sgsnMncMcc: String? = null
    // 3GPP-MS-TimeZone ( Avp 23 )
    private var msTimezone: ByteArray? = null
    // Charging-Rule-Base-Name ( Avp 1004 )
    private var chargingRulebaseName: String? = null
    // 3GPP-RAT-Type ( Avp 21 )
    private var ratType: ByteArray? = null
    // 3GPP-User-Location-Info ( Avp 21 )
    private var userLocationInfo: ByteArray? = null

    init {
        parseAvps(psInformationAvps)
    }

    override fun parse(): PsInformation {
        return PsInformation(
                chargingId,
                pdpType,
                pdpAddress,
                sgsnAddress,
                ggsnAddress,
                imsiMccMnc,
                ggsnMccMnc,
                nsapi,
                calledStationId,
                selectionMode,
                chargingCharacteristics,
                sgsnMncMcc,
                msTimezone,
                chargingRulebaseName,
                ratType,
                userLocationInfo)
    }

    private fun parseAvps(psInformationAvps: AvpSet) {
        try {
            this.chargingId = psInformationAvps.getAvp(Avp.TGPP_CHARGING_ID)?.octetString
            this.pdpType = psInformationAvps.getAvp(Avp.TGPP_PDP_TYPE)?.integer32 ?: 0
            this.pdpAddress = psInformationAvps.getAvp(Avp.PDP_ADDRESS)?.address
            this.sgsnAddress = psInformationAvps.getAvp(Avp.SGSN_ADDRESS)?.address
            this.ggsnAddress = psInformationAvps.getAvp(Avp.GGSN_ADDRESS)?.address
            this.imsiMccMnc = psInformationAvps.getAvp(Avp.TGPP_IMSI_MCC_MNC)?.utF8String
            this.ggsnMccMnc = psInformationAvps.getAvp(Avp.TGPP_GGSN_MCC_MNC)?.utF8String
            this.calledStationId = psInformationAvps.getAvp(30)?.utF8String // CALLED_STATION_ID (Avp 30)
            this.selectionMode = psInformationAvps.getAvp(Avp.TGPP_SELECTION_MODE)?.utF8String
            this.chargingCharacteristics = psInformationAvps.getAvp(Avp.TGPP_CHARGING_CHARACTERISTICS)?.utF8String
            this.sgsnMncMcc = psInformationAvps.getAvp(Avp.GPP_SGSN_MCC_MNC)?.utF8String
            this.msTimezone = psInformationAvps.getAvp(Avp.TGPP_MS_TIMEZONE)?.octetString
            this.chargingRulebaseName = psInformationAvps.getAvp(Avp.CHARGING_RULE_BASE_NAME)?.utF8String
            this.ratType = psInformationAvps.getAvp(Avp.TGPP_RAT_TYPE)?.octetString
            this.userLocationInfo = psInformationAvps.getAvp(Avp.GPP_USER_LOCATION_INFO)?.octetString
        } catch (e: AvpDataException) {
            LOG.error("Failed to parse PS-Information", e)
        }
    }
}
