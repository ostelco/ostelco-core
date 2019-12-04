package org.ostelco.diameter.util

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet
import org.jdiameter.api.validation.AvpRepresentation
import org.jdiameter.common.impl.validation.DictionaryImpl
import org.ostelco.diameter.getLogger
import org.ostelco.diameter.util.AvpType.*

class DiameterUtilities {

    private val logger by getLogger()

    private val dictionary = DictionaryImpl.INSTANCE

    fun printAvps(avps: AvpSet?) {
        val builder = StringBuilder()
        builder.append("\n")
        if (avps != null) {
            printAvps(avps, "", builder)
        }
        logger.debug(builder.toString())
    }

    private fun printAvps(avps: AvpSet, indentation: String, builder: StringBuilder) {
        for (avp in avps) {
            val avpRep : AvpRepresentation? = dictionary.getAvp(avp.code, avp.vendorId)
            val avpValue = getAvpValue(avp, avpRep?.originalType)
            val avpLine = StringBuilder("$indentation${avp.code} : ${avpRep?.name} (${avpRep?.originalType})")
            while (avpLine.length < 50) {
                avpLine.append(if (avpLine.length % 2 == 0) "." else " ")
            }
            avpLine.append(avpValue)
            builder.append(avpLine.toString() + "\n")
            if (isGrouped(avp)) {
                try {
                    printAvps(avp.grouped, "$indentation  ", builder)
                } catch (e: AvpDataException) {
                    // Failed to ungroup... ignore then...
                }
            }
        }
    }

    private fun getAvpValue(avp: Avp, originalType: String?): Any {
        val type = getAvpType(avp, originalType)
        return when (type) {
            ADDRESS.label -> avp.address
            DIAMETER_IDENTITY.label -> avp.diameterIdentity
            DIAMETER_IDENTITY.label -> avp.diameterURI
            FLOAT32.label -> avp.float32
            FLOAT64.label -> avp.float64
            ENUMERATED.label -> avp.integer32
            GROUPED.label -> "<Grouped>"
            INTEGER32.label, APP_ID.label -> avp.integer32
            INTEGER64.label -> avp.integer64
            IP_ADDRESS.label -> avp.address
            OCTET_STRING.label -> ByteArrayToHexString(avp.octetString)
            RAW.label -> avp.raw
            RAW_DATA.label -> avp.rawData
            TIME.label -> avp.time
            UNSIGNED32.label, VENDOR_ID.label -> avp.unsigned32
            UNSIGNED64.label -> avp.unsigned64
            UTF8STRING.label -> avp.utF8String
            else -> "unknown type $originalType"
        }
    }

    private fun getAvpType(avp: Avp, originalType: String?) : String? {
        var type = originalType
        val correctedType = AvpTypeDictionary.getOverrideType(avp)
        if ( correctedType != null ) {
            type = correctedType.label
        }
        return type
    }

    private fun ByteArrayToHexString(bytes: ByteArray): String {
        val hexArray = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
        val hexChars = CharArray(bytes.size * 2)
        var v: Int
        for (j in bytes.indices) {
            v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun hexStringToByteArray(hexString: String): ByteArray {
        val len = hexString.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4)
                    + Character.digit(hexString[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    // TODO martin: for missing Avp, is code and vendorId as 0 okay?
    private fun isGrouped(avp: Avp?): Boolean  {
        if (avp?.code != null) {
            return "Grouped" == dictionary.getAvp(avp.code, avp.vendorId)?.type
        }
        return false
    }
}
