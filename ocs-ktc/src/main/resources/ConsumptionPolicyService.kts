import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.prime.getLogger
import org.ostelco.prime.ocs.core.ConsumptionPolicy
import org.ostelco.prime.ocs.core.ConsumptionRequest
import org.ostelco.prime.ocs.core.Mcc
import org.ostelco.prime.ocs.core.ServiceIdRatingGroup
import org.ostelco.prime.storage.ConsumptionResult


enum class MccMnc(val value: String) {
    LOLTEL("24201")
}

object : ConsumptionPolicy {

    private val logger by getLogger()

    private val testAllowedMcc = setOf(
            Mcc.AUSTRALIA.value,
            Mcc.CHINA.value,
            Mcc.HONG_KONG.value,
            Mcc.INDONESIA.value,
            Mcc.JAPAN.value,
            Mcc.MALAYSIA.value,
            Mcc.NORWAY.value,
            Mcc.PHILIPPINES.value,
            Mcc.SINGAPORE.value,
            Mcc.THAILAND.value,
            Mcc.SOUTH_KOREA.value,
            Mcc.VIET_NAM.value
    )

    override fun checkConsumption(
            msisdn: String,
            multipleServiceCreditControl: MultipleServiceCreditControl,
            sgsnMccMnc: String,
            apn: String,
            imsiMccMnc: String): Either<ConsumptionResult, ConsumptionRequest> {

        val requested = multipleServiceCreditControl.requested?.totalOctets ?: 0
        val used = multipleServiceCreditControl.used?.totalOctets ?: 0

        if (!isMccMncAllowed(sgsnMccMnc, imsiMccMnc)) {
            logger.warn("Blocked usage for sgsnMccMnc $sgsnMccMnc imsiMccMnc $imsiMccMnc msisdn $msisdn ")
            return blockConsumption(msisdn)
        }

        return when (ServiceIdRatingGroup(
                serviceId = multipleServiceCreditControl.serviceIdentifier,
                ratingGroup = multipleServiceCreditControl.ratingGroup)) {

            // NORMAL
            ServiceIdRatingGroup(1L, 10L), // Test
            ServiceIdRatingGroup(2L, 12L), // Test
            ServiceIdRatingGroup(4L, 14L), // Test
            ServiceIdRatingGroup(-1L, 10L) /* Test */ -> {
                ConsumptionRequest(
                        msisdn = msisdn,
                        usedBytes = used,
                        requestedBytes = requested
                ).right()
            }

            // BLOCKED
            else -> blockConsumption(msisdn)
        }
    }

    fun blockConsumption(msisdn: String) : Either<ConsumptionResult, ConsumptionRequest> {
        return ConsumptionResult(
                msisdnAnalyticsId = msisdn,
                granted = 0L,
                balance = 0L
        ).left()
    }

    fun isMccMncAllowed(sgsnMccMnc: String, imsiMccMnc: String) : Boolean {
        val sgsnMcc = sgsnMccMnc.substring(range = 0..2)
        return when (imsiMccMnc) {
            MccMnc.LOLTEL.value -> testAllowedMcc.contains(sgsnMcc)
            else -> false
        }
    }
}
