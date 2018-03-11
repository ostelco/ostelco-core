package com.telenordigital.ostelco.diameter.model

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
        val redirectServer: RedirectServer)

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

// https://tools.ietf.org/html/rfc4006#section-8.16
data class MultipleServiceCreditControl(
        val ratingGroup: Int,
        val serviceIdentifier: Int,
        val requestedUnits: Long,
        val usedUnitsTotal: Long,
        val usedUnitsInput: Long,
        val usedUnitsOutput: Long,
        val grantedServiceUnit: Long,
        val validityTime: Int,
        val finalUnitIndication: FinalUnitIndication?)

enum class RedirectAddressType {
    IPV4_ADDRESS,
    IPV6_ADDRESS,
    URL,
    SIP_URL
}

data class RedirectServer(val redirectAddressType: RedirectAddressType)

data class ServiceInformation(val psInformation: PsInformation?)

// https://tools.ietf.org/html/rfc4006#section-8.47
enum class SubscriptionType {
    END_USER_E164,
    END_USER_IMSI,
    END_USER_SIP_URI,
    END_USER_NAI,
    END_USER_PRIVATE
}

// https://tools.ietf.org/html/rfc4006#page-78
data class UserEquipmentInfo(
        val userEquipmentInfoType: UserEquipmentInfoType,
        val getUserEquipmentInfoValue: ByteArray?)

enum class UserEquipmentInfoType {
    IMEISV,
    MAC,
    EUI64,
    MODIFIED_EUI64
}