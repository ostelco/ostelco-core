package org.ostelco.diameter.builder

import org.jdiameter.api.AvpSet
import org.jdiameter.api.URI
import java.net.InetAddress
import java.util.*

/**
 * DSL style helper class to populate values into [org.jdiameter.api.AvpSet]
 */
fun set(avpSet: AvpSet, init: AvpSetContext.() -> Unit) {
    val avpSetContext = AvpSetContext(avpSet)
    avpSetContext.init()
}

class AvpSetContext(private val avpSet: AvpSet) {
    fun avp(
            avpCode: Int, value: Any, vendorId: Long = 0,
            mFlag: Boolean = true, pFlag: Boolean = false, asOctetString: Boolean = false) {
        when (value) {
            is ByteArray -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag)
            is Int -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag)
            is Long -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag)
            is Float -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag)
            is Double -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag)
            is String -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag, asOctetString)
            is URI -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag)
            is InetAddress -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag)
            is Date -> avpSet.addAvp(avpCode, value, vendorId, mFlag, pFlag)
        }
    }

    fun group(avpCode: Int, vendorId:Long = 0, mFlag: Boolean = true, pFlag: Boolean = false,
              init: AvpSetContext.() -> Unit) {
        val subContext = AvpSetContext(avpSet.addGroupedAvp(avpCode, vendorId, mFlag, pFlag))
        subContext.init()
    }
}

