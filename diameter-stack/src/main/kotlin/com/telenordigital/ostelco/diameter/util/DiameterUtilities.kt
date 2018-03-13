package com.telenordigital.ostelco.diameter.util

import com.telenordigital.ostelco.diameter.logger
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet
import org.jdiameter.common.impl.validation.DictionaryImpl

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
            val name = AVP_DICTIONARY.getAvp(avp.code, avp.vendorId).name
            val avpValue = getAvpValue(avp)
            val avpLine = StringBuilder(indentation + avp.code + ": " + name)
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
            val avpType = AVP_DICTIONARY.getAvp(avp.code, avp.vendorId).type

            when (avpType) {
                "Integer32", "AppId" -> avpValue = avp.integer32
                "Unsigned32", "VendorId" -> avpValue = avp.unsigned32
                "Float64" -> avpValue = avp.float64
                "Integer64" -> avpValue = avp.integer64
                "Time" -> avpValue = avp.time
                "Unsigned64" -> avpValue = avp.unsigned64
                "Grouped" -> avpValue = "<Grouped>"
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
