package org.ostelco.prime.admin.importer

import org.ostelco.prime.model.ChangeSegment
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.Segment

/**
 * The input classes being parsed (as yaml).
 */
data class CreateOffer(val createOffer: Offer)

data class Offer(
        val id:String,
        val createProducts: Collection<Product> = emptyList(),
        val existingProducts: Collection<String> = emptyList(),
        val createSegments: Collection<Segment> = emptyList(),
        val existingSegments: Collection<String> = emptyList())

data class CreateSegments(val createSegments: Collection<Segment>)
data class UpdateSegments(val updateSegments: Collection<Segment>)
data class AddToSegments(val addToSegments: Collection<NonEmptySegment>)
data class RemoveFromSegments(val removeFromSegments: Collection<NonEmptySegment>)
data class ChangeSegments(val changeSegments: Collection<ChangeSegment>)

data class NonEmptySegment(
        val id: String,
        val subscribers: Collection<String>)

/*
data class ProducingAgent(val name: String, val version: String)

data class TimeInterval(val from: String, val to: String)

data class Presentation(
        val badgeLabel: String,
        val description: String,
        val shortDescription: String,
        val label: String,
        val name: String,
        val priceLabel: String,
        val hidden: Boolean,
        val imageUrl: String
)

data class OfferFinancials(
        val repurchability: String,
        val currencyLabel: String,
        val price: Int,
        val taxRate: BigDecimal
)

data class SubscriberIdCollection(
        val decryptionKey: String,
        val members : MutableList<String>
)

data class Segment(
        val type: String,
        val description: String,
        val members: SubscriberIdCollection
)

// XXX Should perhaps, apart from SKU, be a
//     a keyword/value map, to be interpreted by
//     something, somewhere that knows something about
//     technical product parameters?
data class Product(
        val sku: String,
        val noOfBytes: BigInteger
)

data class Offer(
        val visibility: TimeInterval,
        val presentation: Presentation,
        val financial: OfferFinancials,
        val product: Product
)
*/