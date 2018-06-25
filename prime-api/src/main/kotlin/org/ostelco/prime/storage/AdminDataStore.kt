package org.ostelco.prime.storage

import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.Segment

interface AdminDataStore {

    // simple create
    fun createProductClass(productClass: ProductClass): Boolean
    fun createProduct(product: Product): Boolean
    fun createSegment(segment: Segment)
    fun createOffer(offer: Offer)

    // simple update
    // updating an Offer and Product is not allowed
    fun updateSegment(segment: Segment)

    // simple getAll
    // fun getOffers(): Collection<Offer>
    // fun getSegments(): Collection<Segment>
    // fun getSubscribers(): Collection<Subscriber>
    // fun getProducts(): Collection<Product>
    // fun getProductClasses(): Collection<ProductClass>

    // simple get by id
    // fun getOffer(id: String): Offer?
    // fun getSegment(id: String): Segment?
    // fun getProductClass(id: String): ProductClass?
}