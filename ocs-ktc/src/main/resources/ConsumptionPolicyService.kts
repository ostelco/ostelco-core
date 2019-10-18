import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.prime.ocs.core.ConsumptionPolicy
import org.ostelco.prime.ocs.core.ConsumptionRequest
import org.ostelco.prime.storage.ConsumptionResult

private data class ServiceIdRatingGroup(
        val serviceId: Long,
        val ratingGroup: Long
)

object : ConsumptionPolicy {

    override fun checkConsumption(
            msisdn: String,
            multipleServiceCreditControl: MultipleServiceCreditControl,
            mccMnc: String,
            apn: String): Either<ConsumptionResult, ConsumptionRequest> {

        val requested = multipleServiceCreditControl.requested?.totalOctets ?: 0
        val used = multipleServiceCreditControl.used?.totalOctets ?: 0

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
            else -> {
                ConsumptionResult(
                        msisdnAnalyticsId = msisdn,
                        granted = 0L,
                        balance = 0L
                ).left()
            }
        }
    }
}
