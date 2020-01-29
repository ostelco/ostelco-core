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
    M1("52503"),
    DIGI("50216"),
    LOLTEL("24201")
}

object : ConsumptionPolicy {

    private val logger by getLogger()

    private val digiAllowedMcc = setOf(
            Mcc.AUSTRALIA.value,
            Mcc.BRUNEI.value,
            Mcc.CAMBODIA.value,
            Mcc.CHINA.value,
            Mcc.TIMOR.value,
            Mcc.HONG_KONG.value,
            Mcc.INDIA.value,
            Mcc.INDONESIA.value,
            Mcc.JAPAN.value,
            Mcc.LAOS.value,
            Mcc.MACAO.value,
            Mcc.MALAYSIA.value,
            Mcc.MYANMAR.value,
            Mcc.NEW_ZEALAND.value,
            Mcc.NORWAY.value,
            Mcc.PAKISTAN.value,
            Mcc.PHILIPPINES.value,
            Mcc.SINGAPORE.value,
            Mcc.SOUTH_KOREA.value,
            Mcc.TAIWAN.value,
            Mcc.THAILAND.value,
            Mcc.UNITED_KINGDOM.value,
            Mcc.UNITED_STATES.value,
            Mcc.VIET_NAM.value
    )

    // While testing we have some SIMs that are testing roaming, these should never block on MCC
    private val digiFreeRoamingMsisdns = arrayOf("601170510052", "60162746973", "60162730576", "60143229458", "601170510005", "601170510006")

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
            ServiceIdRatingGroup(400L, 400L),       // TATA
            ServiceIdRatingGroup(-1L, 600L),        // M1
            ServiceIdRatingGroup(1L, 10L),          // LolTel
            ServiceIdRatingGroup(-1L, 102010001L)   /* Digi */ -> {
                ConsumptionRequest(
                        msisdn = msisdn,
                        usedBytes = used,
                        requestedBytes = requested
                ).right()
            }

            // ZERO-RATED
            ServiceIdRatingGroup(401L, 401L),       // TATA
            ServiceIdRatingGroup(402L, 402L),       // TATA
            ServiceIdRatingGroup(409L, 409L)        /* Digi */ -> {
                ConsumptionResult(
                    msisdnAnalyticsId = msisdn,
                    granted = multipleServiceCreditControl.requested.totalOctets,
                    balance = multipleServiceCreditControl.requested.totalOctets * 100
                ).left()
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

    fun isAllowedForDigi(sgsnMccMnc : String) : Boolean {
        if ( sgsnMccMnc.length > 3) {
            val sgsnMcc = sgsnMccMnc.substring(range = 0..2)
            return digiAllowedMcc.contains(sgsnMcc)
        }
        return false
    }

    fun isMccMncAllowed(sgsnMccMnc: String, imsiMccMnc: String) : Boolean {
        return when (imsiMccMnc) {
            MccMnc.M1.value -> false
            MccMnc.DIGI.value -> false
            MccMnc.LOLTEL.value -> true
            else -> false
        }
    }
}
