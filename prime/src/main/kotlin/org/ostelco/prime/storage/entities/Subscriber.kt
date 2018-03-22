package org.ostelco.prime.storage.entities

import org.ostelco.prime.storage.AsMappable
import java.util.*

interface Subscriber : AsMappable {

    val noOfBytesLeft: Long

    val msisdn: String?

    override fun asMap(): Map<String, Any?> {
        val result = HashMap<String, Any?>()
        result["noOfBytesLeft"] = noOfBytesLeft
        result["msisdn"] = msisdn
        return result
    }
}
