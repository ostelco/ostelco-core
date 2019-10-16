package org.ostelco.prime.ocs.core

import arrow.core.Either
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.prime.storage.ConsumptionResult

data class ConsumptionRequest(
        val msisdn: String,
        val usedBytes: Long,
        val requestedBytes: Long
)

interface ChargingAndRatingService {

    fun charge(
            msisdn: String,
            multipleServiceCreditControl: MultipleServiceCreditControl,
            mccMnc: String,
            apn: String
    ): Either<ConsumptionResult, ConsumptionRequest>
}
