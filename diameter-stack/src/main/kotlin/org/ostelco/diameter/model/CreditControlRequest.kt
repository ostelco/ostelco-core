package org.ostelco.diameter.model

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.ostelco.diameter.model.SubscriptionType.END_USER_E164
import org.ostelco.diameter.model.SubscriptionType.END_USER_IMSI
import org.ostelco.diameter.parser.AvpField
import org.ostelco.diameter.parser.AvpList

class CreditControlRequest() {

    @AvpList(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL, MultipleServiceCreditControl::class)
    var multipleServiceCreditControls: List<MultipleServiceCreditControl> = emptyList()

    @AvpList(Avp.SERVICE_INFORMATION, ServiceInformation::class)
    var serviceInformation: List<ServiceInformation> = emptyList()

    @AvpList(Avp.USER_EQUIPMENT_INFO, UserEquipmentInfo::class)
    var userEquipmentInfo: List<UserEquipmentInfo> = emptyList()

    @AvpList(Avp.SUBSCRIPTION_ID, SubscriptionId::class)
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
        get() = this.multipleServiceCreditControls.first().requested[0].total

    // ToDo: This only get the total. There is also input/output if needed
    val usedUnits: Long
        get() = this.multipleServiceCreditControls.first().used.total
}
