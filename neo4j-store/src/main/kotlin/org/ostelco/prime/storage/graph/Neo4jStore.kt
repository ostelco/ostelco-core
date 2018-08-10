package org.ostelco.prime.storage.graph

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.orElse
import org.neo4j.driver.v1.Transaction
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Entity
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.OcsAdminService
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.ValidationError
import org.ostelco.prime.storage.graph.Graph.read
import org.ostelco.prime.storage.graph.Relation.BELONG_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.HAS_BUNDLE
import org.ostelco.prime.storage.graph.Relation.HAS_SUBSCRIPTION
import org.ostelco.prime.storage.graph.Relation.LINKED_TO_BUNDLE
import org.ostelco.prime.storage.graph.Relation.OFFERED_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.OFFER_HAS_PRODUCT
import org.ostelco.prime.storage.graph.Relation.PURCHASED
import org.ostelco.prime.storage.graph.Relation.REFERRED
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

enum class Relation {
    HAS_SUBSCRIPTION,      // (Subscriber) -[HAS_SUBSCRIPTION]-> (Subscription)
    HAS_BUNDLE,            // (Subscriber) -[HAS_BUNDLE]-> (Bundle)
    LINKED_TO_BUNDLE,      // (Subscription) -[LINKED_TO_BUNDLE]-> (Bundle)
    PURCHASED,             // (Subscriber) -[PURCHASED]-> (Product)
    REFERRED,              // (Subscriber) -[REFERRED]-> (Subscriber)
    OFFERED_TO_SEGMENT,    // (Offer) -[OFFERED_TO_SEGMENT]-> (Segment)
    OFFER_HAS_PRODUCT,     // (Offer) -[OFFER_HAS_PRODUCT]-> (Product)
    BELONG_TO_SEGMENT      // (Subscriber) -[BELONG_TO_SEGMENT]-> (Segment)
}


class Neo4jStore : GraphStore by Neo4jStoreSingleton

object Neo4jStoreSingleton : GraphStore {

    private val ocs: OcsAdminService by lazy { getResource<OcsAdminService>() }

    //
    // Entity
    //

    private val subscriberEntity = EntityType(Subscriber::class.java)
    private val subscriberStore = EntityStore(subscriberEntity)

    private val productEntity = EntityType(Product::class.java)
    private val productStore = EntityStore(productEntity)

    private val subscriptionEntity = EntityType(Subscription::class.java)
    private val subscriptionStore = EntityStore(subscriptionEntity)

    private val bundleEntity = EntityType(Bundle::class.java)
    private val bundleStore = EntityStore(bundleEntity)

    //
    // Relation
    //

    private val subscriptionRelation = RelationType(
            relation = HAS_SUBSCRIPTION,
            from = subscriberEntity,
            to = subscriptionEntity,
            dataClass = Void::class.java)
    private val subscriptionRelationStore = RelationStore(subscriptionRelation)

    private val subscriberToBundleRelation = RelationType(
            relation = HAS_BUNDLE,
            from = subscriberEntity,
            to = bundleEntity,
            dataClass = Void::class.java)
    private val subscriberToBundleStore = RelationStore(subscriberToBundleRelation)

    private val subscriptionToBundleRelation = RelationType(
            relation = LINKED_TO_BUNDLE,
            from = subscriptionEntity,
            to = bundleEntity,
            dataClass = Void::class.java)
    private val subscriptionToBundleStore = RelationStore(subscriptionToBundleRelation)

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

    // -------------
    // Client Store
    // -------------

    //
    // Balance (Subscriber - Bundle)
    //

    override fun getBundles(subscriberId: String): Either<StoreError, Collection<Bundle>?> = readTransaction {
        subscriberStore.getRelated(subscriberId, subscriberToBundleRelation, transaction)
    }

    override fun updateBundle(bundle: Bundle): Option<StoreError> = writeTransaction {
        bundleStore.update(bundle, transaction)
                .ifFailedThenRollback(transaction)
    }

    //
    // Subscriber
    //

    override fun getSubscriber(subscriberId: String): Either<StoreError, Subscriber> =
            readTransaction { subscriberStore.get(subscriberId, transaction) }

