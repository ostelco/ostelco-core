package org.ostelco.prime.storage.legacy

import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product


object Products {

    val DATA_TOPUP_3GB = Product("DataTopup3GB",
            Price(30000, "NOK"),
            mapOf(Pair("noOfBytes", "${3L*1024*1024*1024}")),
            emptyMap())
}
