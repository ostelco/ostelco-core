package org.ostelco.diameter.util

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet
import org.jdiameter.common.impl.validation.DictionaryImpl
import org.ostelco.diameter.logger
import org.ostelco.diameter.util.AvpType.ADDRESS
import org.ostelco.diameter.util.AvpType.APP_ID
import org.ostelco.diameter.util.AvpType.FLOAT32
import org.ostelco.diameter.util.AvpType.FLOAT64
import org.ostelco.diameter.util.AvpType.GROUPED
import org.ostelco.diameter.util.AvpType.IDENTITY
import org.ostelco.diameter.util.AvpType.INTEGER32
import org.ostelco.diameter.util.AvpType.INTEGER64
import org.ostelco.diameter.util.AvpType.OCTET_STRING
import org.ostelco.diameter.util.AvpType.RAW
import org.ostelco.diameter.util.AvpType.RAW_DATA
import org.ostelco.diameter.util.AvpType.TIME
import org.ostelco.diameter.util.AvpType.UNSIGNED32
import org.ostelco.diameter.util.AvpType.UNSIGNED64
import org.ostelco.diameter.util.AvpType.URI
import org.ostelco.diameter.util.AvpType.UTF8STRING
import org.ostelco.diameter.util.AvpType.VENDOR_ID

class DiameterUtilities {

    private val logger by logger()

    private val dictionary = DictionaryImpl.INSTANCE

    fun printAvps(avps: AvpSet?) {
        if (avps != null) {
            printAvps(avps, "")
        }
    }

    private fun printAvps(avps: AvpSet, indentation: String) {
        for (avp in avps) {
            val avpRep = dictionary.getAvp(avp.code, avp.vendorId)
            val avpValue = getAvpValue(avp)
            val avpLine = StringBuilder("$indentation${avp.code} : ${avpRep.name} (${avpRep.type})")
            while (avpLine.length < 50) {
                avpLine.append(if (avpLine.length % 2 == 0) "." else " ")
            }
            avpLine.append(avpValue)
            logger.debug(avpLine.toString())
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
        val avpType = AvpDictionary.getType(avp)
        return when (avpType) {
            ADDRESS -> avp.address
            IDENTITY -> avp.diameterIdentity
            URI -> avp.diameterURI
            FLOAT32 -> avp.float32
            FLOAT64 -> avp.float64
            GROUPED -> "<Grouped>"
            INTEGER32, APP_ID -> avp.integer32
            INTEGER64 -> avp.integer64
            OCTET_STRING -> String(avp.octetString)
            RAW -> avp.raw
            RAW_DATA -> avp.rawData
            TIME -> avp.time
            UNSIGNED32, VENDOR_ID -> avp.unsigned32
            UNSIGNED64 -> avp.unsigned64
            UTF8STRING -> avp.utF8String
            null -> "<null>"
        }
    }

    // TODO martin: for missing Avp, is code and vendorId as 0 okay?
    private fun isGrouped(avp: Avp?): Boolean =
            ("Grouped" == dictionary.getAvp(avp?.code ?: 0, avp?.vendorId ?: 0).type)
}