    // TODO vihang: Move this logic to DSL + Rule Engine + Triggers, when they are ready
    override fun addSubscriber(subscriber: Subscriber, referredBy: String?): Option<StoreError> = writeTransaction {

        if (subscriber.id == referredBy) {
            return@writeTransaction Option(ValidationError(
                    type = subscriberEntity.name,
                    id = subscriber.id,
                    message = "Referred by self"))
        }

        val bundleId = subscriber.id

        val failed = subscriberStore.create(subscriber, transaction)
        if (referredBy != null) {
            // Give 1 GB if subscriber is referred
            failed
                    .ifSuccessThen { referredRelationStore.create(referredBy, subscriber.id, transaction) }
                    .ifSuccessThen { bundleStore.create(Bundle(bundleId, 1_000_000_000), transaction) }
                    .ifSuccessThen {
                        productStore
                                .get("1GB_FREE_ON_REFERRED", transaction)
                                .flatMap {
                                    createPurchaseRecordRelation(
                                            subscriber.id,
                                            PurchaseRecord(product = it, timestamp = Instant.now().toEpochMilli()),
                                            transaction)
                                }
                                .swap().toOption()
                    }
                    .ifSuccessThen {
                        ocs.addBundle(Bundle(bundleId, 1_000_000_000))
                        None
                    }
        } else {
            // Give 100 MB as free initial balance
            failed
                    .ifSuccessThen { bundleStore.create(Bundle(bundleId, 100_000_000), transaction) }
                    .ifSuccessThen {
                        productStore
                                .get("100MB_FREE_ON_JOINING", transaction)
                                .map {
                                    createPurchaseRecordRelation(
                                            subscriber.id,
                                            PurchaseRecord(product = it, timestamp = Instant.now().toEpochMilli()),
                                            transaction)
                                }
                                .fold({ Option(it) }, { None })
                    }
                    .ifSuccessThen {
                        ocs.addBundle(Bundle(bundleId, 100_000_000))
                        None
                    }
        }.ifSuccessThen { subscriberToBundleStore.create(subscriber.id, bundleId, transaction) }
                .ifSuccessThen { subscriberToSegmentStore.create(subscriber.id, "all", transaction) }
                .ifFailedThenRollback(transaction)
    }

    override fun updateSubscriber(subscriber: Subscriber): Option<StoreError> = writeTransaction {
        subscriberStore.update(subscriber, transaction)
                .ifFailedThenRollback(transaction)
    }

    override fun removeSubscriber(subscriberId: String): Option<StoreError> = writeTransaction {
        subscriberStore.exists(subscriberId, transaction)
                .ifSuccessThen {
                    subscriberStore.getRelated(subscriberId, subscriberToBundleRelation, transaction)
                            .map { it.forEach { bundle -> bundleStore.delete(bundle.id, transaction) } }
                    subscriberStore.getRelated(subscriberId, subscriptionRelation, transaction)
                            .map { it.forEach { subscription -> subscriptionStore.delete(subscription.id, transaction) } }
                    None
                }
                .ifSuccessThen { subscriberStore.delete(subscriberId, transaction) }
                .ifFailedThenRollback(transaction)
    }

    //
    // Subscription
    //

    override fun addSubscription(subscriberId: String, msisdn: String): Option<StoreError> = writeTransaction {

        subscriberStore.getRelated(subscriberId, subscriberToBundleRelation, transaction)
                .flatMap { bundles ->
                    if (bundles.isEmpty()) {
                        Either.left(NotFoundError(type = subscriberToBundleRelation.relation.name, id = "$subscriberId -> *"))
                    } else {
                        Either.right(bundles)
                    }
                }
                .flatMap { bundles ->
                    subscriptionStore.create(Subscription(msisdn), transaction)
                            .swapToEither { bundles }
                }
                .flatMap { bundles ->
                    subscriptionStore.get(msisdn, transaction)
                            .map { subscription -> Pair(bundles, subscription) }
                }
                .flatMap { (bundles, subscription) ->
                    subscriberStore.get(subscriberId, transaction)
                            .map { subscriber -> Triple(bundles, subscription, subscriber) }
                }
                .flatMap { (bundles, subscription, subscriber) ->
                    bundles.fold(None as Option<StoreError>) { failed, bundle ->
                        failed.ifSuccessThen {
                            subscriptionToBundleStore.create(subscription, bundle, transaction)
                                    .ifSuccessThen {
                                        ocs.addMsisdnToBundleMapping(msisdn, bundle.id)
                                        None
                                    }
                        }
                    }.swapToEither { Pair(subscription, subscriber) }
                }
                .flatMap { (subscription, subscriber) ->
                    subscriptionRelationStore.create(subscriber, subscription, transaction)
                            .swapToEither { None }
                }
                .swap()
                .toOption()
                .ifFailedThenRollback(transaction)
    }

