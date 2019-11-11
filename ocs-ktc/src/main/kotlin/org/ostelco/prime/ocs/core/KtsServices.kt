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
    AUSTRALIA("505"),
    CHINA("460"),
    HONG_KONG("454"),
    INDONESIA("510"),
    JAPAN("440"),
    MALAYSIA("502"),
    NORWAY("242"),
    PHILIPPINES("515"),
    THAILAND("520"),
    SINGAPORE("525"),
    SOUTH_KOREA("450"),
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
