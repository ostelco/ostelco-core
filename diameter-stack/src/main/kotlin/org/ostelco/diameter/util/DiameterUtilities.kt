package org.ostelco.diameter.util

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet
import org.jdiameter.common.impl.validation.DictionaryImpl
import org.ostelco.diameter.logger
import org.ostelco.diameter.util.AvpType.APP_ID
import org.ostelco.diameter.util.AvpType.FLOAT64
import org.ostelco.diameter.util.AvpType.GROUPED
import org.ostelco.diameter.util.AvpType.INTEGER32
import org.ostelco.diameter.util.AvpType.INTEGER64
import org.ostelco.diameter.util.AvpType.TIME
import org.ostelco.diameter.util.AvpType.UNSIGNED32
import org.ostelco.diameter.util.AvpType.UNSIGNED64
import org.ostelco.diameter.util.AvpType.VENDOR_ID

class DiameterUtilities {

    private val LOG by logger()

    private val AVP_DICTIONARY = DictionaryImpl.INSTANCE

    fun printAvps(avps: AvpSet?) {
        if (avps != null) {
            printAvps(avps, "")
        }
    }

    private fun printAvps(avps: AvpSet, indentation: String) {
        for (avp in avps) {
            val avpRep = AVP_DICTIONARY.getAvp(avp.code, avp.vendorId)
            val avpValue = getAvpValue(avp)
            val avpLine = StringBuilder("$indentation${avp.code} : ${avpRep.name} (${avpRep.type})")
            while (avpLine.length < 50) {
                avpLine.append(if (avpLine.length % 2 == 0) "." else " ")
            }
            avpLine.append(avpValue)
            LOG.info(avpLine.toString())
            if (isGrouped(avp)) {
                try {
                    printAvps(avp.grouped, "$indentation  ")
                } catch (e: AvpDataException) {
                    // Failed to ungroup... ignore then...
                }
            }
        }
    }

    private fun getAvpValue(avp: Avp): Any {
        var avpValue: Any
        try {
            val avpType = AvpDictionary.getType(avp)

            when (avpType) {
                INTEGER32, APP_ID -> avpValue = avp.integer32
                UNSIGNED32, VENDOR_ID -> avpValue = avp.unsigned32
                FLOAT64 -> avpValue = avp.float64
                INTEGER64 -> avpValue = avp.integer64
                TIME -> avpValue = avp.time
                UNSIGNED64 -> avpValue = avp.unsigned64
                GROUPED -> avpValue = "<Grouped>"
                else -> avpValue = (avp.utF8String as String)
                        .replace("\r", "")
                        .replace("\n", "")
            }
        } catch (ignore: Exception) {
            try {
                avpValue = avp.utF8String
                        .replace("\r", "")
                        .replace("\n", "")
            } catch (e: AvpDataException) {
                avpValue = avp.toString()
            }

        }
        return avpValue
    }

    // TODO for missing Avp, is code and vendorId as 0 okay?
    private fun isGrouped(avp: Avp?): Boolean =
            ("Grouped" == AVP_DICTIONARY.getAvp(avp?.code ?: 0, avp?.vendorId ?: 0).type)
}
