package com.telenordigital.ostelco.diameter.parser

// https://tools.ietf.org/html/rfc4006#page-78

import com.telenordigital.ostelco.diameter.logger
import com.telenordigital.ostelco.diameter.model.UserEquipmentInfo
import com.telenordigital.ostelco.diameter.model.UserEquipmentInfoType
import com.telenordigital.ostelco.diameter.model.UserEquipmentInfoType.IMEISV
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet

internal class UserEquipmentInfoParser(val set: AvpSet) : Parser<UserEquipmentInfo> {

    private val LOG by logger()

    private var userEquipmentInfoType = IMEISV
    private var getUserEquipmentInfoValue: ByteArray? = null

    init {
        parseAvp(set)
    }

    override fun parse(): UserEquipmentInfo {
        return UserEquipmentInfo(
                userEquipmentInfoType,
                getUserEquipmentInfoValue)
    }

    private fun parseAvp(set: AvpSet) {
        try {
            this.userEquipmentInfoType = UserEquipmentInfoType.values()[set.getAvp(Avp.USER_EQUIPMENT_INFO_TYPE).integer32]
            this.getUserEquipmentInfoValue = set.getAvp(Avp.USER_EQUIPMENT_INFO_VALUE).octetString
        } catch (e: AvpDataException) {
            LOG.error("Failed to parse User-Equipment-Info", e)
        }
    }
}
