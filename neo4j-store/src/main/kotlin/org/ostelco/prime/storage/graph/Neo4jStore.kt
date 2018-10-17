package org.ostelco.prime.storage.graph

import arrow.core.*
import arrow.effects.*
import arrow.instances.*
import arrow.typeclasses.*
import org.neo4j.driver.v1.Transaction
import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.ChangeSegment
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.ocs.OcsAdminService
import org.ostelco.prime.ocs.OcsSubscriberService
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.*
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.NotCreatedError
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
import java.util.UUID
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

    private val ocsAdminService: OcsAdminService by lazy { getResource<OcsAdminService>() }
    private val logger by getLogger()

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

    override fun getBundles(subscriberId: String): Either<StoreError, Collection<Bundle>> = readTransaction {
        subscriberStore.getRelated(subscriberId, subscriberToBundleRelation, transaction)
    }

    override fun updateBundle(bundle: Bundle): Either<StoreError, Unit> = writeTransaction {
        bundleStore.update(bundle, transaction)
                .ifFailedThenRollback(transaction)
    }

    //
    // Subscriber
    //

    override fun getSubscriber(subscriberId: String): Either<StoreError, Subscriber> =
            readTransaction { subscriberStore.get(subscriberId, transaction) }


    private fun transformCreateSubscriberError(subscriber: Subscriber, storeError: StoreError) =
            if (storeError is NotCreatedError && storeError.type == subscriberToSegmentRelation.relation.name) {
                ValidationError(type = subscriberEntity.name, id = subscriber.id, message = "Unsupported country: ${subscriber.country}")
            } else {
                storeError
            }

    private fun validateCreateSubscriberParams(subscriber: Subscriber, referredBy: String?): Either<StoreError, Unit> =
            if (subscriber.id == referredBy) {
                Either.left(ValidationError(type = subscriberEntity.name, id = subscriber.id, message = "Referred by self"))
            } else {
                Either.right(Unit)
            }

    override fun addSubscriber(subscriber: Subscriber, referredBy: String?): Either<StoreError, Unit> = writeTransaction {
        addSubscriberIO(subscriber, referredBy, transaction)
                .unsafeRunSync()
                .mapLeft { transformCreateSubscriberError(subscriber, it) }
                .ifFailedThenRollback(transaction)
    }

    // TODO vihang: Move this logic to DSL + Rule Engine + Triggers, when they are ready
    // >> BEGIN
    private fun addSubscriberIO(subscriber: Subscriber, referredBy: String?, transaction: PrimeTransaction): IO<Either<StoreError, Unit>> = IO {
        ForEither<StoreError>() extensions {
            binding {
                validateCreateSubscriberParams(subscriber, referredBy).bind()
                val bundleId = subscriber.id
                subscriberStore.create(subscriber, transaction).bind()
                subscriberToSegmentStore.create(subscriber.id,
                        getSegmentNameFromCountryCode(subscriber.country),
                        transaction).bind()
                // Give 100 MB as free initial balance
                var productId: String = "100MB_FREE_ON_JOINING"
                var balance: Long = 100_000_000
                if (referredBy != null) {
                    // Give 1 GB if subscriber is referred
                    productId = "1GB_FREE_ON_REFERRED"
                    balance = 1_000_000_000
                    referredRelationStore.create(referredBy, subscriber.id, transaction).bind()
                }
                bundleStore.create(Bundle(bundleId, balance), transaction).bind()
                val product = productStore.get(productId, transaction).bind()
                createPurchaseRecordRelation(
                        subscriber.id,
                        PurchaseRecord(id = UUID.randomUUID().toString(), product = product, timestamp = Instant.now().toEpochMilli(), msisdn = ""),
                        transaction)
                ocsAdminService.addBundle(Bundle(bundleId, balance))
                subscriberToBundleStore.create(subscriber.id, bundleId, transaction).bind()
                if (subscriber.country.equals("sg", ignoreCase = true)) {
                    logger.info(NOTIFY_OPS_MARKER, "Created a new user with email: ${subscriber.email} for Singapore.\nProvision a SIM card for this user.")
                }
            }.fix()
        }
    }
    // << END

    override fun updateSubscriber(subscriber: Subscriber): Either<StoreError, Unit> = writeTransaction {
        subscriberStore.update(subscriber, transaction)
                .ifFailedThenRollback(transaction)
    }

    override fun removeSubscriber(subscriberId: String): Either<StoreError, Unit> = writeTransaction {
        subscriberStore.exists(subscriberId, transaction)
                .flatMap { _ ->
                    subscriberStore.getRelated(subscriberId, subscriberToBundleRelation, transaction)
                            .map { it.forEach { bundle -> bundleStore.delete(bundle.id, transaction) } }
                    subscriberStore.getRelated(subscriberId, subscriptionRelation, transaction)
                            .map { it.forEach { subscription -> subscriptionStore.delete(subscription.id, transaction) } }
                }
                .flatMap { subscriberStore.delete(subscriberId, transaction) }
                .ifFailedThenRollback(transaction)
    }

    //
    // Subscription
    //
    private fun validateBundleList(bundles: List<Bundle>, subscriberId: String): Either<StoreError, Unit> =
            if (bundles.isEmpty()) {
                Either.left(NotFoundError(type = subscriberToBundleRelation.relation.name, id = "$subscriberId -> *"))
            } else {
                Either.right(Unit)
            }

    override fun addSubscription(subscriberId: String, msisdn: String): Either<StoreError, Unit> = writeTransaction {
        IO {
            ForEither<StoreError>() extensions {
                binding {
                    val bundles = subscriberStore.getRelated(subscriberId, subscriberToBundleRelation, transaction).bind()
                    validateBundleList(bundles, subscriberId).bind()
                    subscriptionStore.create(Subscription(msisdn), transaction).bind()
                    val subscription = subscriptionStore.get(msisdn, transaction).bind()
                    val subscriber = subscriberStore.get(subscriberId, transaction).bind()
                    bundles.forEach { bundle ->
                        subscriptionToBundleStore.create(subscription, bundle, transaction).bind()
                        ocsAdminService.addMsisdnToBundleMapping(msisdn, bundle.id)
                    }
                    subscriptionRelationStore.create(subscriber, subscription, transaction).bind()
                    if (subscriber.country.equals("sg", ignoreCase = true)) {
                        logger.info(NOTIFY_OPS_MARKER, "Assigned +${subscription.msisdn} to the user: ${subscriber.email} in Singapore.")
                    }
                }.fix()
            }
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun getSubscriptions(subscriberId: String): Either<StoreError, Collection<Subscription>> =
            readTransaction { subscriberStore.getRelated(subscriberId, subscriptionRelation, transaction) }

    override fun getMsisdn(subscriptionId: String): Either<StoreError, String> {
        return readTransaction {
            subscriberStore.getRelated(subscriptionId, subscriptionRelation, transaction)
                    .flatMap {
                        if (it.isEmpty()) {
                            Either.left(NotFoundError(
                                    type = subscriptionEntity.name,
                                    id = "for ${subscriberEntity.name} = $subscriptionId"))
                        } else {
                            Either.right(it.first().msisdn)
                        }
                    }
        }
    }

    //
    // Products
    //

    override fun getProducts(subscriberId: String): Either<StoreError, Map<String, Product>> {
        return readTransaction {

            subscriberStore.exists(subscriberId, transaction)
                    .flatMap { _ ->
                        read<Either<StoreError, Map<String, Product>>>("""
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
            getProduct(subscriberId, sku, transaction)
        }
    }

    private fun getProduct(subscriberId: String, sku: String, transaction: Transaction): Either<StoreError, Product> {
        return subscriberStore.exists(subscriberId, transaction)
                .flatMap {
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

    //
    // Purchase Records
    //

    // TODO vihang: Move this logic to DSL + Rule Engine + Triggers, when they are ready
    // >> BEGIN
    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }
    private val ocs by lazy { getResource<OcsSubscriberService>() }
    private val analyticsReporter by lazy { getResource<AnalyticsService>() }

    private fun fetchOrCreatePaymentProfile(subscriberId: String): Either<PaymentError, ProfileInfo> =
            // Fetch/Create stripe payment profile for the subscriber.
            paymentProcessor.getPaymentProfile(subscriberId)
                    .fold(
                            { paymentProcessor.createPaymentProfile(subscriberId) },
                            { profileInfo -> Either.right(profileInfo) }
                    )

    override fun purchaseProduct(
            subscriberId: String,
            sku: String,
            sourceId: String?,
            saveCard: Boolean): Either<PaymentError, ProductInfo> = writeTransaction {
        IO {
            ForEither<PaymentError>() extensions {
                binding {
                    val product = getProduct(subscriberId, sku, transaction)
                            // If we can't find the product, return not-found
                            .mapLeft { org.ostelco.prime.paymentprocessor.core.NotFoundError("Product unavailable") }
                            .bind()
                    val profileInfo = fetchOrCreatePaymentProfile(subscriberId).bind()
                    val paymentCustomerId = profileInfo.id
                    if (sourceId != null) {
                        // First fetch all existing saved sources
                        val sourceDetails = paymentProcessor.getSavedSources(paymentCustomerId)
                                // If we can't find the product, return not-found
                                .mapLeft { org.ostelco.prime.paymentprocessor.core.BadGatewayError("Failed to fetch sources for user", it.description) }
                                .bind()
                        // If the sourceId is not found in existing list of saved sources,
                        // then save the source
                        if (!sourceDetails.any { sourceDetailsInfo -> sourceDetailsInfo.id == sourceId }) {
                            paymentProcessor.addSource(paymentCustomerId, sourceId)
                                    // For success case, saved source is removed after "capture charge" is saveCard == false.
                                    // Making sure same happens even for failure case by linking reversal action to transaction
                                    .finallyDo(transaction) { _ -> removePaymentSource(saveCard, paymentCustomerId, sourceId) }
                                    .bind()
                        }
                    }
                    //TODO: If later steps fail, then refund the authorized charge
                    val chargeId = paymentProcessor.authorizeCharge(paymentCustomerId, sourceId, product.price.amount, product.price.currency)
                            .mapLeft { apiError ->
                                logger.error("failed to authorize purchase for paymentCustomerId $paymentCustomerId, sourceId $sourceId, sku $sku")
                                apiError
                            }.linkReversalActionToTransaction(transaction) { chargeId ->
                                paymentProcessor.refundCharge(chargeId)
                                logger.error(NOTIFY_OPS_MARKER, "Failed to refund charge for paymentCustomerId $paymentCustomerId, chargeId $chargeId.\nFix this in Stripe dashboard.")
                            }.bind()
                    val purchaseRecord = PurchaseRecord(
                            id = chargeId,
                            product = product,
                            timestamp = Instant.now().toEpochMilli(),
                            msisdn = "")
                    createPurchaseRecordRelation(subscriberId, purchaseRecord, transaction)
                            .mapLeft { storeError ->
                                logger.error("failed to save purchase record, for paymentCustomerId $paymentCustomerId, chargeId $chargeId, payment will be unclaimed in Stripe")
                                BadGatewayError(storeError.message)
                            }.bind()
                    //TODO: While aborting transactions, send a record with "reverted" status
                    analyticsReporter.reportPurchaseInfo(
                            purchaseRecord = purchaseRecord,
                            subscriberId = subscriberId,
                            status = "success")
                    ocs.topup(subscriberId, sku)
                            .mapLeft { BadGatewayError("Failed to perform topup", it)  }
                            .bind()
                    // Even if the "capture charge operation" failed, we do not want to rollback.
                    // In that case, we just want to log it at error level.
                    // These transactions can then me manually changed before they are auto rollback'ed in 'X' days.
                    paymentProcessor.captureCharge(chargeId, paymentCustomerId)
                            .mapLeft {
                                // TODO payment: retry capture charge
                                logger.error(NOTIFY_OPS_MARKER, "Capture failed for paymentCustomerId $paymentCustomerId, chargeId $chargeId.\nFix this in Stripe Dashboard")
                            }
                    // Ignore failure to capture charge, by not calling bind()
                    ProductInfo(product.sku)
                }.fix()
            }
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
   }

    fun purchaseProductOld(
            subscriberId: String,
            sku: String,
            sourceId: String?,
            saveCard: Boolean): Either<PaymentError, ProductInfo> = writeTransaction {

        getProduct(subscriberId, sku, transaction)
                // If we can't find the product, return not-found
                .mapLeft { org.ostelco.prime.paymentprocessor.core.NotFoundError("Product unavailable") }
                .flatMap { product: Product ->
                    // Fetch/Create stripe payment profile for the subscriber.
                    paymentProcessor.getPaymentProfile(subscriberId)
                            .fold(
                                    { paymentProcessor.createPaymentProfile(subscriberId) },
                                    { profileInfo -> Either.right(profileInfo) }
                            )
                            .map { profileInfo -> Pair(product, profileInfo.id) }
                }
                .flatMap { (product, paymentCustomerId) ->
                    // Add payment source
                    if (sourceId != null) {
                        // First fetch all existing saved sources
                        paymentProcessor.getSavedSources(paymentCustomerId)
                                .fold(
                                        {
                                            Either.left(org.ostelco.prime.paymentprocessor.core.BadGatewayError("Failed to fetch sources for user", it.description))
                                        },
                                        {
                                            // If the sourceId is not found in existing list of saved sources,
                                            // then save the source
                                            if (!it.any { sourceDetailsInfo -> sourceDetailsInfo.id == sourceId }) {
                                                paymentProcessor.addSource(paymentCustomerId, sourceId)
                                                        // For success case, saved source is removed after "capture charge" is saveCard == false.
                                                        // Making sure same happens even for failure case by linking reversal action to transaction
                                                        .finallyDo(transaction) { _ -> removePaymentSource(saveCard, paymentCustomerId, sourceId) }
                                                        .map { sourceInfo -> Triple(product, paymentCustomerId, sourceInfo.id) }
                                            } else {
                                                Either.right(Triple(product, paymentCustomerId, sourceId))
                                            }
                                        }
                                )
                    } else {
                        Either.right(Triple(product, paymentCustomerId, null))
                    }
                }
                .flatMap { (product, paymentCustomerId, sourceId) ->
                    // Authorize stripe charge for this purchase
                    val price = product.price
                    //TODO: If later steps fail, then refund the authorized charge
                    paymentProcessor.authorizeCharge(paymentCustomerId, sourceId, price.amount, price.currency)
                            .mapLeft { apiError ->
                                logger.error("failed to authorize purchase for paymentCustomerId $paymentCustomerId, sourceId $sourceId, sku $sku")
                                apiError
                            }
                            .linkReversalActionToTransaction(transaction) { chargeId ->
                                paymentProcessor.refundCharge(chargeId)
                                logger.error(NOTIFY_OPS_MARKER, "Failed to refund charge for paymentCustomerId $paymentCustomerId, chargeId $chargeId.\nFix this in Stripe dashboard.")
                            }
                            .map { chargeId -> Tuple3(product, paymentCustomerId, chargeId) }
                }
                .flatMap { (product, paymentCustomerId, chargeId) ->
                    val purchaseRecord = PurchaseRecord(
                            id = chargeId,
                            product = product,
                            timestamp = Instant.now().toEpochMilli(),
                            msisdn = "")
                    // Create purchase record
                    createPurchaseRecordRelation(subscriberId, purchaseRecord, transaction)
                            .mapLeft { storeError ->
                                logger.error("failed to save purchase record, for paymentCustomerId $paymentCustomerId, chargeId $chargeId, payment will be unclaimed in Stripe")
                                BadGatewayError(storeError.message)
                            }
                            .flatMap {
                                //TODO: While aborting transactions, send a record with "reverted" status
                                analyticsReporter.reportPurchaseInfo(
                                        purchaseRecord = purchaseRecord,
                                        subscriberId = subscriberId,
                                        status = "success")

                                Either.right(Tuple3(product, paymentCustomerId, chargeId))
                            }
                }
                .flatMap { (product, paymentCustomerId, chargeId) ->
                    // Notify OCS
                    ocs.topup(subscriberId, sku)
                            .bimap({ BadGatewayError(description = "Failed to perform topup", externalErrorMessage = it) },
                                    { Tuple3(product, paymentCustomerId, chargeId) })
                }
                .map { (product, paymentCustomerId, chargeId) ->

                    // Even if the "capture charge operation" failed, we do not want to rollback.
                    // In that case, we just want to log it at error level.
                    // These transactions can then me manually changed before they are auto rollback'ed in 'X' days.
                    paymentProcessor.captureCharge(chargeId, paymentCustomerId)
                            .mapLeft {
                                // TODO payment: retry capture charge
                                logger.error(NOTIFY_OPS_MARKER, "Capture failed for paymentCustomerId $paymentCustomerId, chargeId $chargeId.\nFix this in Stripe Dashboard")
                            }

                    // Ignore failure to capture charge and always send Either.right()
                    ProductInfo(product.sku)
                }
                .ifFailedThenRollback(transaction)
    }
    // << END

    private fun removePaymentSource(saveCard: Boolean, paymentCustomerId: String, sourceId: String) {
        // In case we fail to remove saved source, we log it at error level.
        // These saved sources can then me manually removed.
        if (!saveCard) {
            paymentProcessor.removeSource(paymentCustomerId, sourceId)
                    .mapLeft { paymentError ->
                        logger.error("Failed to remove card, for customerId $paymentCustomerId, sourceId $sourceId")
                        paymentError
                    }
        }
    }

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
                purchaseRecordRelationStore.create(subscriber, purchase, product, transaction)
                        .map { purchase.id }
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

    //
    // For metrics
    //
    override fun getSubscriberCount(): Long = readTransaction {
        read("""
                MATCH (subscriber:${subscriberEntity.name})
                RETURN count(subscriber) AS count
                """.trimIndent(),
                transaction) { result ->
            result.single().get("count").asLong()
        }
    }

    override fun getReferredSubscriberCount(): Long = readTransaction {
        read("""
                MATCH (:${subscriberEntity.name})-[:${referredRelation.relation.name}]->(subscriber:${subscriberEntity.name})
                RETURN count(subscriber) AS count
                """.trimIndent(),
                transaction) { result ->
            result.single().get("count").asLong()
        }
    }

    override fun getPaidSubscriberCount(): Long = readTransaction {
        getPaidSubscriberCount(transaction)
    }

    private fun getPaidSubscriberCount(transaction: Transaction): Long {
        return read("""
                MATCH (subscriber:${subscriberEntity.name})-[:${purchaseRecordRelation.relation.name}]->(product:${productEntity.name})
                WHERE product.`price/amount` > 0
                RETURN count(subscriber) AS count
                """.trimIndent(),
                transaction) { result ->
            result.single().get("count").asLong()
        }
    }

    //
    // Stores
    //

    private val offerEntity = EntityType(Offer::class.java)
    private val offerStore = EntityStore(offerEntity)

    private val segmentEntity = EntityType(Segment::class.java)
    private val segmentStore = EntityStore(segmentEntity)

    private val offerToSegmentRelation = RelationType(OFFERED_TO_SEGMENT, offerEntity, segmentEntity, Void::class.java)
    private val offerToSegmentStore = RelationStore(offerToSegmentRelation)

    private val offerToProductRelation = RelationType(OFFER_HAS_PRODUCT, offerEntity, productEntity, Void::class.java)
    private val offerToProductStore = RelationStore(offerToProductRelation)

    private val subscriberToSegmentRelation = RelationType(BELONG_TO_SEGMENT, subscriberEntity, segmentEntity, Void::class.java)
    private val subscriberToSegmentStore = RelationStore(subscriberToSegmentRelation)

    private val productClassEntity = EntityType(ProductClass::class.java)
    private val productClassStore = EntityStore(productClassEntity)

    //
    // Product Class
    //
    override fun createProductClass(productClass: ProductClass): Either<StoreError, Unit> = writeTransaction {
        productClassStore.create(productClass, transaction)
                .ifFailedThenRollback(transaction)
    }

    //
    // Product
    //
    override fun createProduct(product: Product): Either<StoreError, Unit> = writeTransaction {
        createProduct(product, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun createProduct(product: Product, transaction: Transaction): Either<StoreError, Unit> =
            productStore.create(product, transaction)

    //
    // Segment
    //
    override fun createSegment(segment: Segment): Either<StoreError, Unit> = writeTransaction {
        createSegment(segment, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun createSegment(segment: Segment, transaction: Transaction): Either<StoreError, Unit> {
        return segmentStore.create(segment, transaction)
                .flatMap { subscriberToSegmentStore.create(segment.subscribers, segment.id, transaction) }
    }

    override fun updateSegment(segment: Segment): Either<StoreError, Unit> = writeTransaction {
        updateSegment(segment, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun updateSegment(segment: Segment, transaction: Transaction): Either<StoreError, Unit> {
        return subscriberToSegmentStore.removeAll(toId = segment.id, transaction = transaction)
                .flatMap { subscriberToSegmentStore.create(segment.subscribers, segment.id, transaction) }
    }

    //
    // Offer
    //
    override fun createOffer(offer: Offer): Either<StoreError, Unit> = writeTransaction {
        createOffer(offer, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun createOffer(offer: Offer, transaction: Transaction): Either<StoreError, Unit> {
        return offerStore
                .create(offer.id, transaction)
                .flatMap { offerToSegmentStore.create(offer.id, offer.segments, transaction) }
                .flatMap { offerToProductStore.create(offer.id, offer.products, transaction) }
    }

    //
    // Atomic Imports
    //

    /**
     * Create of Offer + Product + Segment
     */
    override fun atomicCreateOffer(
            offer: Offer,
            segments: Collection<Segment>,
            products: Collection<Product>): Either<StoreError, Unit> = writeTransaction {

        // validation
        val productIds = (offer.products + products.map { it.sku }).toSet()
        val segmentIds = (offer.segments + segments.map { it.id }).toSet()

        if (productIds.isEmpty()) {
            return@writeTransaction Either.left(ValidationError(
                    type = productEntity.name,
                    id = offer.id,
                    message = "Cannot create Offer without new/existing Product(s)"))
        }

        if (segmentIds.isEmpty()) {
            return@writeTransaction Either.left(ValidationError(
                    type = offerEntity.name,
                    id = offer.id,
                    message = "Cannot create Offer without new/existing Segment(s)"))
        }
        // end of validation

        var result = Either.right(Unit) as Either<StoreError, Unit>

        result = products.fold(
                initial = result,
                operation = { acc, product ->
                    acc.flatMap { createProduct(product, transaction) }
                })

        result = segments.fold(
                initial = result,
                operation = { acc, segment ->
                    acc.flatMap { createSegment(segment, transaction) }
                })

        val actualOffer = Offer(
                id = offer.id,
                products = productIds,
                segments = segmentIds)

        result
                .flatMap { createOffer(actualOffer, transaction) }
                .ifFailedThenRollback(transaction)
    }

    /**
     * Create Segments
     */
    override fun atomicCreateSegments(createSegments: Collection<Segment>): Either<StoreError, Unit> = writeTransaction {

        createSegments.fold(
                initial = Either.right(Unit) as Either<StoreError, Unit>,
                operation = { acc, segment ->
                    acc.flatMap { createSegment(segment, transaction) }
                })
                .ifFailedThenRollback(transaction)
    }

    /**
     * Update segments
     */
    override fun atomicUpdateSegments(updateSegments: Collection<Segment>): Either<StoreError, Unit> = writeTransaction {

        updateSegments.fold(
                initial = Either.right(Unit) as Either<StoreError, Unit>,
                operation = { acc, segment ->
                    acc.flatMap { updateSegment(segment, transaction) }
                })
                .ifFailedThenRollback(transaction)
    }

    override fun atomicAddToSegments(addToSegments: Collection<Segment>): Either<StoreError, Unit> {
        TODO()
    }

    override fun atomicRemoveFromSegments(removeFromSegments: Collection<Segment>): Either<StoreError, Unit> {
        TODO()
    }

    override fun atomicChangeSegments(changeSegments: Collection<ChangeSegment>): Either<StoreError, Unit> {
        TODO()
    }

    // override fun getOffers(): Collection<Offer> = offerStore.getAll().values.map { Offer().apply { id = it.id } }

    // override fun getSegments(): Collection<Segment> = segmentStore.getAll().values.map { Segment().apply { id = it.id } }

    // override fun getOffer(id: String): Offer? = offerStore.get(id)?.let { Offer().apply { this.id = it.id } }

    // override fun getSegment(id: String): Segment? = segmentStore.get(id)?.let { Segment().apply { this.id = it.id } }

    // override fun getProductClass(id: String): ProductClass? = productClassStore.get(id)
}