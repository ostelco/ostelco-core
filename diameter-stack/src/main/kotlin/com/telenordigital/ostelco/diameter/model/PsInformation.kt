package com.telenordigital.ostelco.diameter.model

import java.net.InetAddress

// ToDo : Add the following AVPs
// 3GPP-GPRS-Negotiated-QoS-Profile ( Avp 5 )

data class PsInformation(
        // 3GPP-Charging-Id (Avp 2)
        val chargingId: ByteArray?,
        // 3GPP-PDP-Type ( Avp 3 )
        val pdpType: Int?,
        // PDP-Address ( Avp 1227 )
        val pdpAddress: InetAddress?,
        // SGSN-Adress ( Avp 1228 )
        val sgsnAddress: InetAddress?,
        // GGSN-Address ( Avp 847 )
        val ggsnAddress: InetAddress?,
        // 3GPP-IMSI-MNC-MCC ( Avp 8 )
        val imsiMccMnc: String?,
        // 3GPP-GGSN-MCC-MNC ( Avp 9 )
        val ggsnMccMnc: String?,
        // 3GPP-NSAPI ( Avp 10 )
        val nsapi: Int,
        // Called-Station-Id ( Avp 30 )
        val calledStationId: String?,
        // 3GPP-Selection-Mode ( Avp 12 )
        val selectionMode: String?,
        // 3GPP-Charging-Characteristics ( Avp 13 )
        val chargingCharacteristics: String?,
        // 3GPP-SGSN-MCC-MNC ( Avp 18)
        val sgsnMncMcc: String?,
        // 3GPP-MS-TimeZone ( Avp 23 )
        val msTimezone: ByteArray?,
        // Charging-Rule-Base-Name ( Avp 1004 )
        val chargingRulebaseName: String?,
        // 3GPP-RAT-Type ( Avp 21 )
        val ratType: ByteArray?,
        // 3GPP-User-Location-Info ( Avp 21 )
        val userLocationInfo: ByteArray?)