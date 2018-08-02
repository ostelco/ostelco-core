package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.Entity
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.graph.Graph.read
import org.ostelco.prime.storage.graph.Relation.BELONG_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.HAS_SUBSCRIPTION
import org.ostelco.prime.storage.graph.Relation.OFFERED_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.OFFER_HAS_PRODUCT
import org.ostelco.prime.storage.graph.Relation.PURCHASED
import org.ostelco.prime.storage.graph.Relation.REFERRED
import java.util.*
import java.util.stream.Collectors

enum class Relation {
    HAS_SUBSCRIPTION,      // (Subscriber) -[HAS_SUBSCRIPTION]-> (Subscription)
    PURCHASED,             // (Subscriber) -[PURCHASED]-> (Product)
    REFERRED,              // (Subscriber) -[REFERRED]-> (Subscriber)
    OFFERED_TO_SEGMENT,    // (Offer) -[OFFERED_TO_SEGMENT]-> (Segment)
    OFFER_HAS_PRODUCT,     // (Offer) -[OFFER_HAS_PRODUCT]-> (Product)
    BELONG_TO_SEGMENT      // (Subscriber) -[BELONG_TO_SEGMENT]-> (Segment)
}


class Neo4jStore : GraphStore by Neo4jStoreSingleton

object Neo4jStoreSingleton : GraphStore {

    private val subscriberEntity = EntityType(Subscriber::class.java)
    private val subscriberStore = EntityStore(subscriberEntity)

    private val productEntity = EntityType(Product::class.java)
    private val productStore = EntityStore(productEntity)

    private val subscriptionEntity = EntityType(Subscription::class.java)
    private val subscriptionStore = EntityStore(subscriptionEntity)

    private val subscriptionRelation = RelationType(
            relation = HAS_SUBSCRIPTION,
            from = subscriberEntity,
            to = subscriptionEntity,
            dataClass = Void::class.java)
    private val subscriptionRelationStore = RelationStore(subscriptionRelation)

    private val purchaseRecordRelation = RelationType(
            relation = PURCHASED,
            from = subscriberEntity,
            to = productEntity,
            dataClass = PurchaseRecord::class.java)
    private val purchaseRecordRelationStore = RelationStore(purchaseRecordRelation)

    private val referredRelation = RelationType(
            relation = REFERRED,
            from = subscriberEntity,
            to = subscriberEntity,
            dataClass = Void::class.java)
    private val referredRelationStore = RelationStore(referredRelation)

    override val balances: Map<String, Long>
        get() = readTransaction { subscriptionStore.getAll(transaction).mapValues { it.value.balance } }

    override fun getSubscriber(id: String): Subscriber? = readTransaction { subscriberStore.get(id, transaction) }

    override fun addSubscriber(subscriber: Subscriber, referredBy: String?): Boolean = writeTransaction {
        subscriberStore.create(subscriber.id, subscriber, transaction)
                && referredBy?.let { referredRelationStore.create(it, subscriber.id, transaction) } ?: true
    }

    override fun updateSubscriber(subscriber: Subscriber): Boolean = writeTransaction {
        subscriberStore.update(subscriber.id, subscriber, transaction)
    }

    override fun removeSubscriber(id: String) = writeTransaction { subscriberStore.delete(id, transaction) }

    override fun addSubscription(id: String, msisdn: String): Boolean {
        return writeTransaction {
            val from = subscriberStore.get(id, transaction) ?: return@writeTransaction false
            subscriptionStore.create(msisdn, Subscription(msisdn, 0L), transaction)
            val to = subscriptionStore.get(msisdn, transaction) ?: return@writeTransaction false
            subscriptionRelationStore.create(from, null, to, transaction)
        }
    }

    override fun getProducts(subscriberId: String): Map<String, Product> {
        return readTransaction {
            read("""
                MATCH (:${subscriberEntity.name} {id: '$subscriberId'})
                -[:${subscriberToSegmentRelation.relation.name}]->(:${segmentEntity.name})
                <-[:${offerToSegmentRelation.relation.name}]-(:${offerEntity.name})
                -[:${offerToProductRelation.relation.name}]->(product:${productEntity.name})
                RETURN product;
                """.trimIndent(),
                    transaction) {

                it.list { ObjectHandler.getObject(it["product"].asMap(), Product::class.java) }
                        .stream()
                        .collect(Collectors.toMap({ it?.sku }, { it }))
            }
        }
    }

    override fun getProduct(subscriberId: String?, sku: String): Product? =
            readTransaction { productStore.get(sku, transaction) }

