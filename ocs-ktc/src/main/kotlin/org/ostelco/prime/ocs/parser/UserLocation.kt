package org.ostelco.prime.ocs.parser

import org.ostelco.prime.getLogger
import java.io.UnsupportedEncodingException

class UserLocation(
        var geographicLocationType: String,
        var mnc: String,
        var mcc: String
)

/***
 *  This will parse the 3GPP-USER-LOCATION-INFO, for now we only read the MCC and MCC
 *  but this also include the cellId, LAC, ECGI etc
 *
 *  For definitions please see : TS 29.061
 *                               TS 29.274 (8.21.x)
 *
 */
@ExperimentalUnsignedTypes
object UserLocationParser {

    private val logger by getLogger()

    fun getParsedUserLocation(userLocationObject: ByteArray?): UserLocation? {

        fun UByte.upper4Bits() = toInt().ushr(4)

        fun UByte.lower4Bits() = (this and 15u)

        if (userLocationObject != null && userLocationObject.size > 4) {
            try {
                val b = userLocationObject[0]
                val geographicLocationType: String = (b.toInt() and 0xFF).toString()

                val ub1 = userLocationObject[1].toUByte()

                val mcc1 = ub1.lower4Bits()
                val mcc2 = ub1.upper4Bits()

                val ub2 = userLocationObject[2].toUByte()

                val mcc3 = ub2.lower4Bits()
                val mnc3 = ub2.upper4Bits()

                val ub3 = userLocationObject[3].toUByte()

                val mnc1 = ub3.lower4Bits()
                val mnc2 = ub3.upper4Bits()

                val mnc = if (mnc3 > 9) {
                    "$mnc1$mnc2"
                } else {
                    "$mnc1$mnc2$mnc3"
                }

                val mcc = "$mcc1$mcc2$mcc3"

                return UserLocation(geographicLocationType, mnc, mcc)
            } catch (e: UnsupportedEncodingException) {
                logger.info("Unsupported encoding", e)
            }
        } else {
            logger.debug("Empty 3GPP-USER-LOCATION-INFO")
        }
        return null
    }
}
