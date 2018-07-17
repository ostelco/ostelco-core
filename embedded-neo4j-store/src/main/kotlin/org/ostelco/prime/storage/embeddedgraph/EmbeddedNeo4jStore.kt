package org.ostelco.prime.storage.embeddedgraph

import org.ostelco.prime.logger
import org.ostelco.prime.model.Entity
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.storage.GraphStore
import java.util.*
import java.util.stream.Collectors

class EmbeddedNeo4jStore : GraphStore by EmbeddedNeo4jStoreSingleton

object EmbeddedNeo4jStoreSingleton : GraphStore {

    private val LOG by logger()

    private val subscriberEntity = EntityType("Subscriber", Subscriber::class.java)
    private val subscriberStore = EntityStore(subscriberEntity)

    private val productEntity = EntityType("Product", Product::class.java)
    private val productStore = EntityStore(productEntity)

    private val subscriptionEntity = EntityType("Subscription", Subscription::class.java)
    private val subscriptionStore = EntityStore(subscriptionEntity)

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
        get() = transaction(emptyMap()) { subscriptionStore.getAll().mapValues { it.value.balance } }

    override fun getSubscriber(id: String): Subscriber? = transaction { subscriberStore.get(id) }

    override fun addSubscriber(subscriber: Subscriber): Boolean = transaction(false) { subscriberStore.create(subscriber.id, subscriber) }

    override fun updateSubscriber(subscriber: Subscriber): Boolean = transaction(false) { subscriberStore.update(subscriber.id, subscriber) }

    override fun removeSubscriber(id: String): Boolean = transaction(false) {
        subscriberStore.getRelated(id, subscriptionRelation).forEach { subscriptionStore.delete(it.msisdn) }
        subscriberStore.delete(id)
    }


    override fun addSubscription(id: String, msisdn: String): Boolean {
        return transaction(false) {
            val from = subscriberStore.get(id) ?: throw Exception("Subscriber not found")
            subscriptionStore.create(msisdn, Subscription(msisdn, 0L))
            val to = subscriptionStore.get(msisdn) ?: throw Exception("Subscription not found")
            subscriptionRelationStore.create(from, null, to)
        }
    }

    override fun getProducts(subscriberId: String): Map<String, Product> {
        val result = GraphServer.graphDb.execute(
                """
                MATCH (:${subscriberEntity.name} {id: '$subscriberId'})
                <-[:${segmentToSubscriberRelation.name}]-(:${segmentEntity.name})
                <-[:${offerToSegmentRelation.name}]-(:${offerEntity.name})
                -[:${offerToProductRelation.name}]->(product:${productEntity.name})
                RETURN properties(product) AS product
                """.trimIndent())

        return result.stream()
                .map { ObjectHandler.getObject(it["product"] as Map<String, Any>, Product::class.java) }
                .collect(Collectors.toMap({ it?.sku }, { it }))
    }

    override fun getProduct(subscriberId: String?, sku: String): Product? = transaction { productStore.get(sku) }

    override fun getBalance(id: String): Long? = transaction {
        subscriberStore.getRelated(id, subscriptionRelation)
                .first()
                .balance
    }

    override fun setBalance(msisdn: String, noOfBytes: Long): Boolean = transaction(false) {
        subscriptionStore.update(msisdn, Subscription(msisdn, balance = noOfBytes))
    }

    override fun getMsisdn(subscriptionId: String): String? = transaction {
        subscriberStore.getRelated(subscriptionId, subscriptionRelation)
                .first()
                .msisdn
    }


    override fun getPurchaseRecords(id: String): Collection<PurchaseRecord> = transaction(emptyList()) {
        subscriberStore.getRelations(id, purchaseRecordRelation)
    }

    override fun addPurchaseRecord(id: String, purchase: PurchaseRecord): String? = transaction {
        val subscriber = subscriberStore.get(id) ?: throw Exception("Subscriber not found")
        val product = productStore.get(purchase.product.sku) ?: throw Exception("Product not found")
        purchase.id = UUID.randomUUID().toString()
        purchaseRecordStore.create(subscriber, purchase, product)
        purchase.id
    }

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

    override fun createProductClass(productClass: ProductClass): Boolean = transaction(false) {
        productClassStore.create(productClass.id, productClass)
    }

    override fun createProduct(product: Product): Boolean = transaction(false) {
        productStore.create(product.sku, product)
    }

    override fun createSegment(segment: Segment): Boolean = transaction(false) {
        segmentStore.create(segment.id, segment)
        updateSegment(segment)
    }

    override fun createOffer(offer: Offer): Boolean = transaction(false) {
        offerStore.create(offer.id, offer)
                && offerToSegmentStore.create(offer.id, offer.segments)
                && offerToProductStore.create(offer.id, offer.products)
    }

    override fun updateSegment(segment: Segment): Boolean = transaction(false) {
        segmentToSubscriberStore.create(segment.id, segment.subscribers)
    }

    // override fun getOffers(): Collection<Offer> = offerStore.getAll().values.map { Offer().apply { id = it.id } }

    // override fun getSegments(): Collection<Segment> = segmentStore.getAll().values.map { Segment().apply { id = it.id } }

    // override fun getOffer(id: String): Offer? = offerStore.get(id)?.let { Offer().apply { this.id = it.id } }

    // override fun getSegment(id: String): Segment? = segmentStore.get(id)?.let { Segment().apply { this.id = it.id } }

    // override fun getProductClass(id: String): ProductClass? = productClassStore.get(id)
}
