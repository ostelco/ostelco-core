package org.ostelco.prime.admin.importer

import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.Segment

/**
 * The input classes being parsed (as yaml).
 */

data class ProducingAgent(val name: String, val version: String)

class ImportDeclaration(
        val producingAgent: ProducingAgent,
        val offer: Offer,
        val segments: Collection<Segment> = emptyList(),
        val products: Collection<Product> = emptyList())

/*
class TimeInterval(var from: String?= null, var to: String? = null)

class Presentation(
        var badgeLabel: String? = null,
        var description: String? = null,
        var shortDescription: String? = null,
        var label: String? = null,
        var name: String? = null,
        var priceLabel: String? = null,
        var hidden: Boolean? = null,
        var imageUrl: String? = null
)

class OfferFinancials(
        var repurchability: String? = null,
        var currencyLabel: String? = null,
        var price: Int? = null,
        var taxRate: BigDecimal? = null
)

class SubscriberIdCollection(
        var decryptionKey: String? = null,
        var members : MutableList<String>? = null
)


class Segment(
        var type: String? = null,
        var description: String? = null,
        var members: SubscriberIdCollection? = null
)

// XXX Should perhaps, apart from SKU, be a
//     a keyword/value map, to be interpreted by
//     something, somewhere that knows something about
//     technical product parameters?
class Product(
        var sku: String? = null,
        var noOfBytes: BigInteger? = null
)


class Offer(
        var visibility: TimeInterval? = null,
        var presentation: Presentation? = null,
        var financial: OfferFinancials? = null,
        var product: Product? = null
)
*/