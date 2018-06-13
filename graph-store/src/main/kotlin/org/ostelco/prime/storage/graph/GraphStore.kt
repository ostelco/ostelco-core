package org.ostelco.prime.storage.graph

import org.neo4j.driver.v1.AccessMode.READ
import org.ostelco.prime.logger
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Entity
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.storage.AdminDataStore
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.prime.storage.legacy.StorageException
import java.util.*
import java.util.stream.Collectors

class GraphStore : Storage by GraphStoreSingleton, AdminDataStore by GraphStoreSingleton

object GraphStoreSingleton : Storage, AdminDataStore {

    private val LOG by logger()

    private val subscriberEntity = EntityType("Subscriber", Subscriber::class.java)
    private val subscriberStore = EntityStore(subscriberEntity)

    private val productEntity = EntityType("Product", Product::class.java)
    private val productStore = EntityStore(productEntity)

    private val subscriptionEntity = EntityType("Subscription", Subscription::class.java)
    private val subscriptionStore = EntityStore(subscriptionEntity)

    private val notificationTokenEntity = EntityType("NotificationToken", ApplicationToken::class.java)
    private val notificationTokenStore = EntityStore(notificationTokenEntity)

    private val subscriptionRelation = RelationType(
            name = "HAS_SUBSCRIPTION",
            from = subscriberEntity,
            to = subscriptionEntity,
            dataClass = Void::class.java)
    private val subscriptionRelationStore = RelationStore(subscriptionRelation)

    private val purchaseRecordRelation = RelationType(
            name = "PURCHASED",
            from = subscriberEntity,
            to = productEntity,
            dataClass = PurchaseRecord::class.java)
    private val purchaseRecordStore = RelationStore(purchaseRecordRelation)

    override val balances: Map<String, Long>
        get() = subscriptionStore.getAll().mapValues { it.value.balance }

    override fun getSubscriber(id: String): Subscriber? = subscriberStore.get(id)

    override fun addSubscriber(subscriber: Subscriber): Boolean = subscriberStore.create(subscriber.id, subscriber)

    override fun updateSubscriber(subscriber: Subscriber): Boolean = subscriberStore.update(subscriber.id, subscriber)

    override fun removeSubscriber(id: String) = subscriberStore.delete(id)

    override fun addSubscription(id: String, msisdn: String): Boolean {
        val from = subscriberStore.get(id) ?: return false
        subscriptionStore.create(msisdn, Subscription(msisdn, 0L))
        val to = subscriptionStore.get(msisdn) ?: return false
        return subscriptionRelationStore.create(from, null, to)
    }

    override fun getProducts(subscriberId: String): Map<String, Product> {
        val tx = Neo4jClient
                .driver
                .session(READ)
                .beginTransaction()

        try {
            val result = tx.run(
                    """
                MATCH (:${subscriberEntity.name} {id: '$subscriberId'})
                <-[:${segmentToSubscriberRelation.name}]-(:${segmentEntity.name})
                <-[:${offerToSegmentRelation.name}]-(:${offerEntity.name})
                -[:${offerToProductRelation.name}]->(product:${productEntity.name})
                RETURN properties(product) AS product
                """.trimIndent())

            tx.success()

            return result.list { ObjectHandler.getObject(it.asMap(), Product::class.java) }
                    .stream()
                    .collect(Collectors.toMap({ it?.sku }, { it }))
        } catch (e: Exception) {
            LOG.error("Failed to fetch products", e)
            return emptyMap()
        } finally {
            tx.failure()
        }
    }

    override fun getProduct(subscriberId: String?, sku: String): Product? = productStore.get(sku)

    override fun getBalance(id: String): Long? {
        return subscriberStore.getRelated(id, subscriptionRelation, subscriptionEntity)
                .first()
                .balance
    }

    override fun setBalance(msisdn: String, noOfBytes: Long): Boolean =
            subscriptionStore.update(msisdn, Subscription(msisdn, balance = noOfBytes))

    override fun getMsisdn(subscriptionId: String): String? {
        return subscriberStore.getRelated(subscriptionId, subscriptionRelation, subscriptionEntity)
                .first()
                .msisdn
    }

    override fun getPurchaseRecords(id: String): Collection<PurchaseRecord> {
        return subscriberStore.getRelations(id, purchaseRecordRelation)
    }

    override fun addPurchaseRecord(id: String, purchase: PurchaseRecord): String? {
        val subscriber = subscriberStore.get(id) ?: throw StorageException("Subscriber not found")
        val product = productStore.get(purchase.product.sku) ?: throw StorageException("Product not found")
        purchase.id = UUID.randomUUID().toString()
        purchaseRecordStore.create(subscriber, purchase, product)
        return purchase.id
    }

    override fun getNotificationTokens(msisdn: String): Collection<ApplicationToken> = notificationTokenStore.getAll().values

    override fun addNotificationToken(msisdn: String, token: ApplicationToken): Boolean = notificationTokenStore.create("$msisdn.${token.applicationID}", token)

    override fun getNotificationToken(msisdn: String, applicationID: String): ApplicationToken? = notificationTokenStore.get("$msisdn.$applicationID")

    //
    // Admin Store
    //

    private val offerEntity = EntityType("Offer", Entity::class.java)
    private val offerStore = EntityStore(offerEntity)

    private val segmentEntity = EntityType("Segment", Entity::class.java)
    private val segmentStore = EntityStore(segmentEntity)

    private val offerToSegmentRelation = RelationType("offerHasSegment", offerEntity, segmentEntity, Void::class.java)
    private val offerToSegmentStore = RelationStore(offerToSegmentRelation)

    private val offerToProductRelation = RelationType("offerHasProduct", offerEntity, productEntity, Void::class.java)
    private val offerToProductStore = RelationStore(offerToProductRelation)

    private val segmentToSubscriberRelation = RelationType("segmentToSubscriber", segmentEntity, subscriberEntity, Void::class.java)
    private val segmentToSubscriberStore = RelationStore(segmentToSubscriberRelation)

    private val productClassEntity = EntityType("ProductClass", ProductClass::class.java)
    private val productClassStore = EntityStore(productClassEntity)

    override fun createProductClass(productClass: ProductClass): Boolean = productClassStore.create(productClass.id, productClass)

    override fun createProduct(product: Product): Boolean = productStore.create(product.sku, product)

    override fun createSegment(segment: Segment) {
        segmentStore.create(segment.id, segment)
        updateSegment(segment)
    }

    override fun createOffer(offer: Offer) {
        offerStore.create(offer.id, offer)
        offerToSegmentStore.create(offer.id, offer.segments)
        offerToProductStore.create(offer.id, offer.products)
    }

    override fun updateSegment(segment: Segment) {
        segmentToSubscriberStore.create(segment.id, segment.subscribers)
    }

    // override fun getOffers(): Collection<Offer> = offerStore.getAll().values.map { Offer().apply { id = it.id } }

    // override fun getSegments(): Collection<Segment> = segmentStore.getAll().values.map { Segment().apply { id = it.id } }

    // override fun getOffer(id: String): Offer? = offerStore.get(id)?.let { Offer().apply { this.id = it.id } }

    // override fun getSegment(id: String): Segment? = segmentStore.get(id)?.let { Segment().apply { this.id = it.id } }

    // override fun getProductClass(id: String): ProductClass? = productClassStore.get(id)
}