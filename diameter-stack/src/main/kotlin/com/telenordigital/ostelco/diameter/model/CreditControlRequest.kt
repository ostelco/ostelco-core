package com.telenordigital.ostelco.diameter.model

import com.telenordigital.ostelco.diameter.model.SubscriptionType.END_USER_E164
import com.telenordigital.ostelco.diameter.model.SubscriptionType.END_USER_IMSI
import com.telenordigital.ostelco.diameter.parser.AvpField
import com.telenordigital.ostelco.diameter.parser.AvpGroup
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet

class CreditControlRequest() {

    @AvpGroup(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL, MultipleServiceCreditControl::class)
    var multipleServiceCreditControls: List<MultipleServiceCreditControl> = emptyList()

    @AvpField(Avp.SERVICE_INFORMATION)
    var serviceInformation: ServiceInformation? = null

    @AvpField(Avp.USER_EQUIPMENT_INFO)
    var userEquipmentInfo: UserEquipmentInfo? = null

    @AvpGroup(Avp.SUBSCRIPTION_ID, SubscriptionId::class)
    var subscriptionIds: List<SubscriptionId> = emptyList()
        set(value) {
            value.filter { it.idType == END_USER_E164 }.forEach { msisdn = it.idData ?: msisdn }
            value.filter { it.idType == END_USER_IMSI }.forEach { imsi = it.idData ?: imsi }
            field = value
        }

    var msisdn: String = ""
        private set

    var imsi: String = ""
        private set

    @AvpField(Avp.CC_REQUEST_TYPE)
    var ccRequestType: Avp? = null

    @AvpField(Avp.CC_REQUEST_NUMBER)
    var ccRequestNumber: Avp? = null

    var ccrAvps: AvpSet? = null

    // ToDo: This should be connected to rating groups
    val requestedUnits: Long
        get() = this.multipleServiceCreditControls.first().requested.total

    // ToDo: This only get the total. There is also input/output if needed
    val usedUnits: Long
        get() = this.multipleServiceCreditControls.first().used.total
}
