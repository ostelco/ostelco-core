package org.ostelco.diameter.model

import org.ostelco.diameter.parser.AvpField
import org.jdiameter.api.Avp

data class CreditControlAnswer(val multipleServiceCreditControls: List<MultipleServiceCreditControl>)

enum class CreditControlResultCode(val value: Int) {
    DIAMETER_END_USER_SERVICE_DENIED(4010),
    DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE(4011),
    DIAMETER_CREDIT_LIMIT_REACHED(4012),
    DIAMETER_RATING_FAILED(5031),
    DIAMETER_USER_UNKNOWN(5030)
}

// https://tools.ietf.org/html/rfc4006#page-71
enum class FinalUnitAction {
    TERMINATE,
    REDIRECT,
    RESTRICT_ACCESS
}

data class FinalUnitIndication(
        val finalUnitAction: FinalUnitAction,
        val restrictionFilterRule: List<IPFilterRule>,
        val filterId: List<String>,
        val redirectServer: RedirectServer?)

enum class Action {
    PERMIT,
    DENY
}

enum class Direction {
    IN,
    OUT
}

data class IPFilterRule(
        val action: Action,
        val direction: Direction,
        val proto: String,
        val host: String)

class ServiceUnit() {

    @AvpField(Avp.CC_TOTAL_OCTETS)
    var total: Long = 0

    @AvpField(Avp.CC_INPUT_OCTETS)
    var input: Long = 0

    @AvpField(Avp.CC_OUTPUT_OCTETS)
    var output: Long = 0

    constructor(total: Long, input: Long, output: Long) : this() {
        this.total = total
        this.input = input
        this.output = output
    }
}

// https://tools.ietf.org/html/rfc4006#section-8.16
class MultipleServiceCreditControl() {

    @AvpField(Avp.RATING_GROUP)
    var ratingGroup: Long = -1

    @AvpField(Avp.SERVICE_IDENTIFIER_CCA)
    var serviceIdentifier: Long = -1

    @AvpField(Avp.REQUESTED_SERVICE_UNIT)
    var requested = ServiceUnit()

    @AvpField(Avp.USED_SERVICE_UNIT)
    var used = ServiceUnit()

    @AvpField(Avp.GRANTED_SERVICE_UNIT)
    var granted = ServiceUnit()

    var validityTime = 86400

    var finalUnitIndication: FinalUnitIndication? = null

    constructor(ratingGroup: Long, serviceIdentifier: Long, requested: ServiceUnit, used: ServiceUnit, granted: ServiceUnit, validityTime: Int, finalUnitIndication: FinalUnitIndication?) : this() {
        this.ratingGroup = ratingGroup
        this.serviceIdentifier = serviceIdentifier
        this.requested = requested
        this.used = used
        this.granted = granted
        this.validityTime = validityTime
        this.finalUnitIndication = finalUnitIndication
    }
}

enum class RedirectAddressType {
    IPV4_ADDRESS,
    IPV6_ADDRESS,
    URL,
    SIP_URL
}

data class RedirectServer(val redirectAddressType: RedirectAddressType)

class ServiceInformation() {

    @AvpField(Avp.PS_INFORMATION)
    var psInformation: PsInformation? = null
}

// https://tools.ietf.org/html/rfc4006#section-8.47
enum class SubscriptionType {
    END_USER_E164,
    END_USER_IMSI,
    END_USER_SIP_URI,
    END_USER_NAI,
    END_USER_PRIVATE
}

class SubscriptionId() {

    @AvpField(Avp.SUBSCRIPTION_ID_TYPE)
    var idType: SubscriptionType? = null

    @AvpField(Avp.SUBSCRIPTION_ID_DATA)
    var idData:String? = ""
}

// https://tools.ietf.org/html/rfc4006#page-78
class UserEquipmentInfo() {

    @AvpField(Avp.USER_EQUIPMENT_INFO_TYPE)
    var userEquipmentInfoType: UserEquipmentInfoType? = null

    @AvpField(Avp.USER_EQUIPMENT_INFO_VALUE)
    var getUserEquipmentInfoValue: ByteArray? = null
}

enum class UserEquipmentInfoType {
    IMEISV,
    MAC,
    EUI64,
    MODIFIED_EUI64
}