package org.ostelco.diameter.util

import org.jdiameter.api.Avp
import org.mobicents.diameter.dictionary.AvpDictionary
import org.mobicents.diameter.dictionary.AvpRepresentation
import org.ostelco.diameter.getLogger
import org.ostelco.diameter.util.AvpType.ADDRESS
import org.ostelco.diameter.util.AvpType.DIAMETER_IDENTITY
import org.ostelco.diameter.util.AvpType.OCTET_STRING
import org.ostelco.diameter.util.AvpType.UTF8STRING

object AvpTypeDictionary {

    private val logger by getLogger()

    private val avpRepMap: MutableMap<Int, AvpType?> = HashMap()
    private val avpTypeMap: MutableMap<String, AvpType?> = HashMap()

    init {
        try {
            AvpDictionary.INSTANCE.parseDictionary(dictionaryPath())
            AvpRep.values().forEach { avpRepMap[it.avpCode] = it.avpType }
            AvpType.values().forEach { avpTypeMap[it.label] = it }
        } catch (e:Exception) {
            logger.error("Failed to init AvpTypeDictionary", e)
        }
    }

    private fun dictionaryPath(): String {
        var configPath: String? = System.getenv("CONFIG_FOLDER")
        if (configPath == null) {
            configPath = "config"
        }
        return configPath + "/dictionary.xml"
    }

    fun getType(avp: Avp): AvpType? {

        var avpType: AvpType? = avpRepMap[avp.code]

        if (avpType == null) {
            // We need to know the data type of the given AVP so that we call right method to fetch the value.
            // Metadata about AVP is lookup into a dictionary.
            var avpRep: AvpRepresentation? = AvpDictionary.INSTANCE.getAvp(avp.code, avp.vendorId)
            // If the lookup returns null,
            if (avpRep == null) {
                avpRep = AvpDictionary.INSTANCE.getAvp(avp.code)
            }
            if (avpRep == null) {
                logger.error("AVP ${avp.code} missing in dictionary")
                return null
            }

            logger.trace("Type(str): ${avpRep.type}")
            avpType = avpTypeMap[avpRep.type]
        }

        return avpType
    }

    // Some AVPs are incorrect in jDiameter dictionary. This will correct the known misstakes.
    fun getOverrideType(avp: Avp): AvpType? {
        return avpRepMap[avp.code]
    }
}

enum class AvpType(val label: String) {
    ADDRESS("Address"),
    DIAMETER_IDENTITY("DiameterIdentity"),
    DIAMETER_URI("DiameterURI"),
    ENUMERATED("Enumerated"),
    FLOAT32("Float32"),
    FLOAT64("Float64"),
    GROUPED("Grouped"),
    INTEGER32("Integer32"),
    INTEGER64("Integer64"),
    IP_ADDRESS("IPAddress"),
    IP_FILTER_RULE("IPFilterRule"),
    OCTET_STRING("OctetString"),
    QOS_FILTER_RULE("QoSFilterRule"),
    RAW("Raw"),
    RAW_DATA("RawData"),
    TIME("Time"),
    UNSIGNED32("Unsigned32"),
    UNSIGNED64("Unsigned64"),
    UTF8STRING("UTF8String"),

    APP_ID("AppId"),
    VENDOR_ID("VendorId")
}

enum class AvpRep(val avpCode: Int, val avpType: AvpType) {
    GGSN_ADDRESS(Avp.GGSN_ADDRESS, ADDRESS),
    PDP_ADDRESS(Avp.PDP_ADDRESS, ADDRESS),
    RAT_TYPE(Avp.TGPP_RAT_TYPE, OCTET_STRING),
    SELECTION_MODE(Avp.TGPP_SELECTION_MODE, UTF8STRING),
    SGSN_ADDRESS(Avp.SGSN_ADDRESS, ADDRESS),
    USER_LOCATION(Avp.GPP_USER_LOCATION_INFO, OCTET_STRING),
    ORIGIN_HOST(Avp.ORIGIN_HOST, DIAMETER_IDENTITY),
    ORIGIN_REALM(Avp.ORIGIN_REALM, DIAMETER_IDENTITY)
}