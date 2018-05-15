package org.ostelco.prime.storage

import org.ostelco.prime.model.AdminProduct
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.SubscriberV2

interface DataStore {

    fun createOffer(offer: Offer): String
    fun createSegment(segment: Segment): String
    fun createSubscriber(subscriber: SubscriberV2): String
    fun createProduct(product: AdminProduct): String
    fun createProductClass(productClass: ProductClass): String

    fun getOffers(): Collection<Offer>
    fun getSegments(): Collection<Segment>
    fun getSubscribers(): Collection<SubscriberV2>
    fun getProducts(): Collection<AdminProduct>
    fun getProductClasses(): Collection<ProductClass>

    fun getOffer(id: String): Offer?
    fun getSegment(id: String): Segment?
    fun getSubscriber(id: String): SubscriberV2?
    fun getProduct(id: String): AdminProduct?
    fun getProductClass(id: String): ProductClass?

    // updating an Offer and Product is not allowed
    fun updateSegment(segment: Segment): Boolean
    fun updateSubscriber(subscriber: SubscriberV2): Boolean
    fun updateProductClass(productClass: ProductClass): Boolean
}