    override fun getSubscriptions(subscriberId: String): Either<StoreError, Collection<Subscription>> =
            readTransaction { subscriberStore.getRelated(subscriberId, subscriptionRelation, transaction) }

    override fun getMsisdn(subscriptionId: String): Either<StoreError, String> {
        return readTransaction {
            subscriberStore.getRelated(subscriptionId, subscriptionRelation, transaction)
                    .map { it.first().msisdn }
        }
    }

    //
    // Products
    //

    override fun getProducts(subscriberId: String): Either<StoreError, Map<String, Product>> {
        return readTransaction {

            subscriberStore.exists(subscriberId, transaction)
                    .swapToEither { emptyMap<String, Product>() }
                    .ifSuccessThen {
                        read("""
                            MATCH (:${subscriberEntity.name} {id: '$subscriberId'})
                            -[:${subscriberToSegmentRelation.relation.name}]->(:${segmentEntity.name})
                            <-[:${offerToSegmentRelation.relation.name}]-(:${offerEntity.name})
                            -[:${offerToProductRelation.relation.name}]->(product:${productEntity.name})
                            RETURN product;
                            """.trimIndent(),
                                transaction) { statementResult ->
                            Either.right(statementResult
                                    .list { productEntity.createEntity(it["product"].asMap()) }
                                    .stream()
                                    .collect(Collectors.toMap({ it?.sku }, { it })))
                        }
                    }
        }
    }

    override fun getProduct(subscriberId: String, sku: String): Either<StoreError, Product> {
        return readTransaction {
            subscriberStore.exists(subscriberId, transaction)
                    .swapToEither { Product() }
                    .ifSuccessThen {
                        read("""
                            MATCH (:${subscriberEntity.name} {id: '$subscriberId'})
                            -[:${subscriberToSegmentRelation.relation.name}]->(:${segmentEntity.name})
                            <-[:${offerToSegmentRelation.relation.name}]-(:${offerEntity.name})
                            -[:${offerToProductRelation.relation.name}]->(product:${productEntity.name} {sku: '$sku'})
                            RETURN product;
                            """.trimIndent(),
                                transaction) { statementResult ->
                            if (statementResult.hasNext()) {
                                Either.right(productEntity.createEntity(statementResult.single().get("product").asMap()))
                            } else {
                                Either.left(NotFoundError(type = productEntity.name, id = sku))
                            }
                        }
                    }
        }
    }

    //
    // Purchase Records
    //

    override fun getPurchaseRecords(subscriberId: String): Either<StoreError, Collection<PurchaseRecord>> {
        return readTransaction {
            subscriberStore.getRelations(subscriberId, purchaseRecordRelation, transaction)
        }
    }

    override fun addPurchaseRecord(subscriberId: String, purchase: PurchaseRecord): Either<StoreError, String> {
        return writeTransaction {
            createPurchaseRecordRelation(subscriberId, purchase, transaction)
                    .ifFailedThenRollback(transaction)
        }
    }

    private fun createPurchaseRecordRelation(
            subscriberId: String,
            purchase: PurchaseRecord,
            transaction: Transaction): Either<StoreError, String> {

        return subscriberStore.get(subscriberId, transaction).flatMap { subscriber ->
            productStore.get(purchase.product.sku, transaction).flatMap { product ->

                purchase.id = UUID.randomUUID().toString()
                purchaseRecordRelationStore.create(subscriber, purchase, product, transaction)
                        .toEither { purchase.id }
                        .swap()
            }
        }
    }

    //
    // Referrals
    //

    override fun getReferrals(subscriberId: String): Either<StoreError, Collection<String>> = readTransaction {
        subscriberStore.getRelated(subscriberId, referredRelation, transaction)
                .map { list -> list.map { it.name } }
    }

    override fun getReferredBy(subscriberId: String): Either<StoreError, String?> = readTransaction {
        subscriberStore.getRelatedFrom(subscriberId, referredRelation, transaction)
                .map { it.singleOrNull()?.name }
    }

    // ------------
    // Admin Store
    // ------------

    //
    // Balance (Subscriber - Subscription - Bundle)
    //

    override fun getMsisdnToBundleMap(): Map<Subscription, Bundle> = readTransaction {
        read("""
                MATCH (subscription:${subscriptionEntity.name})-[:${subscriptionToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})<-[:${subscriberToBundleRelation.relation.name}]-(:${subscriberEntity.name})
                RETURN subscription, bundle
                """.trimIndent(),
                transaction) { result ->
            result.list {
                Pair(ObjectHandler.getObject(it["subscription"].asMap(), Subscription::class.java),
                        ObjectHandler.getObject(it["bundle"].asMap(), Bundle::class.java))
            }.toMap()
        }
    }

