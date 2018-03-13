package com.telenordigital.ostelco.diameter.model

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet

data class CreditControlRequest(
        val ccrAvps: AvpSet?,
        val multipleServiceCreditControls: List<MultipleServiceCreditControl>,
        val serviceInformation: ServiceInformation?,
        val userEquipmentInfo: UserEquipmentInfo?,
        val msisdn: String,
        val imsi: String,
        val ccRequestType: Avp?,
        val ccRequestNumber: Avp?) {

    // ToDo: This should be connected to rating groups
    val requestedUnits: Long
        get() = this.multipleServiceCreditControls.first().requestedUnits

    // ToDo: This only get the total. There is also input/output if needed
    val usedUnits: Long
        get() = this.multipleServiceCreditControls.first().usedUnitsTotal
}
