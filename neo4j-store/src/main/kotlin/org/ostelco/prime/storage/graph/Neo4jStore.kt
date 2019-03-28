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
import org.ostelco.prime.ekyc.DaveKycService
import org.ostelco.prime.ekyc.MyInfoKycService
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.ChangeSegment
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.CustomerRegionStatus
import org.ostelco.prime.model.CustomerRegionStatus.APPROVED
import org.ostelco.prime.model.CustomerRegionStatus.PENDING
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Plan
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.RefundRecord
import org.ostelco.prime.model.Region
import org.ostelco.prime.model.RegionDetails
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.ScanStatus
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.SimProfile
import org.ostelco.prime.model.SimProfileStatus.AVAILABLE_FOR_DOWNLOAD
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.EmailNotifier
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.sim.SimManager
import org.ostelco.prime.storage.AlreadyExistsError
import org.ostelco.prime.storage.ConsumptionResult
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
import org.ostelco.prime.storage.graph.Graph.writeSuspended
import org.ostelco.prime.storage.graph.Relation.BELONG_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.HAS_BUNDLE
import org.ostelco.prime.storage.graph.Relation.HAS_SIM_PROFILE
import org.ostelco.prime.storage.graph.Relation.HAS_SUBSCRIPTION
import org.ostelco.prime.storage.graph.Relation.IDENTIFIES
import org.ostelco.prime.storage.graph.Relation.LINKED_TO_BUNDLE
import org.ostelco.prime.storage.graph.Relation.OFFERED_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.OFFER_HAS_PRODUCT
import org.ostelco.prime.storage.graph.Relation.PURCHASED
import org.ostelco.prime.storage.graph.Relation.REFERRED
import org.ostelco.prime.storage.graph.StatusFlag.ADDRESS_AND_PHONE_NUMBER
import org.ostelco.prime.storage.graph.StatusFlag.JUMIO
import org.ostelco.prime.storage.graph.StatusFlag.MY_INFO
import org.ostelco.prime.storage.graph.StatusFlag.NRIC_FIN
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import javax.ws.rs.core.MultivaluedMap
import org.ostelco.prime.model.Identity as ModelIdentity
import org.ostelco.prime.paymentprocessor.core.NotFoundError as NotFoundPaymentError

enum class Relation {
    IDENTIFIES,                 // (Identity) -[IDENTIFIES]-> (Customer)
    HAS_SUBSCRIPTION,           // (Customer) -[HAS_SUBSCRIPTION]-> (Subscription)
    HAS_BUNDLE,                 // (Customer) -[HAS_BUNDLE]-> (Bundle)
    HAS_SIM_PROFILE,            // (Customer) -[HAS_SIM_PROFILE]-> (SimProfile)
    SUBSCRIBES_TO_PLAN,         // (Customer) -[SUBSCRIBES_TO_PLAN]-> (Plan)
    HAS_PRODUCT,                // (Plan) -[HAS_PRODUCT]-> (Product)
    LINKED_TO_BUNDLE,           // (Subscription) -[LINKED_TO_BUNDLE]-> (Bundle)
    PURCHASED,                  // (Customer) -[PURCHASED]-> (Product)
    REFERRED,                   // (Customer) -[REFERRED]-> (Customer)
    OFFERED_TO_SEGMENT,         // (Offer) -[OFFERED_TO_SEGMENT]-> (Segment)
    OFFER_HAS_PRODUCT,          // (Offer) -[OFFER_HAS_PRODUCT]-> (Product)
    BELONG_TO_SEGMENT,          // (Customer) -[BELONG_TO_SEGMENT]-> (Segment)
    CUSTOMER_STATE,             // (Customer) -[CUSTOMER_STATE]-> (CustomerState)
    EKYC_SCAN,                  // (Customer) -[EKYC_SCAN]-> (ScanInformation)
    BELONG_TO_REGION,           // (Customer) -[BELONG_TO_REGION]-> (Region)
    SUBSCRIPTION_FOR_REGION,    // (Subscription) -[SUBSCRIPTION_FOR_REGION]-> (Region)
    SIM_PROFILE_FOR_REGION,     // (SimProfile) -[SIM_PROFILE_FOR_REGION]-> (Region)
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

    private val simProfileEntity = EntityType(SimProfile::class.java)
    private val simProfileStore = EntityStore(simProfileEntity)

    private val planEntity = EntityType(Plan::class.java)
    private val plansStore = EntityStore(planEntity)

