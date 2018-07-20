package org.ostelco.prime.storage.graph

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
import org.ostelco.prime.storage.graph.Graph.read
import java.util.*
import java.util.stream.Collectors

class Neo4jStore : GraphStore by Neo4jStoreSingleton

object Neo4jStoreSingleton : GraphStore {

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
        get() = readTransaction { subscriptionStore.getAll(transaction).mapValues { it.value.balance } }

    override fun getSubscriber(id: String): Subscriber? = readTransaction { subscriberStore.get(id, transaction) }

    override fun addSubscriber(subscriber: Subscriber): Boolean = writeTransaction {
        subscriberStore.create(subscriber.id, subscriber, transaction)
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
                <-[:${segmentToSubscriberRelation.name}]-(:${segmentEntity.name})
                <-[:${offerToSegmentRelation.name}]-(:${offerEntity.name})
                -[:${offerToProductRelation.name}]->(product:${productEntity.name})
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

    override fun getBalance(id: String): Long? {
        return readTransaction {
            subscriberStore.getRelated(id, subscriptionRelation, transaction)
                    .first()
                    .balance
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
            purchaseRecordStore.create(subscriber, purchase, product, transaction)
            purchase.id
        }
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

    override fun createProductClass(productClass: ProductClass): Boolean = writeTransaction {
        productClassStore.create(productClass.id, productClass, transaction)
    }

    override fun createProduct(product: Product): Boolean =
            writeTransaction { productStore.create(product.sku, product, transaction) }

    override fun createSegment(segment: Segment): Boolean {
        return writeTransaction {
            segmentStore.create(segment.id, segment, transaction)
                    && segmentToSubscriberStore.create(segment.id, segment.subscribers, transaction)
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
        segmentToSubscriberStore.create(segment.id, segment.subscribers, transaction)
    }

    // override fun getOffers(): Collection<Offer> = offerStore.getAll().values.map { Offer().apply { id = it.id } }

    // override fun getSegments(): Collection<Segment> = segmentStore.getAll().values.map { Segment().apply { id = it.id } }

    // override fun getOffer(id: String): Offer? = offerStore.get(id)?.let { Offer().apply { this.id = it.id } }

    // override fun getSegment(id: String): Segment? = segmentStore.get(id)?.let { Segment().apply { this.id = it.id } }

    // override fun getProductClass(id: String): ProductClass? = productClassStore.get(id)
}