    override fun getAllBundles(): Collection<Bundle> = readTransaction {
        read("""
                MATCH (:${subscriberEntity.name})-[:${subscriberToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})<-[:${subscriptionToBundleRelation.relation.name}]-(:${subscriptionEntity.name})
                RETURN bundle
                """.trimIndent(),
                transaction) { result ->
            result.list {
                ObjectHandler.getObject(it["bundle"].asMap(), Bundle::class.java)
            }.toSet()
        }
    }

    override fun getSubscriberToBundleIdMap(): Map<Subscriber, Bundle> = readTransaction {
        read("""
                MATCH (subscriber:${subscriberEntity.name})-[:${subscriberToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})
                RETURN subscriber, bundle
                """.trimIndent(),
                transaction) { result ->
            result.list {
                Pair(ObjectHandler.getObject(it["subscriber"].asMap(), Subscriber::class.java),
                        ObjectHandler.getObject(it["bundle"].asMap(), Bundle::class.java))
            }.toMap()
        }
    }

    override fun getSubscriberToMsisdnMap(): Map<Subscriber, Subscription> = readTransaction {
        read("""
                MATCH (subscriber:${subscriberEntity.name})-[:${subscriptionRelation.relation.name}]->(subscription:${subscriptionEntity.name})
                RETURN subscriber, subscription
                """.trimIndent(),
                transaction) { result ->
            result.list {
                Pair(ObjectHandler.getObject(it["subscriber"].asMap(), Subscriber::class.java),
                        ObjectHandler.getObject(it["subscription"].asMap(), Subscription::class.java))
            }.toMap()
        }
    }

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

    override fun createProductClass(productClass: ProductClass): Option<StoreError> = writeTransaction {
        productClassStore.create(productClass, transaction)
                .ifFailedThenRollback(transaction)
    }

    override fun createProduct(product: Product): Option<StoreError> = writeTransaction {
        productStore.create(product, transaction)
                .ifFailedThenRollback(transaction)
    }

    override fun createSegment(segment: Segment): Option<StoreError> {
        return writeTransaction {
            segmentStore.create(segment, transaction)
                    .ifSuccessThen { subscriberToSegmentStore.create(segment.subscribers, segment.id, transaction) }
                    .ifFailedThenRollback(transaction)
        }
    }

    override fun createOffer(offer: Offer): Option<StoreError> = writeTransaction {
        offerStore
                .create(offer, transaction)
                .ifSuccessThen { offerToSegmentStore.create(offer.id, offer.segments, transaction) }
                .ifSuccessThen { offerToProductStore.create(offer.id, offer.products, transaction) }
                .ifFailedThenRollback(transaction)
    }

    override fun updateSegment(segment: Segment): Option<StoreError> = writeTransaction {
        subscriberToSegmentStore.create(segment.id, segment.subscribers, transaction)
                .ifFailedThenRollback(transaction)
    }

// override fun getOffers(): Collection<Offer> = offerStore.getAll().values.map { Offer().apply { id = it.id } }

// override fun getSegments(): Collection<Segment> = segmentStore.getAll().values.map { Segment().apply { id = it.id } }

// override fun getOffer(id: String): Offer? = offerStore.get(id)?.let { Offer().apply { this.id = it.id } }

// override fun getSegment(id: String): Segment? = segmentStore.get(id)?.let { Segment().apply { this.id = it.id } }

// override fun getProductClass(id: String): ProductClass? = productClassStore.get(id)
}

fun <A> Option<A>.ifSuccessThen(next: () -> Option<A>): Option<A> = this.orElse(next)
fun <A> Option<A>.ifFailedThenRollback(transaction: Transaction): Option<A> {
    if (this.nonEmpty()) {
        transaction.failure()
    }
    return this
}

fun <L, R> Option<L>.swapToEither(right: () -> R): Either<L, R> = this.toEither(right).swap()

fun <L, R> Either<L, R>.ifSuccessThen(next: () -> Either<L, R>): Either<L, R> = this.fold({ Either.left(it) }, { next() })
fun <L, R> Either<L, R>.ifFailedThenRollback(transaction: Transaction): Either<L, R> {
    if (this.isLeft()) {
        transaction.failure()
    }
    return this
}