    private val regionEntity = EntityType(Region::class.java)
    private val regionStore = EntityStore(regionEntity)

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
            dataClass = None::class.java)
    private val subscriptionRelationStore = RelationStore(subscriptionRelation)

    private val customerToBundleRelation = RelationType(
            relation = HAS_BUNDLE,
            from = customerEntity,
            to = bundleEntity,
            dataClass = None::class.java)
    private val customerToBundleStore = RelationStore(customerToBundleRelation)

    private val subscriptionToBundleRelation = RelationType(
            relation = LINKED_TO_BUNDLE,
            from = subscriptionEntity,
            to = bundleEntity,
            dataClass = SubscriptionToBundle::class.java)
    private val subscriptionToBundleStore = RelationStore(subscriptionToBundleRelation)

    private val customerToSimProfileRelation = RelationType(
            relation = HAS_SIM_PROFILE,
            from = customerEntity,
            to = simProfileEntity,
            dataClass = None::class.java)
    private val customerToSimProfileStore = RelationStore(customerToSimProfileRelation)

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
            dataClass = None::class.java)
    private val referredRelationStore = RelationStore(referredRelation)

    private val subscribesToPlanRelation = RelationType(
            relation = Relation.SUBSCRIBES_TO_PLAN,
            from = customerEntity,
            to = planEntity,
            dataClass = PlanSubscription::class.java)
    private val subscribesToPlanRelationStore = UniqueRelationStore(subscribesToPlanRelation)

    private val customerRegionRelation = RelationType(
            relation = Relation.BELONG_TO_REGION,
            from = customerEntity,
            to = regionEntity,
            dataClass = CustomerRegion::class.java)
    private val customerRegionRelationStore = UniqueRelationStore(customerRegionRelation)

    private val scanInformationRelation = RelationType(
            relation = Relation.EKYC_SCAN,
            from = customerEntity,
            to = scanInformationEntity,
            dataClass = None::class.java)
    private val scanInformationRelationStore = UniqueRelationStore(scanInformationRelation)

    private val planProductRelation = RelationType(
            relation = Relation.HAS_PRODUCT,
            from = planEntity,
            to = productEntity,
            dataClass = None::class.java)
    private val planProductRelationStore = UniqueRelationStore(planProductRelation)

    private val subscriptionRegionRelation = RelationType(
            relation = Relation.SUBSCRIPTION_FOR_REGION,
            from = subscriptionEntity,
            to = regionEntity,
            dataClass = None::class.java)
    private val subscriptionRegionRelationStore = UniqueRelationStore(subscriptionRegionRelation)

    private val simProfileRegionRelation = RelationType(
            relation = Relation.SIM_PROFILE_FOR_REGION,
            from = simProfileEntity,
            to = regionEntity,
            dataClass = None::class.java)
    private val simProfileRegionRelationStore = UniqueRelationStore(simProfileRegionRelation)

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

    private fun getCustomerAndAnalyticsId(identity: org.ostelco.prime.model.Identity, transaction: Transaction): Either<StoreError, Pair<String, String>> {
        return identityStore.getRelated(id = identity.id, relationType = identifiesRelation, transaction = transaction)
                .flatMap {
                    if (it.isEmpty()) {
                        NotFoundError(type = identity.type, id = identity.id).left()
                    } else {
                        val customer = it.single()
                        Pair(customer.id, customer.analyticsId).right()
                    }
                }
    }

    //
    // Balance (Customer - Bundle)
    //

    override fun getBundles(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<Bundle>> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId -> customerStore.getRelated(customerId, customerToBundleRelation, transaction) }
    }

    override fun updateBundle(bundle: Bundle): Either<StoreError, Unit> = writeTransaction {
        bundleStore.update(bundle, transaction)
                .ifFailedThenRollback(transaction)
    }

    //
    // Customer
    //

    override fun getCustomer(identity: org.ostelco.prime.model.Identity): Either<StoreError, Customer> = readTransaction {
        getCustomer(identity = identity, transaction = transaction)
    }

    private fun getCustomer(
            identity: org.ostelco.prime.model.Identity,
            transaction: Transaction): Either<StoreError, Customer> = identityStore.getRelated(
            id = identity.id,
            relationType = identifiesRelation,
            transaction = transaction)
            .map(List<Customer>::single)

    private fun validateCreateCustomerParams(customer: Customer, referredBy: String?): Either<StoreError, Unit> =
            if (customer.referralId == referredBy) {
                Either.left(ValidationError(type = customerEntity.name, id = customer.id, message = "Referred by self"))
            } else {
                Unit.right()
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
                        PurchaseRecord(id = UUID.randomUUID().toString(), product = product, timestamp = Instant.now().toEpochMilli()),
                        transaction)
                customerToBundleStore.create(customer.id, bundleId, transaction).bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }
    // << END

    override fun updateCustomer(
            identity: org.ostelco.prime.model.Identity,
            nickname: String?,
            contactEmail: String?): Either<StoreError, Unit> = writeTransaction {

        getCustomer(identity = identity, transaction = transaction)
                .flatMap { existingCustomer ->
                    customerStore.update(
                            existingCustomer.copy(
                                    nickname = nickname ?: existingCustomer.nickname,
                                    contactEmail = contactEmail ?: existingCustomer.contactEmail),
                            transaction)
                }
                .ifFailedThenRollback(transaction)
    }

    override fun removeCustomer(identity: org.ostelco.prime.model.Identity): Either<StoreError, Unit> = writeTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    identityStore.delete(id = identity.id, transaction = transaction)
                    customerStore.exists(customerId, transaction)
                            .flatMap {
                                customerStore.getRelated(customerId, customerToBundleRelation, transaction)
                                        .map { it.forEach { bundle -> bundleStore.delete(bundle.id, transaction) } }
                                customerStore.getRelated(customerId, subscriptionRelation, transaction)
                                        .map { it.forEach { subscription -> subscriptionStore.delete(subscription.id, transaction) } }
                                customerStore.getRelated(customerId, scanInformationRelation, transaction)
                                        .map { it.forEach { scanInfo -> scanInformationStore.delete(scanInfo.id, transaction) } }
                            }
                            .flatMap { customerStore.delete(customerId, transaction) }
                }
                .ifFailedThenRollback(transaction)
    }

    //
    // Customer Region
    //

    override fun getAllRegionDetails(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<RegionDetails>> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    getRegionDetails(
                            customerId = customerId,
                            transaction = transaction).right()
                }
    }

    override fun getRegionDetails(
            identity: org.ostelco.prime.model.Identity,
            regionCode: String): Either<StoreError, RegionDetails> = readTransaction {

        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    getRegionDetails(
                            customerId = customerId,
                            regionCode = regionCode,
                            transaction = transaction)
                            .singleOrNull()
                            ?.right()
                            ?: NotFoundError(type = regionEntity.name, id = regionCode).left()
                }
    }


    private fun getRegionDetails(
            customerId: String,
            regionCode: String? = null,
            transaction: Transaction): Collection<RegionDetails> {

        val regionCodeClause = regionCode?.let { "{id: '$it'}" } ?: ""

        return read("""
                MATCH (c:${customerEntity.name} {id: '$customerId'})-[cr:${customerRegionRelation.name}]->(r:${regionEntity.name} $regionCodeClause)
                OPTIONAL MATCH (c)-[:${customerToSimProfileRelation.name}]->(sp:${simProfileEntity.name})-[:${simProfileRegionRelation.name}]->(r)
                RETURN cr, r, sp;
                """.trimIndent(),
                transaction) { statementResult ->
            statementResult
                    .list { record ->
                        val region = regionEntity.createEntity(record["r"].asMap())
                        val cr = customerRegionRelation.createRelation(record["cr"].asMap()).status
                        val simProfiles = if (record["sp"].isNull) {
                            emptyList()
                        } else {
                            listOf(simProfileEntity.createEntity(record["sp"].asMap()))
                        }
                        RegionDetails(
                                region = region,
                                status = cr,
                                simProfiles = simProfiles)
                    }
                    .requireNoNulls()
                    .groupBy { RegionDetails(region = it.region, status = it.status) }
                    .map { (key, value) ->
                        RegionDetails(
                                region = key.region,
                                status = key.status,
                                simProfiles = value.flatMap(RegionDetails::simProfiles))
                    }
        }
    }


    //
    // SIM Profile
    //

    private val simManager by lazy { getResource<SimManager>() }

    private val emailNotifier by lazy { getResource<EmailNotifier>() }

    private fun validateBundleList(bundles: List<Bundle>, customerId: String): Either<StoreError, Unit> =
            if (bundles.isEmpty()) {
                Either.left(NotFoundError(type = customerToBundleRelation.name, id = "$customerId -> *"))
            } else {
                Unit.right()
            }

    override fun provisionSimProfile(
            identity: org.ostelco.prime.model.Identity,
            regionCode: String,
            profileType: String): Either<StoreError, SimProfile> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = getCustomerId(identity = identity, transaction = transaction).bind()
                val bundles = customerStore.getRelated(customerId, customerToBundleRelation, transaction).bind()
                validateBundleList(bundles, customerId).bind()
                val customer = customerStore.get(customerId, transaction).bind()
                val status = customerRegionRelationStore
                        .get(fromId = customerId, toId = regionCode.toLowerCase(), transaction = transaction)
                        .bind()
                        .status
                isApproved(
                        status = status,
                        customerId = customerId,
                        regionCode = regionCode).bind()
                val region = regionStore.get(id = regionCode.toLowerCase(), transaction = transaction).bind()
                val simEntry = simManager.allocateNextEsimProfile(hlr = getHlr(region.id.toLowerCase()), phoneType = profileType)
                        .mapLeft { NotFoundError("eSIM profile", id = "loltel") }
                        .bind()
                simProfileStore.create(SimProfile(
                        iccId = simEntry.iccId,
                        eSimActivationCode = simEntry.eSimActivationCode,
                        status = AVAILABLE_FOR_DOWNLOAD),
                        transaction).bind()
                customerToSimProfileStore.create(
                        fromId = customerId,
                        toId = simEntry.iccId,
                        transaction = transaction).bind()
                simProfileRegionRelationStore.create(
                        fromId = simEntry.iccId,
                        toId = regionCode.toLowerCase(),
                        transaction = transaction).bind()
                simEntry.msisdnList.forEach { msisdn ->
                    subscriptionStore.create(Subscription(msisdn = msisdn), transaction).bind()
                    val subscription = subscriptionStore.get(msisdn, transaction).bind()
                    bundles.forEach { bundle ->
                        subscriptionToBundleStore.create(
                                from = subscription,
                                relation = SubscriptionToBundle(),
                                to = bundle,
                                transaction = transaction).bind()
                    }
                    subscriptionRelationStore.create(customer, subscription, transaction).bind()
                    subscriptionRegionRelationStore.create(
                            fromId = msisdn,
                            toId = regionCode.toLowerCase(),
                            transaction = transaction).bind()
                    // TODO vihang: link SimProfile to Subscription and unlink Subscription from Region
                }
                if (profileType != "android") {
                    emailNotifier.sendESimQrCodeEmail(
                            email = customer.contactEmail,
                            name = customer.nickname,
                            qrCode = simEntry.eSimActivationCode)
                            .mapLeft {
                                logger.error(NOTIFY_OPS_MARKER, "Failed to send email to {}", customer.contactEmail)
                            }
                }
                simProfileStore.get(simEntry.iccId, transaction).bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    private fun isApproved(
            status: CustomerRegionStatus,
            customerId: String,
            regionCode: String): Either<ValidationError, Unit> {

        return if (status != APPROVED) {
            ValidationError(
                    type = "customerRegionRelation",
                    id = "$customerId -> ${regionCode.toLowerCase()}",
                    message = "eKYC status is $status and not APPROVED.")
                    .left()
        } else {
            Unit.right()
        }
    }

    override fun getSimProfiles(
            identity: org.ostelco.prime.model.Identity,
            regionCode: String?): Either<StoreError, Collection<SimProfile>> = readTransaction {

        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    customerStore.getRelated(
                            id = customerId,
                            relationType = customerToSimProfileRelation,
                            transaction = transaction)
                }
    }

    private fun getHlr(regionCode: String): String {
        return "loltel"
    }

    //
    // Subscription
    //

    @Deprecated(message = "Use createSubscriptions instead")
    override fun addSubscription(identity: org.ostelco.prime.model.Identity, msisdn: String): Either<StoreError, Unit> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = Neo4jStoreSingleton.getCustomerId(identity = identity, transaction = transaction).bind()
                val bundles = customerStore.getRelated(customerId, customerToBundleRelation, transaction).bind()
                validateBundleList(bundles, customerId).bind()
                subscriptionStore.create(Subscription(msisdn), transaction).bind()
                val subscription = subscriptionStore.get(msisdn, transaction).bind()
                val customer = customerStore.get(customerId, transaction).bind()
                bundles.forEach { bundle ->
                    subscriptionToBundleStore.create(subscription, SubscriptionToBundle(), bundle, transaction).bind()
                }
                subscriptionRelationStore.create(customer, subscription, transaction).bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun getSubscriptions(identity: org.ostelco.prime.model.Identity, regionCode: String?): Either<StoreError, Collection<Subscription>> = readTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = getCustomerId(identity = identity, transaction = transaction).bind()
                if (regionCode == null) {
                    customerStore.getRelated(customerId, subscriptionRelation, transaction).bind()
                } else {
                    read<Either<StoreError, Collection<Subscription>>>("""
                        MATCH (:${customerEntity.name} {id: '$customerId'})
                        -[:${subscriptionRelation.name}]->(sn:${subscriptionEntity.name})
                        -[:${subscriptionRegionRelation.name}]->(:${regionEntity.name} {id: '${regionCode.toLowerCase()}'})
                        RETURN sn;
                        """.trimIndent(),
                            transaction) { statementResult ->
                        Either.right(statementResult
                                .list { subscriptionEntity.createEntity(it["sn"].asMap()) })
                    }.bind()
                }
            }.fix()
        }.unsafeRunSync()
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
                            -[:${customerToSegmentRelation.name}]->(:${segmentEntity.name})
                            <-[:${offerToSegmentRelation.name}]-(:${offerEntity.name})
                            -[:${offerToProductRelation.name}]->(product:${productEntity.name})
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
                            -[:${customerToSegmentRelation.name}]->(:${segmentEntity.name})
                            <-[:${offerToSegmentRelation.name}]-(:${offerEntity.name})
                            -[:${offerToProductRelation.name}]->(product:${productEntity.name} {sku: '$sku'})
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
    override suspend fun consume(msisdn: String, usedBytes: Long, requestedBytes: Long, callback: (Either<StoreError, ConsumptionResult>) -> Unit) {

        // Note: _LOCK_ dummy property is set in the relation 'r' and node 'bundle' so that they get locked.
        // Ref: https://neo4j.com/docs/java-reference/current/transactions/#transactions-isolation

        suspendedWriteTransaction {

            writeSuspended("""
                            MATCH (sn:${subscriptionEntity.name} {id: '$msisdn'})-[r:${subscriptionToBundleRelation.name}]->(bundle:${bundleEntity.name})
                            SET bundle._LOCK_ = true, r._LOCK_ = true
                            WITH r, bundle, sn.analyticsId AS msisdnAnalyticsId, (CASE WHEN ((toInteger(bundle.balance) + toInteger(r.reservedBytes) - $usedBytes) > 0) THEN (toInteger(bundle.balance) + toInteger(r.reservedBytes) - $usedBytes) ELSE 0 END) AS tmpBalance
                            WITH r, bundle, msisdnAnalyticsId, tmpBalance, (CASE WHEN (tmpBalance < $requestedBytes) THEN tmpBalance ELSE $requestedBytes END) as tmpGranted
                            SET r.reservedBytes = toString(tmpGranted), bundle.balance = toString(tmpBalance - tmpGranted)
                            REMOVE r._LOCK_, bundle._LOCK_
                            RETURN msisdnAnalyticsId, r.reservedBytes AS granted, bundle.balance AS balance
                            """.trimIndent(),
                    transaction) { completionStage ->
                completionStage
                        .thenApply { it.singleAsync() }
                        .thenAcceptAsync {
                            it.handle { record, throwable ->

                                if (throwable != null) {
                                    callback(NotUpdatedError(type = "Balance for ${subscriptionEntity.name}", id = msisdn).left())
                                } else {
                                    val balance = record.get("balance").asString("0").toLong()
                                    val granted = record.get("granted").asString("0").toLong()
                                    val msisdnAnalyticsId = record.get("msisdnAnalyticsId").asString(msisdn)

                                    logger.trace("requestedBytes = %,d, balance = %,d, granted = %,d".format(requestedBytes, balance, granted))
                                    callback(ConsumptionResult(msisdnAnalyticsId = msisdnAnalyticsId, granted = granted, balance = balance).right())
                                }
                            }
                        }
            }
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

                val (customerId, customerAnalyticsId) = getCustomerAndAnalyticsId(identity = identity, transaction = transaction)
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
                        timestamp = Instant.now().toEpochMilli())
                createPurchaseRecordRelation(customerId, purchaseRecord, transaction)
                        .mapLeft {
                            logger.error("Failed to save purchase record, for paymentCustomerId $paymentCustomerId, chargeId $chargeId, payment will be unclaimed in Stripe")
                            BadGatewayError("Failed to save purchase record",
                                    error = it)
                        }.bind()

                //TODO: While aborting transactions, send a record with "reverted" status
                analyticsReporter.reportPurchaseInfo(purchaseRecord = purchaseRecord, customerAnalyticsId = customerAnalyticsId, status = "success")

                // Topup
                val bytes = product.properties["noOfBytes"]?.replace("_", "")?.toLongOrNull() ?: 0L

                if (bytes == 0L) {
                    logger.error("Product with 0 bytes: sku = ${product.sku}")
                }

                write("""
                    MATCH (cr:${customerEntity.name} { id:'$customerId' })-[:${customerToBundleRelation.name}]->(bundle:${bundleEntity.name})
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
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
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
                            .map { list -> list.map { it.nickname } }
                }
    }

    override fun getReferredBy(identity: org.ostelco.prime.model.Identity): Either<StoreError, String?> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    customerStore.getRelatedFrom(customerId, referredRelation, transaction)
                            .map { it.singleOrNull()?.nickname }
                }
    }

    internal fun createOrUpdateCustomerRegionSetting(
            customerId: String,
            status: CustomerRegionStatus,
            regionCode: String): Either<StoreError, Unit> = writeTransaction {

        createOrUpdateCustomerRegionSetting(
                customerId = customerId,
                status = status,
                regionCode = regionCode.toLowerCase(),
                transaction = transaction)
    }

    private fun createOrUpdateCustomerRegionSetting(
            customerId: String,
            status: CustomerRegionStatus,
            regionCode: String,
            transaction: PrimeTransaction): Either<StoreError, Unit> =

            customerRegionRelationStore
                    .createIfAbsent(
                            fromId = customerId,
                            relation = CustomerRegion(status),
                            toId = regionCode.toLowerCase(),
                            transaction = transaction)
                    .flatMap {
                        if (status == APPROVED) {
                            assignCustomerToRegionSegment(
                                    customerId = customerId,
                                    regionCode = regionCode,
                                    transaction = transaction)
                        } else {
                            Unit.right()
                        }
                    }

    private fun assignCustomerToRegionSegment(
            customerId: String,
            regionCode: String,
            transaction: Transaction): Either<StoreError, Unit> {

        return customerToSegmentStore.create(
                fromId = customerId,
                toId = getSegmentNameFromCountryCode(regionCode),
                transaction = transaction).mapLeft { storeError ->

            if (storeError is NotCreatedError && storeError.type == customerToSegmentRelation.name) {
                ValidationError(type = customerEntity.name, id = customerId, message = "Unsupported region: $regionCode")
            } else {
                storeError
            }
        }
    }

    //
    // eKYC - Jumio
    //

    override fun createNewJumioKycScanId(
            identity: org.ostelco.prime.model.Identity,
            regionCode: String): Either<StoreError, ScanInformation> = writeTransaction {

        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    // Generate new id for the scan
                    val scanId = UUID.randomUUID().toString()
                    val newScan = ScanInformation(scanId = scanId, countryCode = regionCode, status = ScanStatus.PENDING, scanResult = null)
                    createOrUpdateCustomerRegionSetting(
                            customerId = customerId, status = PENDING, regionCode = regionCode.toLowerCase(), transaction = transaction)
                            .flatMap {
                                scanInformationStore.create(newScan, transaction)
                            }
                            .flatMap {
                                scanInformationRelationStore.createIfAbsent(customerId, newScan.id, transaction)
                            }
                            .flatMap {
                                newScan.right()
                            }
                }
    }

    private fun getCustomerUsingScanId(scanId: String, transaction: Transaction): Either<StoreError, Customer> {
        return read("""
                MATCH (customer:${customerEntity.name})-[:${scanInformationRelation.name}]->(scanInformation:${scanInformationEntity.name} {scanId: '$scanId'})
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
                MATCH (scanInformation:${scanInformationEntity.name} {scanId: '$scanId'})
                RETURN scanInformation
                """.trimIndent(),
                transaction) {
            if (it.hasNext())
                Either.right(scanInformationEntity.createEntity(it.single().get("scanInformation").asMap()).countryCode)
            else
                Either.left(NotFoundError(type = scanInformationEntity.name, id = scanId))
        }
    }

    // TODO merge into a single query which will use customerId and scanId
    override fun getScanInformation(identity: org.ostelco.prime.model.Identity, scanId: String): Either<StoreError, ScanInformation> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    scanInformationStore.get(scanId, transaction).flatMap { scanInformation ->
                        getCustomerUsingScanId(scanInformation.scanId, transaction).flatMap { customer ->
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
        getCustomerUsingScanId(scanInformation.scanId, transaction).flatMap { customer ->
            scanInformationStore.update(scanInformation, transaction).flatMap {
                logger.info("updating scan Information for : ${customer.contactEmail} id: ${scanInformation.scanId} status: ${scanInformation.status}")

                when (scanInformation.status) {

                    ScanStatus.PENDING -> CustomerRegionStatus.PENDING.right()

                    ScanStatus.APPROVED -> {

                        logger.info("Inserting scan Information to cloud storage : id: ${scanInformation.scanId} countryCode: ${scanInformation.countryCode}")
                        scanInformationDatastore.upsertVendorScanInformation(customer.id, scanInformation.countryCode, vendorData)
                                .flatMap { CustomerRegionStatus.APPROVED.right() }
                    }

                    ScanStatus.REJECTED -> CustomerRegionStatus.REJECTED.right()

                }.flatMap { customerRegionStatus ->

                    createOrUpdateCustomerRegionSetting(
                            customerId = customer.id,
                            status = customerRegionStatus,
                            regionCode = scanInformation.countryCode,
                            transaction = transaction)
                }
            }
        }
    }

    //
    // eKYC - MyInfo
    //

    private val myInfoKycService by lazy { getResource<MyInfoKycService>() }

    override fun getCustomerMyInfoData(
            identity: org.ostelco.prime.model.Identity,
            authorisationCode: String): Either<StoreError, String> = writeTransaction {

        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    setStatusFlag(
                            customerId = customerId,
                            regionCode = "sg",
                            flag = MY_INFO,
                            transaction = transaction)
                }
                .flatMap { myInfoKycService.getPersonData(authorisationCode).right() }
                .ifFailedThenRollback(transaction)
    }

    //
    // eKYC - NRIC/FIN
    //

    private val daveKycService by lazy { getResource<DaveKycService>() }

    override fun checkNricFinIdUsingDave(
            identity: org.ostelco.prime.model.Identity,
            nricFinId: String): Either<StoreError, Unit> = writeTransaction {

        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    setStatusFlag(
                            customerId = customerId,
                            regionCode = "sg",
                            flag = NRIC_FIN,
                            transaction = transaction)
                }
                .flatMap {
                    if (daveKycService.validate(nricFinId)) {
                        Unit.right()
                    } else {
                        ValidationError(type = "NRIC/FIN ID", id = nricFinId, message = "Invalid NRIC/FIN ID").left()
                    }
                }
                .ifFailedThenRollback(transaction)
    }

    //
    // eKYC - Address and Phone number
    //
    override fun saveAddressAndPhoneNumber(
            identity: org.ostelco.prime.model.Identity,
            address: String,
            phoneNumber: String): Either<StoreError, Unit> = writeTransaction {

        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    setStatusFlag(
                            customerId = customerId,
                            regionCode = "sg",
                            flag = ADDRESS_AND_PHONE_NUMBER,
                            transaction = transaction)
                }
                .flatMap { Unit.right() }
                .ifFailedThenRollback(transaction)
    }

    private fun setStatusFlag(
            customerId: String,
            regionCode: String,
            flag: StatusFlag,
            transaction: Transaction): Either<StoreError, Int> {

        val approvedBitmapSet = getApprovedBitmapSet(regionCode)

        // retry 5 times
        for (i in 1..5) {

            // existing bitMapStatusFlags and customerRegionStatus
            val existing = read("""
                MATCH (:${customerEntity.name} { id: '$customerId' })-[r:${customerRegionRelation.name}]->(:${regionEntity.name} { id: '$regionCode' })
                RETURN r.bitMapStatusFlags, r.customerRegionStatus
                """.trimIndent(),
                    transaction) { result ->
                val record = result.single()
                Pair(record["r.bitMapStatusFlags"].asString().toInt(), CustomerRegionStatus.valueOf(record["r.customerRegionStatus"].asString()))
            }

            val newBitMap = StatusFlags.bitMapStatusFlags(flag).inv().and(existing.first)

            val updated: Boolean = if (existing.second == PENDING && approvedBitmapSet.any { it.inv().and(newBitMap) == 0 }) {
                // if existing status is PENDING and newBitMap is found in approvedBitMap Set
                assignCustomerToRegionSegment(
                        customerId = customerId,
                        regionCode = regionCode,
                        transaction = transaction)

                write("""
                    MATCH (:${customerEntity.name} { id: '$customerId' })-[r:${customerRegionRelation.name} { bitMapStatusFlags: ${existing.first} } ]->(:${regionEntity.name} { id: '$regionCode' })
                    SET r.bitMapStatusFlags = $newBitMap, r.customerRegionStatus = '${CustomerRegionStatus.APPROVED}'
                    RETURN r.bitMapStatusFlags
                    """.trimIndent(),
                        transaction) { result ->
                    result.summary().counters().propertiesSet() == 2
                }
            } else {
                write("""
                    MATCH (:${customerEntity.name} { id: '$customerId' })-[r:${customerRegionRelation.name} { bitMapStatusFlags: ${existing.first} } ]->(:${regionEntity.name} { id: '$regionCode' })
                    SET r.bitMapStatusFlags = $newBitMap
                    RETURN r.bitMapStatusFlags
                    """.trimIndent(),
                        transaction) { result ->
                    result.summary().counters().propertiesSet() == 1
                }
            }

            if (updated) {
                return newBitMap.right()
            }
        }

        return NotUpdatedError(type = customerRegionRelation.name, id = "$customerId -> $regionCode").left()
    }

    private fun getInitialBitmap(regionCode: String): Int {
        return when (regionCode.toLowerCase()) {
            "sg" -> StatusFlags.bitMapStatusFlags(JUMIO, MY_INFO, NRIC_FIN, ADDRESS_AND_PHONE_NUMBER)
            else -> StatusFlags.bitMapStatusFlags(JUMIO)
        }
    }

    private fun getApprovedBitmapSet(regionCode: String): Set<Int> {
        return when (regionCode.toLowerCase()) {
            "sg" -> setOf(StatusFlags.bitMapStatusFlags(MY_INFO),
                    StatusFlags.bitMapStatusFlags(JUMIO, NRIC_FIN, ADDRESS_AND_PHONE_NUMBER))
            else -> setOf(StatusFlags.bitMapStatusFlags(JUMIO))
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
                MATCH (subscription:${subscriptionEntity.name})-[:${subscriptionToBundleRelation.name}]->(bundle:${bundleEntity.name})<-[:${customerToBundleRelation.name}]-(:${customerEntity.name})
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
                MATCH (:${customerEntity.name})-[:${customerToBundleRelation.name}]->(bundle:${bundleEntity.name})<-[:${subscriptionToBundleRelation.name}]-(:${subscriptionEntity.name})
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
                MATCH (customer:${customerEntity.name})-[:${customerToBundleRelation.name}]->(bundle:${bundleEntity.name})
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
                MATCH (customer:${customerEntity.name})-[:${subscriptionRelation.name}]->(subscription:${subscriptionEntity.name})
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
                MATCH (customer:${customerEntity.name})-[:${subscriptionRelation.name}]->(subscription:${subscriptionEntity.name} {msisdn: '$msisdn'})
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

    override fun getCustomerCount(): Long = readTransaction {
        read("""
                MATCH (customer:${customerEntity.name})
                RETURN count(customer) AS count
                """.trimIndent(),
                transaction) { result ->
            result.single().get("count").asLong()
        }
    }

    override fun getReferredCustomerCount(): Long = readTransaction {
        read("""
                MATCH (:${customerEntity.name})-[:${referredRelation.name}]->(customer:${customerEntity.name})
                RETURN count(customer) AS count
                """.trimIndent(),
                transaction) { result ->
            result.single().get("count").asLong()
        }
    }

    override fun getPaidCustomerCount(): Long = readTransaction {
        read("""
                MATCH (customer:${customerEntity.name})-[:${purchaseRecordRelation.name}]->(product:${productEntity.name})
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
                    customerStore.getRelated(id = customerId, relationType = subscribesToPlanRelation, transaction = transaction)
                }
    }

    override fun createPlan(plan: Plan): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {

                productStore.get(plan.id, transaction)
                        .fold(
                                { Unit.right() },
                                {
                                    Either.left(AlreadyExistsError(type = productEntity.name, id = "Failed to find product associated with plan ${plan.id}"))
                                }
                        ).bind()
                plansStore.get(plan.id, transaction)
                        .fold(
                                { Unit.right() },
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
                plansStore.getRelated(id = plan.id, relationType = planProductRelation, transaction = transaction)
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
                            Unit.right()
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
                plansStore.getRelated(id = plan.id, relationType = planProductRelation, transaction = transaction)
                        .bind()
                val profileInfo = paymentProcessor.getPaymentProfile(customer.id)
                        .mapLeft {
                            NotFoundError(type = planEntity.name, id = "Failed to subscribe ${customer.id} to ${plan.id}",
                                    error = it)
                        }.bind()

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
                subscribesToPlanRelationStore.create(
                        fromId = customerId,
                        relation = PlanSubscription(
                                subscriptionId = subscriptionInfo.id,
                                created = subscriptionInfo.created,
                                trialEnd = subscriptionInfo.trialEnd),
                        toId = planId,
                        transaction = transaction)
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
                val planSubscription = subscribesToPlanRelationStore.get(customerId, planId, transaction)
                        .bind()
                paymentProcessor.cancelSubscription(planSubscription.subscriptionId, atIntervalEnd)
                        .mapLeft {
                            NotDeletedError(type = planEntity.name, id = "$customerId -> ${plan.id}",
                                    error = it)
                        }.flatMap {
                            Unit.right()
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
                val plan = productStore.getRelatedFrom(id = sku, relationType = planProductRelation, transaction = transaction)
                        .flatMap {
                            it[0].right()
                        }.bind()
                val purchaseRecord = PurchaseRecord(
                        id = invoiceId,
                        product = product,
                        timestamp = Instant.now().toEpochMilli())

                createPurchaseRecordRelation(customerId, purchaseRecord, transaction).bind()
                plan
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
        return Unit.right()
    }

    private fun updatePurchaseRecord(
            purchase: PurchaseRecord,
            primeTransaction: PrimeTransaction): Either<StoreError, Unit> = changablePurchaseRelationStore.update(purchase, primeTransaction)

    override fun refundPurchase(
            identity: org.ostelco.prime.model.Identity,
            purchaseRecordId: String,
            reason: String): Either<PaymentError, ProductInfo> = writeTransaction {
        IO {
            Either.monad<PaymentError>().binding {
                val (_, customerAnalyticsId) = getCustomerAndAnalyticsId(identity = identity, transaction = transaction)
                        .mapLeft {
                            logger.error("Failed to find customer with identity - $identity")
                            NotFoundPaymentError("Failed to find customer with identity - $identity",
                                    error = it)
                        }.bind()
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
                        refund = refund)
                updatePurchaseRecord(changedPurchaseRecord, transaction)
                        .mapLeft {
                            logger.error("failed to update purchase record, for refund $refund.id, chargeId $purchaseRecordId, payment has been refunded in Stripe")
                            BadGatewayError("Failed to update purchase record for refund ${refund.id}",
                                    error = it)
                        }.bind()
                analyticsReporter.reportPurchaseInfo(purchaseRecord, customerAnalyticsId, "refunded")
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

    private val offerToSegmentRelation = RelationType(OFFERED_TO_SEGMENT, offerEntity, segmentEntity, None::class.java)
    private val offerToSegmentStore = RelationStore(offerToSegmentRelation)

    private val offerToProductRelation = RelationType(OFFER_HAS_PRODUCT, offerEntity, productEntity, None::class.java)
    private val offerToProductStore = RelationStore(offerToProductRelation)

    private val customerToSegmentRelation = RelationType(BELONG_TO_SEGMENT, customerEntity, segmentEntity, None::class.java)
    private val customerToSegmentStore = RelationStore(customerToSegmentRelation)

    private val productClassEntity = EntityType(ProductClass::class.java)
    private val productClassStore = EntityStore(productClassEntity)

    //
    // Region
    //

    override fun createRegion(region: Region): Either<StoreError, Unit> = writeTransaction {
        regionStore.create(region, transaction)
                .ifFailedThenRollback(transaction)
    }

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
                .flatMap { customerToSegmentStore.create(segment.subscribers, segment.id, transaction) }
    }

    override fun updateSegment(segment: Segment): Either<StoreError, Unit> = writeTransaction {
        updateSegment(segment, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun updateSegment(segment: Segment, transaction: Transaction): Either<StoreError, Unit> {
        return customerToSegmentStore.removeAll(toId = segment.id, transaction = transaction)
                .flatMap { customerToSegmentStore.create(segment.subscribers, segment.id, transaction) }
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

        var result = Unit.right() as Either<StoreError, Unit>

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
                initial = Unit.right() as Either<StoreError, Unit>,
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
                initial = Unit.right() as Either<StoreError, Unit>,
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
        write(query = "CREATE INDEX ON :${bundleEntity.name}(id)", transaction = transaction) {}
    }
}
