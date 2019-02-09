package org.ostelco.prime.storage.graph

import arrow.core.Either
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.effects.IO
import arrow.instances.either.monad.monad
import org.neo4j.driver.v1.Transaction
import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.ChangeSegment
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.CustomerState
import org.ostelco.prime.model.CustomerStatus
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Plan
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.RefundRecord
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.ScanStatus
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.storage.AlreadyExistsError
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.NotCreatedError
import org.ostelco.prime.storage.NotDeletedError
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.NotUpdatedError
import org.ostelco.prime.storage.ScanInformationStore
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.ValidationError
import org.ostelco.prime.storage.graph.Graph.read
import org.ostelco.prime.storage.graph.Graph.write
import org.ostelco.prime.storage.graph.Relation.BELONG_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.HAS_BUNDLE
import org.ostelco.prime.storage.graph.Relation.HAS_SUBSCRIPTION
import org.ostelco.prime.storage.graph.Relation.IDENTIFIES
import org.ostelco.prime.storage.graph.Relation.LINKED_TO_BUNDLE
import org.ostelco.prime.storage.graph.Relation.OFFERED_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.OFFER_HAS_PRODUCT
import org.ostelco.prime.storage.graph.Relation.PURCHASED
import org.ostelco.prime.storage.graph.Relation.REFERRED
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import javax.ws.rs.core.MultivaluedMap
import org.ostelco.prime.model.Identity as ModelIdentity

enum class Relation {
    IDENTIFIES,            // (Identity) -[IDENTIFIES]-> (Customer)
    HAS_SUBSCRIPTION,      // (Customer) -[HAS_SUBSCRIPTION]-> (Subscription)
    HAS_BUNDLE,            // (Customer) -[HAS_BUNDLE]-> (Bundle)
    SUBSCRIBES_TO_PLAN,    // (Customer) -[SUBSCRIBES_TO_PLAN]-> (Plan)
    HAS_PRODUCT,           // (Plan) -[HAS_PRODUCT]-> (Product)
    LINKED_TO_BUNDLE,      // (Subscription) -[LINKED_TO_BUNDLE]-> (Bundle)
    PURCHASED,             // (Customer) -[PURCHASED]-> (Product)
    REFERRED,              // (Customer) -[REFERRED]-> (Customer)
    OFFERED_TO_SEGMENT,    // (Offer) -[OFFERED_TO_SEGMENT]-> (Segment)
    OFFER_HAS_PRODUCT,     // (Offer) -[OFFER_HAS_PRODUCT]-> (Product)
    BELONG_TO_SEGMENT,     // (Customer) -[BELONG_TO_SEGMENT]-> (Segment)
    CUSTOMER_STATE,        // (Customer) -[CUSTOMER_STATE]-> (CustomerState)
    EKYC_SCAN,             // (Customer) -[EKYC_SCAN]-> (ScanInformation)
}


class Neo4jStore : GraphStore by Neo4jStoreSingleton

object Neo4jStoreSingleton : GraphStore {

    private val logger by getLogger()
    private val scanInformationDatastore by lazy { getResource<ScanInformationStore>() }

    //
    // Entity
    //

    private val identityEntity = EntityType(Identity::class.java)
    private val identityStore = EntityStore(identityEntity)

    private val customerEntity = EntityType(Customer::class.java)
    private val customerStore = EntityStore(customerEntity)

    private val productEntity = EntityType(Product::class.java)
    private val productStore = EntityStore(productEntity)

    private val subscriptionEntity = EntityType(Subscription::class.java)
    private val subscriptionStore = EntityStore(subscriptionEntity)

    private val bundleEntity = EntityType(Bundle::class.java)
    private val bundleStore = EntityStore(bundleEntity)

    private val planEntity = EntityType(Plan::class.java)
    private val plansStore = EntityStore(planEntity)

    private val customerStateEntity = EntityType(CustomerState::class.java)
    private val customerStateStore = EntityStore(customerStateEntity)

    private val scanInformationEntity = EntityType(ScanInformation::class.java)
    private val scanInformationStore = EntityStore(scanInformationEntity)

    //
    // Relation
    //

    private val identifiesRelation = RelationType(
            relation = IDENTIFIES,
            from = identityEntity,
            to = customerEntity,
            dataClass = Identifies::class.java)
    private val identifiesRelationStore = RelationStore(identifiesRelation)

    private val subscriptionRelation = RelationType(
            relation = HAS_SUBSCRIPTION,
            from = customerEntity,
            to = subscriptionEntity,
            dataClass = Void::class.java)
    private val subscriptionRelationStore = RelationStore(subscriptionRelation)

    private val subscriberToBundleRelation = RelationType(
            relation = HAS_BUNDLE,
            from = customerEntity,
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
            from = customerEntity,
            to = productEntity,
            dataClass = PurchaseRecord::class.java)
    private val purchaseRecordRelationStore = RelationStore(purchaseRecordRelation)
    private val changablePurchaseRelationStore = ChangeableRelationStore(purchaseRecordRelation)

    private val referredRelation = RelationType(
            relation = REFERRED,
            from = customerEntity,
            to = customerEntity,
            dataClass = Void::class.java)
    private val referredRelationStore = RelationStore(referredRelation)

    private val subscribesToPlanRelation = RelationType(
            relation = Relation.SUBSCRIBES_TO_PLAN,
            from = customerEntity,
            to = planEntity,
            dataClass = Void::class.java)
    private val subscribesToPlanRelationStore = UniqueRelationStore(subscribesToPlanRelation)

    private val customerStateRelation = RelationType(
            relation = Relation.CUSTOMER_STATE,
            from = customerEntity,
            to = customerStateEntity,
            dataClass = Void::class.java)
    private val customerStateRelationStore = UniqueRelationStore(customerStateRelation)

