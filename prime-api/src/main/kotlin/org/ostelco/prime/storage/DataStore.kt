package org.ostelco.prime.storage

import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber

interface DataStore {

    fun createOffer(offer: Offer): String
    fun createSegment(segment: Segment): String
    fun createSubscriber(subscriber: Subscriber): String
    fun createProduct(product: Product): String
    fun createProductClass(productClass: ProductClass): String

    fun getOffers(): Collection<Offer>
    fun getSegments(): Collection<Segment>
    fun getSubscribers(): Collection<Subscriber>
    fun getProducts(): Collection<Product>
    fun getProductClasses(): Collection<ProductClass>

    fun getOffer(id: String): Offer?
    fun getSegment(id: String): Segment?
    fun getSubscriber(id: String): Subscriber?
    fun getProduct(id: String): Product?
    fun getProductClass(id: String): ProductClass?

    // updating an Offer and Product is not allowed
    fun updateSegment(segment: Segment): Boolean
    fun updateSubscriber(subscriber: Subscriber): Boolean
    fun updateProductClass(productClass: ProductClass): Boolean
}