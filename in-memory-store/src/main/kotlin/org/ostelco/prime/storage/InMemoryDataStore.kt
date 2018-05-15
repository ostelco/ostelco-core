package org.ostelco.prime.storage

import org.ostelco.prime.model.AdminProduct
import org.ostelco.prime.model.Entity
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.SubscriberV2
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This class is using the singleton class as delegate.
 * This is done because the {@link java.util.ServiceLoader} expects public no-args constructor, which is absent in Singleton.
 */
class InMemoryDataStore : DataStore by InMemorySingletonDataStore

object InMemorySingletonDataStore : DataStore {

    private val offers = ConcurrentHashMap<String, Offer>()
    private val segments = ConcurrentHashMap<String, Segment>()
    private val subscribers = ConcurrentHashMap<String, SubscriberV2>()
    private val products = ConcurrentHashMap<String, AdminProduct>()
    private val productClasses = ConcurrentHashMap<String, ProductClass>()

    private val offerStore = EntityStore(offers)
    private val segmentStore = EntityStore(segments)
    private val subscriberStore = EntityStore(subscribers)
    private val productStore = EntityStore(products)
    private val productClassStore = EntityStore(productClasses)

    override fun createOffer(offer: Offer): String = offerStore.create(offer)
    override fun createSegment(segment: Segment): String = segmentStore.create(segment)
    override fun createSubscriber(subscriber: SubscriberV2): String = subscriberStore.create(subscriber)
    override fun createProduct(product: AdminProduct): String = productStore.create(product)
    override fun createProductClass(productClass: ProductClass): String = productClassStore.create(productClass)

    override fun getOffers(): Collection<Offer> = offerStore.getAll()
    override fun getSegments(): Collection<Segment> = segmentStore.getAll()
    override fun getSubscribers(): Collection<SubscriberV2> = subscriberStore.getAll()
    override fun getProducts(): Collection<AdminProduct> = productStore.getAll()
    override fun getProductClasses(): Collection<ProductClass> = productClassStore.getAll()

    override fun getOffer(id: String) = offers[id]
    override fun getSegment(id: String) = segments[id]
    override fun getSubscriber(id: String) = subscribers[id]
    override fun getProduct(id: String) = products[id]
    override fun getProductClass(id: String) = productClasses[id]

    override fun updateSegment(segment: Segment): Boolean = segmentStore.update(segment)
    override fun updateSubscriber(subscriber: SubscriberV2): Boolean = subscriberStore.update(subscriber)
    override fun updateProductClass(productClass: ProductClass): Boolean = productClassStore.update(productClass)

    class EntityStore<E: Entity>(val map: ConcurrentHashMap<String, E>)
    {
        fun getAll(): Collection<E> = map.values

        fun create(entity: E): String {
            val id = UUID.randomUUID().toString()
            entity.id = id
            map[id] = entity
            return id
        }

        fun get(id: String): E? = map[id]

        fun update(entity: E): Boolean {
            if (map[entity.id] == null) return false
            map[entity.id] = entity
            return true
        }
    }
}