    private val scanInformationRelation = RelationType(
            relation = Relation.EKYC_SCAN,
            from = customerEntity,
            to = scanInformationEntity,
            dataClass = Void::class.java)
    private val scanInformationRelationStore = UniqueRelationStore(scanInformationRelation)

    private val planProductRelation = RelationType(
            relation = Relation.HAS_PRODUCT,
            from = planEntity,
            to = productEntity,
            dataClass = Void::class.java)
    private val planProductRelationStore = UniqueRelationStore(planProductRelation)


    // -------------
    // Client Store
    // -------------

    //
    // Identity
    //

    override fun getCustomerId(identity: org.ostelco.prime.model.Identity): Either<StoreError, String> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
    }

    private fun getCustomerId(identity: org.ostelco.prime.model.Identity, transaction: Transaction): Either<StoreError, String> {
        return identityStore.getRelated(id = identity.id, relationType = identifiesRelation, transaction = transaction)
                .flatMap {
                    if (it.isEmpty()) {
                        NotFoundError(type = identity.type, id = identity.id).left()
                    } else {
                        it.single().id.right()
                    }
                }
    }

    //
    // Balance (Customer - Bundle)
    //

    override fun getBundles(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<Bundle>> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId -> customerStore.getRelated(customerId, subscriberToBundleRelation, transaction) }
    }

    override fun updateBundle(bundle: Bundle): Either<StoreError, Unit> = writeTransaction {
        bundleStore.update(bundle, transaction)
                .ifFailedThenRollback(transaction)
    }

    //
    // Customer
    //

    override fun getCustomer(identity: org.ostelco.prime.model.Identity): Either<StoreError, Customer> {
        return readTransaction {
            identityStore.getRelated(id = identity.id, relationType = identifiesRelation, transaction = transaction)
        }.map { it.single() }
    }

    private fun validateCreateCustomerParams(customer: Customer, referredBy: String?): Either<StoreError, Unit> =
            if (customer.referralId == referredBy) {
                Either.left(ValidationError(type = customerEntity.name, id = customer.id, message = "Referred by self"))
            } else {
                Either.right(Unit)
            }

    // TODO vihang: Move this logic to DSL + Rule Engine + Triggers, when they are ready
    // >> BEGIN
    override fun addCustomer(identity: ModelIdentity, customer: Customer, referredBy: String?): Either<StoreError, Unit> = writeTransaction {
        // IO is used to represent operations that can be executed lazily and are capable of failing.
        // Here it runs IO synchronously and returning its result blocking the current thread.
        // https://arrow-kt.io/docs/patterns/monad_comprehensions/#comprehensions-over-coroutines
        // https://arrow-kt.io/docs/effects/io/#unsaferunsync
        IO {
            Either.monad<StoreError>().binding {
                validateCreateCustomerParams(customer, referredBy).bind()
                val bundleId = UUID.randomUUID().toString()
                identityStore.create(Identity(id = identity.id, type = identity.type), transaction).bind()
                customerStore.create(customer, transaction).bind()
                identifiesRelationStore.create(fromId = identity.id, relation = Identifies(provider = identity.provider), toId = customer.id, transaction = transaction).bind()
                createCustomerState(customer.id, CustomerStatus.REGISTERED, transaction).bind()
                subscriberToSegmentStore.create(customer.id, getSegmentNameFromCountryCode(customer.country), transaction)
                        .mapLeft { storeError ->
                            if (storeError is NotCreatedError && storeError.type == subscriberToSegmentRelation.relation.name) {
                                ValidationError(type = customerEntity.name, id = customer.id, message = "Unsupported country: ${customer.country}")
                            } else {
                                storeError
                            }
                        }.bind()
                // Give 100 MB as free initial balance
                var productId = "100MB_FREE_ON_JOINING"
                var balance: Long = 100_000_000
                if (referredBy != null) {
                    // Give 1 GB if customer is referred
                    productId = "1GB_FREE_ON_REFERRED"
                    balance = 1_000_000_000
                    referredRelationStore.create(referredBy, customer.id, transaction).bind()
                }
                bundleStore.create(Bundle(bundleId, balance), transaction).bind()
                val product = productStore.get(productId, transaction).bind()
                createPurchaseRecordRelation(
                        customer.id,
                        PurchaseRecord(id = UUID.randomUUID().toString(), product = product, timestamp = Instant.now().toEpochMilli(), msisdn = ""),
                        transaction)
                subscriberToBundleStore.create(customer.id, bundleId, transaction).bind()
                // TODO Remove hardcoded country code.
                // https://docs.oracle.com/javase/9/docs/api/java/util/Locale.IsoCountryCode.html
                if (customer.country.equals("sg", ignoreCase = true)) {
                    logger.info(NOTIFY_OPS_MARKER, "Created a new user with email: ${customer.email} for Singapore.\nProvision a SIM card for this user.")
                }
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }
    // << END

    override fun updateCustomer(identity: org.ostelco.prime.model.Identity, customer: Customer): Either<StoreError, Unit> = writeTransaction {
        customerStore.update(customer, transaction)
                .ifFailedThenRollback(transaction)
    }

    override fun removeCustomer(identity: org.ostelco.prime.model.Identity): Either<StoreError, Unit> = writeTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    identityStore.delete(id = identity.id, transaction = transaction)
                    customerStore.exists(customerId, transaction)
                            .flatMap { _ ->
                                customerStore.getRelated(customerId, subscriberToBundleRelation, transaction)
                                        .map { it.forEach { bundle -> bundleStore.delete(bundle.id, transaction) } }
                                customerStore.getRelated(customerId, subscriptionRelation, transaction)
                                        .map { it.forEach { subscription -> subscriptionStore.delete(subscription.id, transaction) } }
                                customerStore.getRelated(customerId, customerStateRelation, transaction)
                                        .map { it.forEach { bundle -> customerStateStore.delete(bundle.id, transaction) } }
                            }
                            .flatMap { customerStore.delete(customerId, transaction) }
                }
                .ifFailedThenRollback(transaction)
    }

    //
    // Subscription
    //
    private fun validateBundleList(bundles: List<Bundle>, customerId: String): Either<StoreError, Unit> =
            if (bundles.isEmpty()) {
                Either.left(NotFoundError(type = subscriberToBundleRelation.relation.name, id = "$customerId -> *"))
            } else {
                Either.right(Unit)
            }

    override fun addSubscription(identity: org.ostelco.prime.model.Identity, msisdn: String): Either<StoreError, Unit> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = Neo4jStoreSingleton.getCustomerId(identity = identity, transaction = transaction).bind()
                val bundles = customerStore.getRelated(customerId, subscriberToBundleRelation, transaction).bind()
                validateBundleList(bundles, customerId).bind()
                subscriptionStore.create(Subscription(msisdn), transaction).bind()
                val subscription = subscriptionStore.get(msisdn, transaction).bind()
                val customer = customerStore.get(customerId, transaction).bind()
                bundles.forEach { bundle ->
                    subscriptionToBundleStore.create(subscription, bundle, transaction).bind()
                }
                subscriptionRelationStore.create(customer, subscription, transaction).bind()
                // TODO Remove hardcoded country code.
                // https://docs.oracle.com/javase/9/docs/api/java/util/Locale.IsoCountryCode.html
                if (customer.country.equals("sg", ignoreCase = true)) {
                    logger.info(NOTIFY_OPS_MARKER, "Assigned +${subscription.msisdn} to the user: ${customer.email} in Singapore.")
                }
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun getSubscriptions(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<Subscription>> =
            readTransaction {
                getCustomerId(identity = identity, transaction = transaction)
                        .flatMap { customerId -> customerStore.getRelated(customerId, subscriptionRelation, transaction) }

            }

    override fun getMsisdn(identity: org.ostelco.prime.model.Identity): Either<StoreError, String> {
        return readTransaction {
            getCustomerId(identity = identity, transaction = transaction)
                    .flatMap { customerId ->
                        customerStore.getRelated(customerId, subscriptionRelation, transaction)
                                .flatMap {
                                    if (it.isEmpty()) {
                                        Either.left(NotFoundError(
                                                type = subscriptionEntity.name,
                                                id = "for ${customerEntity.name} = $customerId"))
                                    } else {
                                        Either.right(it.first().msisdn)
                                    }
                                }
                    }
        }
    }

    //
    // Products
    //

    override fun getProducts(identity: org.ostelco.prime.model.Identity): Either<StoreError, Map<String, Product>> {
        return readTransaction {

            getCustomerId(identity = identity, transaction = transaction)
                    .flatMap { customerId ->
                        read<Either<StoreError, Map<String, Product>>>("""
                            MATCH (:${customerEntity.name} {id: '$customerId'})
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

    override fun getProduct(identity: org.ostelco.prime.model.Identity, sku: String): Either<StoreError, Product> {
        return readTransaction {
            getProduct(identity, sku, transaction)
        }
    }

    private fun getProduct(identity: org.ostelco.prime.model.Identity, sku: String, transaction: Transaction): Either<StoreError, Product> {
        return getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    read("""
                            MATCH (:${customerEntity.name} {id: '$customerId'})
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
    // Consumption
    //

    /**
     * This method takes [msisdn], [usedBytes] and [requestedBytes] as parameters.
     * The [usedBytes] will then be deducted from existing `balance` and `reservedBytes` from a [Bundle] associated with
     * this [msisdn].
     * Thus, `reservedBytes` is set back to `zero` and `surplus/deficit` bytes are adjusted with main `balance`.
     * Then, bytes equal to or less than [requestedBytes] are deducted from `balance` such that `balance` is `non-negative`.
     * Those bytes are then set as `reservedBytes` and returned as response.
     *
     * The above logic is vanilla case and may be enriched based on multiple factors such as mcc_mnc, rating-group etc.
     *
     * @param msisdn which is consuming
     * @param usedBytes Bytes already consumed.
     * @param requestedBytes Bytes requested for consumption.
     *
     */
    override fun consume(msisdn: String, usedBytes: Long, requestedBytes: Long): Either<StoreError, Pair<Long, Long>> {

        // Note: _LOCK_ dummy property is set in the relation 'r' and node 'bundle' so that they get locked.
        // Ref: https://neo4j.com/docs/java-reference/current/transactions/#transactions-isolation

        return writeTransaction {
            IO {
                Either.monad<StoreError>().binding {
                    val (reservedBytes, balance) = read("""
                            MATCH (:${subscriptionEntity.name} {id: '$msisdn'})-[r:${subscriptionToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})
                            SET r._LOCK_ = true, bundle._LOCK_ = true
                            RETURN r.reservedBytes AS reservedBytes, bundle.balance AS balance
                            """.trimIndent(),
                            transaction) { statementResult ->
                        if (statementResult.hasNext()) {
                            val record = statementResult.single()
                            val reservedBytes = record.get("reservedBytes").asString("0").toLong()
                            val balance = record.get("balance").asString("0").toLong()
                            Pair(reservedBytes, balance).right()
                        } else {
                            NotFoundError("Bundle for ${subscriptionEntity.name}", msisdn).left()
                        }
                    }.bind()

                    // First adjust reserved and used bytes
                    // Balance cannot be negative
                    var newBalance = Math.max(balance + reservedBytes - usedBytes, 0)

                    // Then check how much of requested bytes can be granted
                    val granted = Math.min(newBalance, requestedBytes)
                    newBalance -= granted

                    write("""
                            MATCH (:${subscriptionEntity.name} {id: '$msisdn'})-[r:${subscriptionToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})
                            SET r.reservedBytes = '$granted', bundle.balance = '$newBalance'
                            REMOVE r._LOCK_, bundle._LOCK_
                            RETURN r.reservedBytes AS granted, bundle.balance AS balance
                            """.trimIndent(),
                            transaction) { statementResult ->
                        if (statementResult.hasNext()) {
                            val record = statementResult.single()
                            val savedBalance = record.get("balance").asString("0").toLong()
                            if (savedBalance != newBalance) {
                                logger.error(NOTIFY_OPS_MARKER, "Wrong balance set for msisdn: $msisdn to instead of $newBalance")
                            }
                            val savedGranted = record.get("granted").asString("0").toLong()
                            Pair(savedGranted, savedBalance).right()
                        } else {
                            NotUpdatedError("Balance for ${subscriptionEntity.name}", msisdn).left()
                        }
                    }.bind()
                }.fix()
            }.unsafeRunSync()//.ifFailedThenRollback(transaction)
        }
    }

    //
    // Purchase
    //

    // TODO vihang: Move this logic to DSL + Rule Engine + Triggers, when they are ready
    // >> BEGIN
    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }
    private val analyticsReporter by lazy { getResource<AnalyticsService>() }

    private fun fetchOrCreatePaymentProfile(customerId: String): Either<PaymentError, ProfileInfo> =
    // Fetch/Create stripe payment profile for the customer.
            paymentProcessor.getPaymentProfile(customerId)
                    .fold(
                            { paymentProcessor.createPaymentProfile(customerId) },
                            { profileInfo -> Either.right(profileInfo) }
                    )

    override fun purchaseProduct(
            identity: org.ostelco.prime.model.Identity,
            sku: String,
            sourceId: String?,
            saveCard: Boolean): Either<PaymentError, ProductInfo> {

        return getProduct(identity, sku).fold(
                {
                    Either.left(org.ostelco.prime.paymentprocessor.core.NotFoundError("Product $sku is unavailable",
                            error = it))
                },
                {
                    /* TODO: Complete support for 'product-class' and store plans as a
                             'product' of product-class: 'plan'. */
                    return if (it.properties.containsKey("productType")
                            && it.properties["productType"].equals("plan", true)) {

                        purchasePlan(
                                identity = identity,
                                product = it,
                                sourceId = sourceId,
                                saveCard = saveCard)
                    } else {

                        purchaseProduct(
                                identity = identity,
                                product = it,
                                sourceId = sourceId,
                                saveCard = saveCard)
                    }
                }
        )
    }

    private fun purchasePlan(identity: org.ostelco.prime.model.Identity,
                             product: Product,
                             sourceId: String?,
                             saveCard: Boolean): Either<PaymentError, ProductInfo> = writeTransaction {
        IO {
            Either.monad<PaymentError>().binding {
                val customerId = getCustomerId(identity = identity, transaction = transaction)
                        .mapLeft {
                            org.ostelco.prime.paymentprocessor.core.NotFoundError(
                                    "Failed to get customerId for customer with identity - $identity",
                                    error = it)
                        }
                        .bind()
                val profileInfo = fetchOrCreatePaymentProfile(customerId)
                        .bind()
                val paymentCustomerId = profileInfo.id

                if (sourceId != null) {
                    val sourceDetails = paymentProcessor.getSavedSources(paymentCustomerId)
                            .mapLeft {
                                BadGatewayError("Failed to fetch sources for customer: $customerId",
                                        error = it)
                            }.bind()
                    if (!sourceDetails.any { sourceDetailsInfo -> sourceDetailsInfo.id == sourceId }) {
                        paymentProcessor.addSource(paymentCustomerId, sourceId)
                                .finallyDo(transaction) {
                                    removePaymentSource(saveCard, paymentCustomerId, it.id)
                                }.bind().id
                    }
                }
                subscribeToPlan(identity, product.id)
                        .mapLeft {
                            org.ostelco.prime.paymentprocessor.core.BadGatewayError("Failed to subscribe $customerId to plan ${product.id}",
                                    error = it)
                        }
                        .flatMap {
                            Either.right(ProductInfo(product.id))
                        }.bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    private fun purchaseProduct(identity: org.ostelco.prime.model.Identity,
                                product: Product,
                                sourceId: String?,
                                saveCard: Boolean): Either<PaymentError, ProductInfo> = writeTransaction {
        IO {
            Either.monad<PaymentError>().binding {

                val customerId = getCustomerId(identity = identity, transaction = transaction)
                        .mapLeft {
                            org.ostelco.prime.paymentprocessor.core.NotFoundError(
                                    "Failed to get customerId for customer with identity - $identity",
                                    error = it)
                        }
                        .bind()
                val profileInfo = fetchOrCreatePaymentProfile(customerId).bind()
                val paymentCustomerId = profileInfo.id
                var addedSourceId: String? = null
                if (sourceId != null) {
                    // First fetch all existing saved sources
                    val sourceDetails = paymentProcessor.getSavedSources(paymentCustomerId)
                            .mapLeft {
                                BadGatewayError("Failed to fetch sources for user",
                                        error = it)
                            }.bind()
                    addedSourceId = sourceId
                    // If the sourceId is not found in existing list of saved sources,
                    // then save the source
                    if (!sourceDetails.any { sourceDetailsInfo -> sourceDetailsInfo.id == sourceId }) {
                        addedSourceId = paymentProcessor.addSource(paymentCustomerId, sourceId)
                                // For success case, saved source is removed after "capture charge" is saveCard == false.
                                // Making sure same happens even for failure case by linking reversal action to transaction
                                .finallyDo(transaction) { removePaymentSource(saveCard, paymentCustomerId, it.id) }
                                .bind().id
                    }
                }
                //TODO: If later steps fail, then refund the authorized charge
                val chargeId = paymentProcessor.authorizeCharge(paymentCustomerId, addedSourceId, product.price.amount, product.price.currency)
                        .mapLeft {
                            logger.error("Failed to authorize purchase for paymentCustomerId $paymentCustomerId, sourceId $addedSourceId, sku ${product.sku}")
                            it
                        }.linkReversalActionToTransaction(transaction) { chargeId ->
                            paymentProcessor.refundCharge(chargeId, product.price.amount, product.price.currency)
                            logger.error(NOTIFY_OPS_MARKER,
                                    "Failed to refund charge for paymentCustomerId $paymentCustomerId, chargeId $chargeId.\nFix this in Stripe dashboard.")
                        }.bind()

                val purchaseRecord = PurchaseRecord(
                        id = chargeId,
                        product = product,
                        timestamp = Instant.now().toEpochMilli(),
                        msisdn = "")
                createPurchaseRecordRelation(customerId, purchaseRecord, transaction)
                        .mapLeft {
                            logger.error("Failed to save purchase record, for paymentCustomerId $paymentCustomerId, chargeId $chargeId, payment will be unclaimed in Stripe")
                            BadGatewayError("Failed to save purchase record",
                                    error = it)
                        }.bind()

                //TODO: While aborting transactions, send a record with "reverted" status
                analyticsReporter.reportPurchaseInfo(purchaseRecord, customerId, "success")

                // Topup
                val bytes = product.properties["noOfBytes"]?.replace("_", "")?.toLongOrNull() ?: 0L

                if (bytes == 0L) {
                    logger.error("Product with 0 bytes: sku = ${product.sku}")
                }

                write("""
                    MATCH (cr:${customerEntity.name} { id:'$customerId' })-[:${subscriberToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})
                    SET bundle.balance = toString(toInteger(bundle.balance) + $bytes)
                    """.trimIndent(), transaction) {
                    Either.cond(
                            test = it.summary().counters().containsUpdates(),
                            ifTrue = {},
                            ifFalse = {
                                logger.error("Failed to update balance during purchase for customer: $customerId")
                                BadGatewayError(
                                        description = "Failed to update balance during purchase for customer: $customerId",
                                        message = "Failed to perform topup")
                            })
                }.bind()

                // Even if the "capture charge operation" failed, we do not want to rollback.
                // In that case, we just want to log it at error level.
                // These transactions can then me manually changed before they are auto rollback'ed in 'X' days.
                paymentProcessor.captureCharge(chargeId, paymentCustomerId, product.price.amount, product.price.currency)
                        .mapLeft {
                            // TODO payment: retry capture charge
                            logger.error(NOTIFY_OPS_MARKER, "Capture failed for paymentCustomerId $paymentCustomerId, chargeId $chargeId.\nFix this in Stripe Dashboard")
                        }
                // Ignore failure to capture charge, by not calling bind()
                ProductInfo(product.sku)
            }.fix()
        }.unsafeRunSync().ifFailedThenRollback(transaction)
    }
    // << END

    private fun removePaymentSource(saveCard: Boolean, paymentCustomerId: String, sourceId: String) {
        // In case we fail to remove saved source, we log it at error level.
        // These saved sources can then me manually removed.
        if (!saveCard) {
            paymentProcessor.removeSource(paymentCustomerId, sourceId)
                    .mapLeft {
                        logger.error("Failed to remove card, for customerId $paymentCustomerId, sourceId $sourceId")
                        it
                    }
        }
    }

    //
    // Purchase Records
    //

    override fun getPurchaseRecords(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<PurchaseRecord>> {
        return readTransaction {
            getCustomerId(identity = identity, transaction = transaction)
                    .flatMap { customerId -> customerStore.getRelations(customerId, purchaseRecordRelation, transaction) }
        }
    }

    override fun addPurchaseRecord(customerId: String, purchase: PurchaseRecord): Either<StoreError, String> {
        return writeTransaction {
            createPurchaseRecordRelation(customerId, purchase, transaction)
                    .ifFailedThenRollback(transaction)
        }
    }

    private fun createPurchaseRecordRelation(
            customerId: String,
            purchase: PurchaseRecord,
            transaction: Transaction): Either<StoreError, String> {

        return customerStore.get(customerId, transaction).flatMap { customer ->
            productStore.get(purchase.product.sku, transaction).flatMap { product ->
                purchaseRecordRelationStore.create(customer, purchase, product, transaction)
                        .map { purchase.id }
            }
        }
    }

    //
    // Referrals
    //

    override fun getReferrals(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<String>> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    customerStore.getRelated(customerId, referredRelation, transaction)
                            .map { list -> list.map { it.name } }
                }
    }

    override fun getReferredBy(identity: org.ostelco.prime.model.Identity): Either<StoreError, String?> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    customerStore.getRelatedFrom(customerId, referredRelation, transaction)
                            .map { it.singleOrNull()?.name }
                }
    }

    //
    // Customer State
    //

    private fun createCustomerState(customerId: String, status: CustomerStatus, transaction: PrimeTransaction): Either<StoreError, CustomerState> {
        val state = CustomerState(status, Date().time, null, customerId)
        return customerStateStore.create(state, transaction).flatMap {
            customerStateRelationStore.create(customerId, customerId, transaction)
                    .map { state }
        }
    }

    private fun getOrCreateCustomerState(customerId: String, status: CustomerStatus, transaction: PrimeTransaction): Either<StoreError, CustomerState> {
        return customerStateStore.get(customerId, transaction)
                .fold(
                        { createCustomerState(customerId, status, transaction) },
                        { subscriberState -> Either.right(subscriberState) }
                )
    }

    private fun updateCustomerState(customerId: String, status: CustomerStatus, scanId: String?, transaction: PrimeTransaction): Either<StoreError, CustomerState> {
        return customerStateStore.get(customerId, transaction)
                .fold(
                        { createCustomerState(customerId, status, transaction) },
                        {
                            val state = CustomerState(status, Date().time, scanId, customerId)
                            customerStateStore.update(state, transaction)
                                    .map { state }
                        }
                )
    }

    override fun getCustomerState(identity: org.ostelco.prime.model.Identity): Either<StoreError, CustomerState> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId -> customerStateStore.get(customerId, transaction) }
    }

    //
    // eKYC
    //

    override fun newEKYCScanId(identity: org.ostelco.prime.model.Identity, countryCode: String): Either<StoreError, ScanInformation> = writeTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    customerStore.get(customerId, transaction).flatMap { customer ->
                        // Generate new id for the scan
                        val scanId = UUID.randomUUID().toString()
                        val newScan = ScanInformation(scanId = scanId, countryCode = countryCode, status = ScanStatus.PENDING, scanResult = null)
                        scanInformationStore.create(newScan, transaction).flatMap {
                            scanInformationRelationStore.create(customer.id, newScan.id, transaction).flatMap { Either.right(newScan) }
                        }
                    }
                }
    }

    private fun getCustomerIdUsingScanId(scanId: String, transaction: Transaction): Either<StoreError, Customer> {
        return read("""
                MATCH (customer:${customerEntity.name})-[:${scanInformationRelation.relation.name}]->(scanInformation:${scanInformationEntity.name} {scanId: '${scanId}'})
                RETURN customer
                """.trimIndent(),
                transaction) {
            if (it.hasNext())
                Either.right(customerEntity.createEntity(it.single().get("customer").asMap()))
            else
                Either.left(NotFoundError(type = scanInformationEntity.name, id = scanId))
        }
    }

    override fun getCountryCodeForScan(scanId: String): Either<StoreError, String> = readTransaction {
        read("""
                MATCH (scanInformation:${scanInformationEntity.name} {scanId: '${scanId}'})
                RETURN scanInformation
                """.trimIndent(),
                transaction) {
            if (it.hasNext())
                Either.right(scanInformationEntity.createEntity(it.single().get("scanInformation").asMap()).countryCode)
            else
                Either.left(NotFoundError(type = scanInformationEntity.name, id = scanId))
        }
    }

    override fun getScanInformation(identity: org.ostelco.prime.model.Identity, scanId: String): Either<StoreError, ScanInformation> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    scanInformationStore.get(scanId, transaction).flatMap { scanInformation ->
                        getCustomerIdUsingScanId(scanInformation.scanId, transaction).flatMap { customer ->
                            // Check if the scan belongs to this customer
                            if (customer.id == customerId) {
                                scanInformation.right()
                            } else {
                                ValidationError(type = scanInformationEntity.name, id = scanId, message = "Not allowed").left()
                            }
                        }
                    }
                }
    }

    override fun getAllScanInformation(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<ScanInformation>> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    customerStore.getRelated(customerId, scanInformationRelation, transaction)
                }
    }

    override fun updateScanInformation(scanInformation: ScanInformation, vendorData: MultivaluedMap<String, String>): Either<StoreError, Unit> = writeTransaction {
        logger.info("updateScanInformation : ${scanInformation.scanId} status: ${scanInformation.status}")
        getCustomerIdUsingScanId(scanInformation.scanId, transaction).flatMap { customer ->
            scanInformationStore.update(scanInformation, transaction).flatMap {
                logger.info("updating scan Information for : ${customer.email} id: ${scanInformation.scanId} status: ${scanInformation.status}")
                getOrCreateCustomerState(customer.id, CustomerStatus.REGISTERED, transaction).flatMap { subcriberState ->
                    if (scanInformation.status == ScanStatus.APPROVED) {
                        // Update the scan information store with the new scan data
                        logger.info("Inserting scan Information to cloud storage : id: ${scanInformation.scanId} countryCode: ${scanInformation.countryCode}")
                        scanInformationDatastore.upsertVendorScanInformation(customer.id, scanInformation.countryCode, vendorData).flatMap {
                            // Update the state if the scan was successful and we are waiting for eKYC results
                            updateCustomerState(customer.id, CustomerStatus.EKYC_APPROVED, scanInformation.scanId, transaction).map { Unit }
                        }
                    } else if (scanInformation.status == ScanStatus.REJECTED && subcriberState.status == CustomerStatus.REGISTERED) {
                        // Update the state if the scan was a failure and we are waiting for eKYC results
                        logger.info("Setting Rejected Status : id: ${scanInformation.scanId} countryCode: ${scanInformation.countryCode}")
                        updateCustomerState(customer.id, CustomerStatus.EKYC_REJECTED, null, transaction).map { Unit }
                    } else {
                        // Remain in the previous state
                        logger.info("Keeping old status : id: ${scanInformation.scanId} status: ${subcriberState.status}")
                        Either.right(Unit)
                    }
                }
            }
        }
    }

    // ------------
    // Admin Store
    // ------------

    //
    // Balance (Customer - Subscription - Bundle)
    //

    override fun getMsisdnToBundleMap(): Map<Subscription, Bundle> = readTransaction {
        read("""
                MATCH (subscription:${subscriptionEntity.name})-[:${subscriptionToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})<-[:${subscriberToBundleRelation.relation.name}]-(:${customerEntity.name})
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
                MATCH (:${customerEntity.name})-[:${subscriberToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})<-[:${subscriptionToBundleRelation.relation.name}]-(:${subscriptionEntity.name})
                RETURN bundle
                """.trimIndent(),
                transaction) { result ->
            result.list {
                ObjectHandler.getObject(it["bundle"].asMap(), Bundle::class.java)
            }.toSet()
        }
    }

    override fun getCustomerToBundleIdMap(): Map<Customer, Bundle> = readTransaction {
        read("""
                MATCH (customer:${customerEntity.name})-[:${subscriberToBundleRelation.relation.name}]->(bundle:${bundleEntity.name})
                RETURN customer, bundle
                """.trimIndent(),
                transaction) { result ->
            result.list {
                Pair(ObjectHandler.getObject(it["customer"].asMap(), Customer::class.java),
                        ObjectHandler.getObject(it["bundle"].asMap(), Bundle::class.java))
            }.toMap()
        }
    }

    override fun getCustomerToMsisdnMap(): Map<Customer, Subscription> = readTransaction {
        read("""
                MATCH (customer:${customerEntity.name})-[:${subscriptionRelation.relation.name}]->(subscription:${subscriptionEntity.name})
                RETURN customer, subscription
                """.trimIndent(),
                transaction) { result ->
            result.list {
                Pair(ObjectHandler.getObject(it["customer"].asMap(), Customer::class.java),
                        ObjectHandler.getObject(it["subscription"].asMap(), Subscription::class.java))
            }.toMap()
        }
    }

    override fun getCustomerForMsisdn(msisdn: String): Either<StoreError, Customer> = readTransaction {
        read("""
                MATCH (customer:${customerEntity.name})-[:${subscriptionRelation.relation.name}]->(subscription:${subscriptionEntity.name} {msisdn: '${msisdn}'})
                RETURN customer
                """.trimIndent(),
                transaction) {
            if (it.hasNext())
                Either.right(customerEntity.createEntity(it.single().get("customer").asMap()))
            else
                Either.left(NotFoundError(type = customerEntity.name, id = msisdn))
        }
    }

    //
    // For metrics
    //

    override fun getSubscriberCount(): Long = readTransaction {
        read("""
                MATCH (customer:${customerEntity.name})
                RETURN count(customer) AS count
                """.trimIndent(),
                transaction) { result ->
            result.single().get("count").asLong()
        }
    }

    override fun getReferredSubscriberCount(): Long = readTransaction {
        read("""
                MATCH (:${customerEntity.name})-[:${referredRelation.relation.name}]->(customer:${customerEntity.name})
                RETURN count(customer) AS count
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
                MATCH (customer:${customerEntity.name})-[:${purchaseRecordRelation.relation.name}]->(product:${productEntity.name})
                WHERE product.`price/amount` > 0
                RETURN count(customer) AS count
                """.trimIndent(), transaction) { result ->
            result.single().get("count").asLong()
        }
    }

    //
    // For plans and subscriptions
    //

    override fun getPlan(planId: String): Either<StoreError, Plan> = readTransaction {
        plansStore.get(planId, transaction)
    }

    override fun getPlans(identity: org.ostelco.prime.model.Identity): Either<StoreError, List<Plan>> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    subscribesToPlanRelationStore.get(customerId, transaction)
                }
    }

    override fun createPlan(plan: Plan): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {

                productStore.get(plan.id, transaction)
                        .fold(
                                { Either.right(Unit) },
                                {
                                    Either.left(AlreadyExistsError(type = productEntity.name, id = "Failed to find product associated with plan ${plan.id}"))
                                }
                        ).bind()
                plansStore.get(plan.id, transaction)
                        .fold(
                                { Either.right(Unit) },
                                {
                                    Either.left(AlreadyExistsError(type = planEntity.name, id = "Failed to find plan ${plan.id}"))
                                }
                        ).bind()

                val productInfo = paymentProcessor.createProduct(plan.id)
                        .mapLeft {
                            NotCreatedError(type = planEntity.name, id = "Failed to create plan ${plan.id}",
                                    error = it)
                        }.linkReversalActionToTransaction(transaction) {
                            paymentProcessor.removeProduct(it.id)
                        }.bind()
                val planInfo = paymentProcessor.createPlan(productInfo.id, plan.price.amount, plan.price.currency,
                        PaymentProcessor.Interval.valueOf(plan.interval.toUpperCase()), plan.intervalCount)
                        .mapLeft {
                            NotCreatedError(type = planEntity.name, id = "Failed to create plan ${plan.id}",
                                    error = it)
                        }.linkReversalActionToTransaction(transaction) {
                            paymentProcessor.removePlan(it.id)
                        }.bind()

                /* The associated product to the plan. Note that:
                         sku - name of the plan
                         property value 'productType' is set to "plan"
                    TODO: Complete support for 'product-class' and merge 'plan' and 'product' objects
                          into one object differentiated by 'product-class'. */
                val product = Product(sku = plan.id, price = plan.price,
                        properties = plan.properties + mapOf(
                                "productType" to "plan",
                                "interval" to plan.interval,
                                "intervalCount" to plan.intervalCount.toString()),
                        presentation = plan.presentation)

                /* Propagates errors from lower layer if any. */
                productStore.create(product, transaction)
                        .bind()
                plansStore.create(plan.copy(properties = plan.properties.plus(mapOf(
                        "planId" to planInfo.id,
                        "productId" to productInfo.id))), transaction)
                        .bind()
                planProductRelationStore.create(plan.id, product.id, transaction)
                        .bind()
                plansStore.get(plan.id, transaction)
                        .bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun deletePlan(planId: String): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val plan = plansStore.get(planId, transaction)
                        .bind()
                /* The name of the product is the same as the name of the corresponding plan. */
                productStore.get(planId, transaction)
                        .bind()
                planProductRelationStore.get(plan.id, transaction)
                        .bind()

                /* Not removing the product due to purchase references. */

                /* Removing the plan will remove the plan itself and all relations going to it. */
                plansStore.delete(plan.id, transaction)
                        .bind()

                /* Lookup in payment backend will fail if no value found for 'planId'. */
                paymentProcessor.removePlan(plan.properties.getOrDefault("planId", "missing"))
                        .mapLeft {
                            NotDeletedError(type = planEntity.name, id = "Failed to delete ${plan.id}",
                                    error = it)
                        }.linkReversalActionToTransaction(transaction) {
                            /* (Nothing to do.) */
                        }.flatMap {
                            Either.right(Unit)
                        }.bind()
                /* Lookup in payment backend will fail if no value found for 'productId'. */
                paymentProcessor.removeProduct(plan.properties.getOrDefault("productId", "missing"))
                        .mapLeft {
                            NotDeletedError(type = planEntity.name, id = "Failed to delete ${plan.id}",
                                    error = it)
                        }.linkReversalActionToTransaction(transaction) {
                            /* (Nothing to do.) */
                        }.bind()
                plan
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun subscribeToPlan(identity: org.ostelco.prime.model.Identity, planId: String, trialEnd: Long): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = getCustomerId(identity = identity, transaction = transaction)
                        .bind()
                val customer = customerStore.get(customerId, transaction)
                        .bind()
                val plan = plansStore.get(planId, transaction)
                        .bind()
                planProductRelationStore.get(plan.id, transaction)
                        .bind()
                val profileInfo = paymentProcessor.getPaymentProfile(customer.id)
                        .mapLeft {
                            NotFoundError(type = planEntity.name, id = "Failed to subscribe ${customer.id} to ${plan.id}",
                                    error = it)
                        }.bind()
                subscribesToPlanRelationStore.create(customer.id, plan.id, transaction)
                        .bind()

                /* Lookup in payment backend will fail if no value found for 'planId'. */
                val subscriptionInfo = paymentProcessor.createSubscription(plan.properties.getOrDefault("planId", "missing"),
                        profileInfo.id, trialEnd)
                        .mapLeft {
                            NotCreatedError(type = planEntity.name, id = "Failed to subscribe $customerId to ${plan.id}",
                                    error = it)
                        }.linkReversalActionToTransaction(transaction) {
                            paymentProcessor.cancelSubscription(it.id)
                        }.bind()

                /* Store information from payment backend for later use. */
                subscribesToPlanRelationStore.setProperties(customerId, planId, mapOf("subscriptionId" to subscriptionInfo.id,
                        "created" to subscriptionInfo.created,
                        "trialEnd" to subscriptionInfo.trialEnd), transaction)
                        .flatMap {
                            Either.right(plan)
                        }.bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun unsubscribeFromPlan(identity: org.ostelco.prime.model.Identity, planId: String, atIntervalEnd: Boolean): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val plan = plansStore.get(planId, transaction)
                        .bind()
                val customerId = getCustomerId(identity = identity, transaction = transaction)
                        .bind()
                val properties = subscribesToPlanRelationStore.getProperties(customerId, planId, transaction)
                        .bind()
                paymentProcessor.cancelSubscription(properties["subscriptionId"].toString(), atIntervalEnd)
                        .mapLeft {
                            NotDeletedError(type = planEntity.name, id = "$customerId -> ${plan.id}",
                                    error = it)
                        }.flatMap {
                            Either.right(Unit)
                        }.bind()

                subscribesToPlanRelationStore.delete(customerId, planId, transaction)
                        .flatMap {
                            Either.right(plan)
                        }.bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun subscriptionPurchaseReport(invoiceId: String, customerId: String, sku: String, amount: Long, currency: String): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val product = productStore.get(sku, transaction)
                        .bind()
                val plan = planProductRelationStore.getFrom(sku, transaction)
                        .flatMap {
                            Either.right(it.get(0))
                        }.bind()
                val purchaseRecord = PurchaseRecord(
                        id = invoiceId,
                        product = product,
                        timestamp = Instant.now().toEpochMilli(),
                        msisdn = "")

                createPurchaseRecordRelation(customerId, purchaseRecord, transaction)
                        .flatMap {
                            Either.right(plan)
                        }.bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    //
    // For refunds
    //

    private fun checkPurchaseRecordForRefund(purchaseRecord: PurchaseRecord): Either<PaymentError, Unit> {
        if (purchaseRecord.refund != null) {
            logger.error("Trying to refund again, ${purchaseRecord.id}, refund ${purchaseRecord.refund?.id}")
            return Either.left(ForbiddenError("Trying to refund again"))
        } else if (purchaseRecord.product.price.amount == 0) {
            logger.error("Trying to refund a free product, ${purchaseRecord.id}")
            return Either.left(ForbiddenError("Trying to refund a free purchase"))
        }
        return Either.right(Unit)
    }

    private fun updatePurchaseRecord(
            purchase: PurchaseRecord,
            primeTransaction: PrimeTransaction): Either<StoreError, Unit> {
        return changablePurchaseRelationStore.update(purchase, primeTransaction)
                .ifFailedThenRollback(primeTransaction)
    }

    override fun refundPurchase(
            customerId: String,
            purchaseRecordId: String,
            reason: String): Either<PaymentError, ProductInfo> = writeTransaction {
        IO {
            Either.monad<PaymentError>().binding {
                val purchaseRecord = changablePurchaseRelationStore.get(purchaseRecordId, transaction)
                        // If we can't find the record, return not-found
                        .mapLeft {
                            org.ostelco.prime.paymentprocessor.core.NotFoundError("Purchase Record unavailable",
                                    error = it)
                        }.bind()
                checkPurchaseRecordForRefund(purchaseRecord).bind()
                val refundId = paymentProcessor.refundCharge(
                        purchaseRecord.id,
                        purchaseRecord.product.price.amount,
                        purchaseRecord.product.price.currency).bind()
                val refund = RefundRecord(refundId, reason, Instant.now().toEpochMilli())
                val changedPurchaseRecord = PurchaseRecord(
                        id = purchaseRecord.id,
                        product = purchaseRecord.product,
                        timestamp = purchaseRecord.timestamp,
                        msisdn = "",
                        refund = refund)
                updatePurchaseRecord(changedPurchaseRecord, transaction)
                        .mapLeft {
                            logger.error("failed to update purchase record, for refund $refund.id, chargeId $purchaseRecordId, payment has been refunded in Stripe")
                            BadGatewayError("Failed to update purchase record for refund ${refund.id}",
                                    error = it)
                        }.bind()
                analyticsReporter.reportPurchaseInfo(purchaseRecord, customerId, "refunded")
                ProductInfo(purchaseRecord.product.sku)
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
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

    private val subscriberToSegmentRelation = RelationType(BELONG_TO_SEGMENT, customerEntity, segmentEntity, Void::class.java)
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

    //
    // Indexes
    //

    fun createIndex() = writeTransaction {
        write(query = "CREATE INDEX ON :${identityEntity.name}(id)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${subscriptionEntity.name}(id)", transaction = transaction) {}
    }
}
