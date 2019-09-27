package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product


object Products {

    val DATA_TOPUP_3GB = Product("DataTopup3GB",
            Price(30000, "NOK"),
            mapOf(
                    "productClass" to "SIMPLE_DATA",
                    "noOfBytes" to "3_000_000_000"
            ),
            emptyMap())
}