    override fun getSubscriptions(id: String): Collection<Subscription>? {
        return readTransaction {
            subscriberStore.getRelated(id, subscriptionRelation, transaction)
        }
    }

    override fun setBalance(msisdn: String, noOfBytes: Long): Boolean = readTransaction {
        subscriptionStore.update(msisdn, Subscription(msisdn, balance = noOfBytes), transaction)
    }

    override fun getMsisdn(subscriptionId: String): String? {
        return readTransaction {
            subscriberStore.getRelated(subscriptionId, subscriptionRelation, transaction)
                    .first()
                    .msisdn
        }
    }

    override fun getPurchaseRecords(id: String): Collection<PurchaseRecord> {
        return readTransaction {
            subscriberStore.getRelations(id, purchaseRecordRelation, transaction)
        }
    }

    override fun addPurchaseRecord(id: String, purchase: PurchaseRecord): String? {
        return writeTransaction {
            val subscriber = subscriberStore.get(id, transaction) ?: throw Exception("Subscriber not found")
            val product = productStore.get(purchase.product.sku, transaction) ?: throw Exception("Product not found")
            purchase.id = UUID.randomUUID().toString()
            purchaseRecordRelationStore.create(subscriber, purchase, product, transaction)
            purchase.id
        }
    }

    override fun getReferrals(id: String): Collection<String> = readTransaction {
        subscriberStore.getRelated(id, referredRelation, transaction).map { it.name }
    }

    override fun getReferredBy(id: String): String? = readTransaction {
        subscriberStore.getRelatedFrom(id, referredRelation, transaction).singleOrNull()?.name
    }

    //
    // Admin Store
    //

    private val offerEntity = EntityType(Entity::class.java, "Offer")
    private val offerStore = EntityStore(offerEntity)

    private val segmentEntity = EntityType(Entity::class.java, "Segment")
    private val segmentStore = EntityStore(segmentEntity)

    private val offerToSegmentRelation = RelationType(OFFERED_TO_SEGMENT, offerEntity, segmentEntity, Void::class.java)
    private val offerToSegmentStore = RelationStore(offerToSegmentRelation)

    private val offerToProductRelation = RelationType(OFFER_HAS_PRODUCT, offerEntity, productEntity, Void::class.java)
    private val offerToProductStore = RelationStore(offerToProductRelation)

    private val subscriberToSegmentRelation = RelationType(BELONG_TO_SEGMENT, subscriberEntity, segmentEntity, Void::class.java)
    private val subscriberToSegmentStore = RelationStore(subscriberToSegmentRelation)

    private val productClassEntity = EntityType(ProductClass::class.java)
    private val productClassStore = EntityStore(productClassEntity)

    override fun createProductClass(productClass: ProductClass): Boolean = writeTransaction {
        productClassStore.create(productClass.id, productClass, transaction)
    }

    override fun createProduct(product: Product): Boolean =
            writeTransaction { productStore.create(product.sku, product, transaction) }

    override fun createSegment(segment: Segment): Boolean {
        return writeTransaction {
            segmentStore.create(segment.id, segment, transaction)
                    && subscriberToSegmentStore.create(segment.subscribers, segment.id, transaction)
        }
    }

    override fun createOffer(offer: Offer): Boolean = writeTransaction {
        //         offerStore.create(offer.id, offer)
//                && offerToSegmentStore.create(offer.id, offer.segments)
//                && offerToProductStore.create(offer.id, offer.products)

        val result = offerStore.create(offer.id, offer, transaction)
        val result2 = result && offerToSegmentStore.create(offer.id, offer.segments, transaction)
        val result3 = result && offerToProductStore.create(offer.id, offer.products, transaction)
        result && result2 && result3
    }

    override fun updateSegment(segment: Segment): Boolean = writeTransaction {
        subscriberToSegmentStore.create(segment.id, segment.subscribers, transaction)
    }

    // override fun getOffers(): Collection<Offer> = offerStore.getAll().values.map { Offer().apply { id = it.id } }

    // override fun getSegments(): Collection<Segment> = segmentStore.getAll().values.map { Segment().apply { id = it.id } }

    // override fun getOffer(id: String): Offer? = offerStore.get(id)?.let { Offer().apply { this.id = it.id } }

    // override fun getSegment(id: String): Segment? = segmentStore.get(id)?.let { Segment().apply { this.id = it.id } }

    // override fun getProductClass(id: String): ProductClass? = productClassStore.get(id)
}