package org.ostelco.prime.ocs.core

import arrow.core.Either
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.prime.storage.ConsumptionResult

data class ConsumptionRequest(
        val msisdn: String,
        val usedBytes: Long,
        val requestedBytes: Long
)

data class ServiceIdRatingGroup(
        val serviceId: Long,
        val ratingGroup: Long
)

enum class Mcc(val value: String) {
    ABKHAZIA("289"),
    AFGHANISTAN("412"),
    ALBANIA("276"),
    ALGERIA("603"),
    ANDORRA("213"),
    ANGOLA("631"),
    ANGUILLA("365"),
    ARGENTINA("722"),
    ARMENIA("283"),
    ARUBA("363"),
    AUSTRALIA("505"),
    AUSTRIA("232"),
    AZERBAIJAN("400"),
    BRUNEI("528"),
    CAMBODIA("456"),
    CHINA("460"),
    CYPRUS("280"),
    DENMARK("238"),
    FRANCE("208"),
    GERMANY("262"),
    HONDURAS("708"),
    HONG_KONG("454"),
    HUNGARY("216"),
    ICELAND("274"),
    INDIA("404"),
    INDONESIA("510"),
    JAPAN("440"),
    LAOS("457"),
    MACAO("455"),
    MALAYSIA("502"),
    MYANMAR("414"),
    NORWAY("242"),
    NEW_ZEALAND("530"),
    PAKISTAN("410"),
    PHILIPPINES("515"),
    SINGAPORE("525"),
    SOUTH_KOREA("450"),
    SWEDEN("240"),
    SWITZERLAND("228"),
    TAIWAN("466"),
    THAILAND("520"),
    TIMOR("514"),
    UNITED_KINGDOM("234"),
    UNITED_STATES("310"),
    VIET_NAM("452")
}

interface ConsumptionPolicy {

    /**
     * This function will either return [ConsumptionResult] as [Either]::Left, which is then to be returned back to PGw.
     * Or it will return Consumption Request as [Either]::Right, which is then to be passed to Storage for persistence.
     * And then the result from Storage will be [ConsumptionResult], which will be returned back to PGw.
     */
    fun checkConsumption(
            msisdn: String,
            multipleServiceCreditControl: MultipleServiceCreditControl,
            sgsnMccMnc: String,
            apn: String,
            imsiMccMnc: String
    ): Either<ConsumptionResult, ConsumptionRequest>
}
