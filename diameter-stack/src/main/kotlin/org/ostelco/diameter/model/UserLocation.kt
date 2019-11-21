package org.ostelco.diameter.model

import org.ostelco.diameter.getLogger
import java.io.UnsupportedEncodingException
import kotlin.experimental.and

class UserLocation(var geographicLocationType: String?, var mnc: String?, var mcc: String?) {}

/***
 *  This will parse the 3GPP-USER-LOCATION-INFO, for now we only read the MCC and MCC
 *  but this also include the cellId, LAC, ECGI etc
 *
 *  For definitions please see : TS 29.061
 *                               TS 29.274 (8.21.x)
 *
 */
object UserLocationParser {

    private val logger by getLogger()

    @ExperimentalUnsignedTypes
    @JvmStatic
    fun getParsedUserLocation(userLocationObject: ByteArray?): UserLocation? {
        var parsedLocation: UserLocation? = null
        if (userLocationObject != null && userLocationObject.size > 4) {
            try {
                val b = userLocationObject[0]
                val geographicLocationType: String = (b.toInt() and 0xFF).toString()

                var ub = userLocationObject[1].toUByte()

                val mcc2 =  ub.toInt().ushr(4).toByte()
                val mcc1 = (ub and 15u).toByte()

                ub = userLocationObject[2].toUByte()

                val mnc3 =  ub.toInt().ushr(4).toByte()
                val mcc3 = (ub and 15u).toByte()

                ub = userLocationObject[3].toUByte()

                val mnc2 =  ub.toInt().ushr(4).toByte()
                val mnc1 = (ub and 15u).toByte()

                val mnc = "" + mnc1 + mnc2
                val mcc = "" + mcc1 + mcc2 + mcc3

                logger.info("geographicLocationType $geographicLocationType mcc $mcc mnc $mnc")
                parsedLocation = UserLocation(geographicLocationType, mnc, mcc)
            } catch (e: UnsupportedEncodingException) {
                    logger.info("Unsupported encoding", e)
            }
        } else {
            logger.debug("Empty 3GPP-USER-LOCATION-INFO")
        }
        return parsedLocation
    }
}
