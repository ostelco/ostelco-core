package org.ostelco.prime.storage.entities

import org.ostelco.prime.storage.AsMappable
import java.util.*

interface RecordOfPurchase : AsMappable {
    val msisdn: String

    val millisSinceEpoch: Long

    val sku: String

    override fun asMap(): Map<String, Any> {
        val result = HashMap<String, Any>()
        result["msisdn"] = msisdn
        result["sku"] = sku
        result["timeInMillisSinceEpoch"] = millisSinceEpoch
        return result
    }
}
