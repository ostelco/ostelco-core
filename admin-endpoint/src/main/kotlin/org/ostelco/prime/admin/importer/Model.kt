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
