package org.ostelco.prime.storage.graph

import arrow.core.Either
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.leftIfNull
import arrow.core.right
import arrow.effects.IO
import arrow.instances.either.monad.monad
import org.neo4j.driver.v1.Transaction
import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.dsl.readTransaction
import org.ostelco.prime.dsl.suspendedWriteTransaction
import org.ostelco.prime.dsl.writeTransaction
import org.ostelco.prime.ekyc.DaveKycService
import org.ostelco.prime.ekyc.MyInfoKycService
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.ChangeSegment
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.CustomerRegionStatus
import org.ostelco.prime.model.CustomerRegionStatus.APPROVED
import org.ostelco.prime.model.CustomerRegionStatus.PENDING
import org.ostelco.prime.model.FCMStrings
import org.ostelco.prime.model.HasId
import org.ostelco.prime.model.KycStatus
import org.ostelco.prime.model.KycStatus.REJECTED
import org.ostelco.prime.model.KycType
import org.ostelco.prime.model.KycType.ADDRESS_AND_PHONE_NUMBER
import org.ostelco.prime.model.KycType.JUMIO
import org.ostelco.prime.model.KycType.MY_INFO
import org.ostelco.prime.model.KycType.NRIC_FIN
import org.ostelco.prime.model.PaymentType.SUBSCRIPTION
import org.ostelco.prime.model.Plan
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.MEMBERSHIP
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.RefundRecord
import org.ostelco.prime.model.Region
import org.ostelco.prime.model.RegionDetails
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.ScanStatus
import org.ostelco.prime.model.SimProfileStatus.NOT_READY
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.EmailNotifier
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.InvoicePaymentInfo
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.PaymentStatus
import org.ostelco.prime.paymentprocessor.core.PlanAlredyPurchasedError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.paymentprocessor.core.SubscriptionDetailsInfo
import org.ostelco.prime.securearchive.SecureArchiveService
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
import org.ostelco.prime.storage.SystemError
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
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import javax.ws.rs.core.MultivaluedMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KClass
import org.ostelco.prime.model.Identity as ModelIdentity
import org.ostelco.prime.paymentprocessor.core.NotFoundError as NotFoundPaymentError

enum class Relation(
        val from: KClass<out HasId>,
        val to: KClass<out HasId>) {

    IDENTIFIES(from = Identity::class, to = Customer::class),                           // (Identity) -[IDENTIFIES]-> (Customer)

    HAS_SUBSCRIPTION(from = Customer::class, to = Subscription::class),                 // (Customer) -[HAS_SUBSCRIPTION]-> (Subscription)

    HAS_BUNDLE(from = Customer::class, to = Bundle::class),                             // (Customer) -[HAS_BUNDLE]-> (Bundle)

    HAS_SIM_PROFILE(from = Customer::class, to = SimProfile::class),                    // (Customer) -[HAS_SIM_PROFILE]-> (SimProfile)

    SUBSCRIBES_TO_PLAN(from = Customer::class, to = Plan::class),                       // (Customer) -[SUBSCRIBES_TO_PLAN]-> (Plan)

    LINKED_TO_BUNDLE(from = Subscription::class, to = Bundle::class),                   // (Subscription) -[LINKED_TO_BUNDLE]-> (Bundle)

    PURCHASED(from = Customer::class, to = Product::class),                             // (Customer) -[PURCHASED]-> (Product)

    REFERRED(from = Customer::class, to = Customer::class),                             // (Customer) -[REFERRED]-> (Customer)

    OFFERED_TO_SEGMENT(from = Offer::class, to = Segment::class),                       // (Offer) -[OFFERED_TO_SEGMENT]-> (Segment)

    OFFER_HAS_PRODUCT(from = Offer::class, to = Product::class),                        // (Offer) -[OFFER_HAS_PRODUCT]-> (Product)

    BELONG_TO_SEGMENT(from = Customer::class, to = Segment::class),                     // (Customer) -[BELONG_TO_SEGMENT]-> (Segment)

    EKYC_SCAN(from = Customer::class, to = ScanInformation::class),                     // (Customer) -[EKYC_SCAN]-> (ScanInformation)

    BELONG_TO_REGION(from = Customer::class, to = Region::class),                       // (Customer) -[BELONG_TO_REGION]-> (Region)

    SIM_PROFILE_FOR_REGION(from = SimProfile::class, to = Region::class),               // (SimProfile) -[SIM_PROFILE_FOR_REGION]-> (Region)

    SUBSCRIPTION_UNDER_SIM_PROFILE(from = Subscription::class, to = SimProfile::class), // (Subscription) -[SUBSCRIPTION_UNDER_SIM_PROFILE]-> (SimProfile)
}

class Neo4jStore : GraphStore by Neo4jStoreSingleton

object Neo4jStoreSingleton : GraphStore {

    private val logger by getLogger()
    private val scanInformationDatastore by lazy { getResource<ScanInformationStore>() }

    //
    // Entity
    //

    private val identityEntity = Identity::class.entityType
    private val identityStore = Identity::class.entityStore

    private val customerEntity = Customer::class.entityType
    private val customerStore = Customer::class.entityStore

    private val productEntity = Product::class.entityType
    private val productStore = Product::class.entityStore

    private val subscriptionEntity = Subscription::class.entityType
    private val subscriptionStore = Subscription::class.entityStore

    private val bundleEntity = Bundle::class.entityType
    private val bundleStore = Bundle::class.entityStore

    private val simProfileEntity = SimProfile::class.entityType
    private val simProfileStore = SimProfile::class.entityStore

    private val planEntity = Plan::class.entityType
    private val plansStore = Plan::class.entityStore

    private val regionEntity = Region::class.entityType
    private val regionStore = Region::class.entityStore

    private val scanInformationEntity = ScanInformation::class.entityType
    private val scanInformationStore = ScanInformation::class.entityStore

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

    private val simProfileRegionRelation = RelationType(
            relation = Relation.SIM_PROFILE_FOR_REGION,
            from = simProfileEntity,
            to = regionEntity,
            dataClass = None::class.java)
    private val simProfileRegionRelationStore = UniqueRelationStore(simProfileRegionRelation)

    private val subscriptionSimProfileRelation = RelationType(
            relation = Relation.SUBSCRIPTION_UNDER_SIM_PROFILE,
            from = subscriptionEntity,
            to = simProfileEntity,
            dataClass = None::class.java)
    private val subscriptionSimProfileRelationStore = UniqueRelationStore(subscriptionSimProfileRelation)

    // -------------
    // Client Store
    // -------------

    //
    // Identity
    //

    override fun getCustomerId(identity: org.ostelco.prime.model.Identity): Either<StoreError, String> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
    }

    private fun getCustomerId(identity: org.ostelco.prime.model.Identity, transaction: Transaction): Either<StoreError, String> =
            getCustomer(identity = identity, transaction = transaction)
                    .map { it.id }

    private fun getCustomerAndAnalyticsId(identity: org.ostelco.prime.model.Identity, transaction: Transaction): Either<StoreError, Pair<String, String>> =
            getCustomer(identity = identity, transaction = transaction)
                    .map { Pair(it.id, it.analyticsId) }

    //
    // Balance (Customer - Bundle)
    //

    override fun getBundles(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<Bundle>> = readTransaction {
        getCustomer(identity = identity, transaction = transaction)
                .flatMap {
                    customerStore.getRelated(it.id, customerToBundleRelation, transaction)
                }
    }

    override fun updateBundle(bundle: Bundle): Either<StoreError, Unit> = writeTransaction {
        update { bundle }.ifFailedThenRollback(transaction)
    }

    //
    // Customer
    //

    override fun getCustomer(identity: org.ostelco.prime.model.Identity): Either<StoreError, Customer> = readTransaction {
        getCustomer(identity = identity, transaction = transaction)
    }

    private fun getCustomer(identity: org.ostelco.prime.model.Identity, transaction: Transaction): Either<StoreError, Customer> =
            identityStore.getRelated(id = identity.id, relationType = identifiesRelation, transaction = transaction)
                    .flatMap {
                        if (it.isEmpty()) {
                            NotFoundError(type = identity.type, id = identity.id)
                                    .left()
                        } else {
                            it.single().right()
                        }
                    }

    private fun validateCreateCustomerParams(customer: Customer, referredBy: String?): Either<StoreError, Unit> =
            if (customer.referralId == referredBy) {
                ValidationError(type = customerEntity.name, id = customer.id, message = "Referred by self")
                        .left()
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
                create { Identity(id = identity.id, type = identity.type) }.bind()
                create { customer }.bind()
                identifiesRelationStore.create(fromId = identity.id, relation = Identifies(provider = identity.provider), toId = customer.id, transaction = transaction).bind()
                // Give 100 MB as free initial balance
                val productId = "2GB_FREE_ON_JOINING"
                val balance: Long = 2_147_483_648
                if (referredBy != null) {
                    referredRelationStore.create(referredBy, customer.id, transaction).bind()
                }
                create { Bundle(bundleId, balance) }.bind()
                val product = get(Product::class, productId).bind()
                createPurchaseRecordRelation(
                        customer.id,
                        PurchaseRecord(
                                id = UUID.randomUUID().toString(),
                                product = product,
                                timestamp = Instant.now().toEpochMilli()
                        ),
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
                    update {
                        existingCustomer.copy(
                                nickname = nickname ?: existingCustomer.nickname,
                                contactEmail = contactEmail ?: existingCustomer.contactEmail)
                    }
                }
                .ifFailedThenRollback(transaction)
    }

    // TODO vihang: Should we also delete SimProfile attached to this user?
    override fun removeCustomer(identity: org.ostelco.prime.model.Identity): Either<StoreError, Unit> = writeTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    identityStore.delete(id = identity.id, transaction = transaction)
                    customerStore.exists(customerId, transaction)
                            .flatMap {
                                customerStore.getRelated(customerId, customerToBundleRelation, transaction)
                                        .map { it.forEach { bundle -> bundleStore.delete(bundle.id, transaction) } }
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
                            ?: NotFoundError(type = customerRegionRelation.name, id = "$customerId -> $regionCode").left()
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
                        val cr = customerRegionRelation.createRelation(record["cr"].asMap())
                        val simProfiles = if (record["sp"].isNull) {
                            emptyList()
                        } else {
                            listOf(simProfileEntity.createEntity(record["sp"].asMap()))
                                    .mapNotNull { simProfile ->
                                        simManager.getSimProfile(
                                                hlr = getHlr(regionCode = region.id),
                                                iccId = simProfile.iccId)
                                                .map { simEntry ->
                                                    org.ostelco.prime.model.SimProfile(
                                                            iccId = simProfile.iccId,
                                                            status = simEntry.status,
                                                            eSimActivationCode = simEntry.eSimActivationCode,
                                                            alias = simProfile.alias)
                                                }
                                                .fold(
                                                        { error ->
                                                            logger.error("Failed to fetch SIM Profile: {} for region: {}. Reason: {}",
                                                                    simProfile.iccId,
                                                                    region.id,
                                                                    error)
                                                            null
                                                        },
                                                        { it })
                                    }
                        }
                        RegionDetails(
                                region = region,
                                status = cr.status,
                                kycStatusMap = cr.kycStatusMap,
                                simProfiles = simProfiles)
                    }
                    .requireNoNulls()
                    .groupBy { RegionDetails(region = it.region, status = it.status, kycStatusMap = it.kycStatusMap) }
                    .map { (key, value) ->
                        RegionDetails(
                                region = key.region,
                                status = key.status,
                                kycStatusMap = key.kycStatusMap,
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
            profileType: String?): Either<StoreError, org.ostelco.prime.model.SimProfile> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = getCustomerId(identity = identity, transaction = transaction).bind()
                val bundles = customerStore.getRelated(customerId, customerToBundleRelation, transaction).bind()
                validateBundleList(bundles, customerId).bind()
                val customer = get(Customer::class, customerId).bind()
                val status = customerRegionRelationStore
                        .get(fromId = customerId, toId = regionCode.toLowerCase(), transaction = transaction)
                        .bind()
                        .status
                isApproved(
                        status = status,
                        customerId = customerId,
                        regionCode = regionCode.toLowerCase()).bind()
                val region = get(Region::class, regionCode.toLowerCase()).bind()
                val simEntry = simManager.allocateNextEsimProfile(hlr = getHlr(region.id.toLowerCase()), phoneType = profileType)
                        .mapLeft { NotFoundError("eSIM profile", id = "Loltel") }
                        .bind()
                val simProfile = SimProfile(id = UUID.randomUUID().toString(), iccId = simEntry.iccId)
                create { simProfile }.bind()
                customerToSimProfileStore.create(
                        fromId = customerId,
                        toId = simProfile.id,
                        transaction = transaction).bind()
                simProfileRegionRelationStore.create(
                        fromId = simProfile.id,
                        toId = regionCode.toLowerCase(),
                        transaction = transaction).bind()
                simEntry.msisdnList.forEach { msisdn ->
                    create { Subscription(msisdn = msisdn) }.bind()
                    val subscription = get(Subscription::class, msisdn).bind()
                    bundles.forEach { bundle ->
                        subscriptionToBundleStore.create(
                                from = subscription,
                                relation = SubscriptionToBundle(),
                                to = bundle,
                                transaction = transaction).bind()
                    }
                    subscriptionRelationStore.create(customer, subscription, transaction).bind()
                    subscriptionSimProfileRelationStore.create(
                            fromId = msisdn,
                            toId = simProfile.id,
                            transaction = transaction).bind()
                    // TODO vihang: unlink Subscription from Region
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
                org.ostelco.prime.model.SimProfile(
                        iccId = simEntry.iccId,
                        alias = "",
                        eSimActivationCode = simEntry.eSimActivationCode,
                        status = simEntry.status)
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
                    id = "$customerId -> $regionCode",
                    message = "eKYC status is $status and not APPROVED.")
                    .left()
        } else {
            Unit.right()
        }
    }

    override fun getSimProfiles(
            identity: org.ostelco.prime.model.Identity,
            regionCode: String?): Either<StoreError, Collection<org.ostelco.prime.model.SimProfile>> {

        val map = mutableMapOf<String, String>()
        val simProfiles = readTransaction {
            IO {
                Either.monad<StoreError>().binding {

                    val customerId = getCustomerId(identity = identity, transaction = transaction).bind()
                    val simProfiles = customerStore.getRelated(
                            id = customerId,
                            relationType = customerToSimProfileRelation,
                            transaction = transaction).bind()
                    if (regionCode == null) {
                        simProfiles.forEach { simProfile ->
                            val region = simProfileStore.getRelated(
                                    id = simProfile.id,
                                    relationType = simProfileRegionRelation,
                                    transaction = transaction)
                                    .bind()
                                    .firstOrNull()
                            if (region != null) {
                                map[simProfile.id] = region.id
                            }
                        }
                    }
                    simProfiles
                }.fix()
            }.unsafeRunSync()
        }

        return IO {
            Either.monad<StoreError>()
                    .binding {
                        simProfiles.bind().map { simProfile ->
                            val regionId = (regionCode ?: map[simProfile.id])
                                    ?: ValidationError(type = simProfileEntity.name, id = simProfile.iccId, message = "SimProfile not linked to any region")
                                            .left()
                                            .bind()

                            val simEntry = simManager.getSimProfile(
                                    hlr = getHlr(regionId),
                                    iccId = simProfile.iccId)
                                    .mapLeft { NotFoundError(type = simProfileEntity.name, id = simProfile.iccId) }
                                    .bind()
                            org.ostelco.prime.model.SimProfile(
                                    iccId = simProfile.iccId,
                                    alias = simProfile.alias,
                                    eSimActivationCode = simEntry.eSimActivationCode,
                                    status = simEntry.status)
                        }
                    }.fix()
        }.unsafeRunSync()
    }

    private fun getHlr(regionCode: String): String {
        return "Loltel"
    }

    override fun updateSimProfile(
            identity: org.ostelco.prime.model.Identity,
            regionCode: String,
            iccId: String,
            alias: String): Either<StoreError, org.ostelco.prime.model.SimProfile> {
        val simProfileEither = writeTransaction {
            IO {
                Either.monad<StoreError>().binding {

                    val customerId = getCustomerId(identity = identity, transaction = transaction).bind()
                    val simProfile = customerStore.getRelated(
                            id = customerId,
                            relationType = customerToSimProfileRelation,
                            transaction = transaction)
                            .bind()
                            .firstOrNull { simProfile -> simProfile.iccId == iccId }
                            ?: NotFoundError(type = simProfileEntity.name, id = iccId).left().bind()

                    update { simProfile.copy(alias = alias) }.bind()

                    org.ostelco.prime.model.SimProfile(
                            iccId = simProfile.iccId,
                            alias = simProfile.alias,
                            eSimActivationCode = "",
                            status = NOT_READY)
                }.fix()
            }.unsafeRunSync().ifFailedThenRollback(transaction)
        }

        return IO {
            Either.monad<StoreError>().binding {
                val simProfile = simProfileEither.bind()
                val simEntry = simManager.getSimProfile(
                        hlr = getHlr(regionCode),
                        iccId = iccId)
                        .mapLeft { NotFoundError(type = simProfileEntity.name, id = simProfile.iccId) }
                        .bind()

                org.ostelco.prime.model.SimProfile(
                        iccId = simProfile.iccId,
                        alias = simProfile.alias,
                        eSimActivationCode = simEntry.eSimActivationCode,
                        status = simEntry.status)
            }.fix()
        }.unsafeRunSync()
    }

    override fun sendEmailWithActivationQrCode(
            identity: org.ostelco.prime.model.Identity,
            regionCode: String,
            iccId: String): Either<StoreError, org.ostelco.prime.model.SimProfile> {

        val infoEither = readTransaction {
            IO {
                Either.monad<StoreError>().binding {

                    val customer = getCustomer(identity = identity, transaction = transaction).bind()
                    val simProfile = customerStore.getRelated(
                            id = customer.id,
                            relationType = customerToSimProfileRelation,
                            transaction = transaction)
                            .bind()
                            .firstOrNull { simProfile -> simProfile.iccId == iccId }
                            ?: NotFoundError(type = simProfileEntity.name, id = iccId).left().bind()

                    Pair(customer, simProfile)
                }.fix()
            }.unsafeRunSync()
        }

        return IO {
            Either.monad<StoreError>().binding {
                val (customer, simProfile) = infoEither.bind()
                val simEntry = simManager.getSimProfile(
                        hlr = getHlr(regionCode),
                        iccId = iccId)
                        .mapLeft {
                            NotFoundError(type = simProfileEntity.name, id = simProfile.iccId)
                        }
                        .bind()

                emailNotifier.sendESimQrCodeEmail(
                        email = customer.contactEmail,
                        name = customer.nickname,
                        qrCode = simEntry.eSimActivationCode)
                        .mapLeft {
                            SystemError(type = "EMAIL", id = customer.contactEmail, message = "Failed to send email")
                        }
                        .bind()

                org.ostelco.prime.model.SimProfile(
                        iccId = simEntry.iccId,
                        eSimActivationCode = simEntry.eSimActivationCode,
                        status = simEntry.status,
                        alias = simProfile.alias)
            }.fix()
        }.unsafeRunSync()
    }

    override fun deleteSimProfileWithSubscription(regionCode: String, iccId: String): Either<StoreError, Unit> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val simProfiles = regionStore.getRelatedFrom(regionCode, simProfileRegionRelation, transaction).bind()
                simProfiles.forEach { simProfile ->
                    val subscriptions = simProfileStore.getRelatedFrom(simProfile.id, subscriptionSimProfileRelation, transaction).bind()
                    subscriptions.forEach { subscription ->
                        subscriptionStore.delete(subscription.id, transaction).bind()
                    }
                    simProfileStore.delete(simProfile.id, transaction).bind()
                }
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    //
    // Subscription
    //

    @Deprecated(message = "Use createSubscriptions instead")
    override fun addSubscription(
            identity: org.ostelco.prime.model.Identity,
            regionCode: String,
            iccId: String,
            alias: String,
            msisdn: String): Either<StoreError, Unit> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = getCustomerId(identity = identity, transaction = transaction).bind()

                val simProfile = SimProfile(
                        id = UUID.randomUUID().toString(),
                        iccId = iccId,
                        alias = alias)
                create { simProfile }.bind()
                simProfileRegionRelationStore.create(fromId = simProfile.id, toId = regionCode, transaction = transaction).bind()
                customerToSimProfileStore.create(fromId = customerId, toId = simProfile.id, transaction = transaction).bind()

                create { Subscription(msisdn) }.bind()
                subscriptionSimProfileRelationStore.create(fromId = msisdn, toId = simProfile.id, transaction = transaction).bind()
                subscriptionRelationStore.create(fromId = customerId, toId = msisdn, transaction = transaction).bind()

                val bundles = customerStore.getRelated(customerId, customerToBundleRelation, transaction).bind()
                validateBundleList(bundles, customerId).bind()
                bundles.forEach { bundle ->
                    subscriptionToBundleStore.create(fromId = msisdn, relation = SubscriptionToBundle(), toId = bundle.id, transaction = transaction).bind()
                }

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
                        -[:${subscriptionSimProfileRelation.name}]->(:${simProfileEntity.name})
                        -[:${simProfileRegionRelation.name}]->(:${regionEntity.name} {id: '${regionCode.toLowerCase()}'})
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

    override fun purchaseProduct(identity: org.ostelco.prime.model.Identity,
                                 sku: String,
                                 sourceId: String?,
                                 saveCard: Boolean): Either<PaymentError, ProductInfo> = writeTransaction {
        IO {
            Either.monad<PaymentError>().binding {

                val customer = getCustomer(identity = identity, transaction = transaction)
                        .mapLeft {
                            org.ostelco.prime.paymentprocessor.core.NotFoundError(
                                    "Failed to get customer data for customer with identity - $identity",
                                    error = it)
                        }.bind()

                val product = getProduct(identity, sku)
                        .mapLeft {
                            org.ostelco.prime.paymentprocessor.core.NotFoundError("Product $sku is unavailable",
                                    error = it)
                        }
                        .bind()

                if (product.price.amount > 0) {
                    val (chargeId, invoiceId) = when (product.paymentType) {
                        SUBSCRIPTION -> {
                            val subscriptionDetailsInfo = purchasePlan(
                                    customer = customer,
                                    sku = product.sku,
                                    sourceId = sourceId,
                                    saveCard = saveCard,
                                    taxRegionId = product.paymentTaxRegionId,
                                    transaction = transaction)
                                    .bind()
                            Pair(subscriptionDetailsInfo.chargeId, subscriptionDetailsInfo.invoiceId)
                        }
                        else -> {
                            val invoicePaymentInfo = oneTimePurchase(
                                    customer = customer,
                                    sourceId = sourceId,
                                    saveCard = saveCard,
                                    sku = product.sku,
                                    price = product.price,
                                    taxRegionId = product.paymentTaxRegionId,
                                    productLabel = product.paymentLabel,
                                    transaction = transaction)
                                    .bind()
                            Pair(invoicePaymentInfo.chargeId, invoicePaymentInfo.id)
                        }
                    }
                    val purchaseRecord = PurchaseRecord(
                            id = chargeId,
                            product = product,
                            timestamp = Instant.now().toEpochMilli())

                    /* If this step fails, the previously added 'removeInvoice' call added to the transaction
                    will ensure that the invoice will be voided. */
                    createPurchaseRecordRelation(customer.id, purchaseRecord, transaction)
                            .mapLeft {
                                logger.error("Failed to save purchase record for customer ${customer.id}, invoice-id $invoiceId, invoice will be voided in Stripe")
                                BadGatewayError("Failed to save purchase record",
                                        error = it)
                            }.bind()

                    /* TODO: While aborting transactions, send a record with "reverted" status. */
                    analyticsReporter.reportPurchaseInfo(
                            purchaseRecord = purchaseRecord,
                            customerAnalyticsId = customer.analyticsId,
                            status = "success")
                }

                when (product.productClass) {
                    MEMBERSHIP -> {
                        product.segmentIds.forEach { segmentId ->
                            assignCustomerToSegment(customerId = customer.id,
                                    segmentId = segmentId,
                                    transaction = transaction)
                                    .mapLeft {
                                        logger.error("Failed to assign Customer to a Segment - {}", it.message)
                                        BadGatewayError("Failed to assign Membership", error = it)
                                    }
                                    .bind()
                        }
                    }
                    SIMPLE_DATA -> {
                        /* Topup. */
                        simpleDataProduct(
                                customerId = customer.id,
                                sku = product.sku,
                                bytes = product.noOfBytes,
                                transaction = transaction)
                                .bind()
                    }
                    else -> {
                        BadGatewayError("Missing product class in properties of product: $sku").left().bind()
                    }
                }

                ProductInfo(product.sku)
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }
    // << END

    private fun fetchOrCreatePaymentProfile(customer: Customer): Either<PaymentError, ProfileInfo> =
            // Fetch/Create stripe payment profile for the customer.
            paymentProcessor.getPaymentProfile(customer.id)
                    .fold(
                            {
                                paymentProcessor.createPaymentProfile(customerId = customer.id,
                                        email = customer.contactEmail)
                            },
                            { profileInfo -> Either.right(profileInfo) }
                    )

    /* Note: 'purchase-relation' info is first added when a successful purchase
             event has been received from Stripe. */
    private fun purchasePlan(customer: Customer,
                             sku: String,
                             taxRegionId: String?,
                             sourceId: String?,
                             saveCard: Boolean,
                             transaction: PrimeTransaction): Either<PaymentError, SubscriptionDetailsInfo> {
        return IO {
            Either.monad<PaymentError>().binding {

                /* Bail out if subscriber tries to buy an already bought plan.
                   Note: Already verified above that 'customer' (subscriber) exists. */
                customerStore.getRelated(customer.id, purchaseRecordRelation, transaction)
                        .map { products ->
                            if (products.any { x -> x.sku == sku }) {
                                PlanAlredyPurchasedError("A subscription to plan $sku already exists")
                                        .left().bind()
                            }
                        }

                /* A source must be associated with a payment profile with the payment vendor.
                   Create the profile if it don't exists. */
                fetchOrCreatePaymentProfile(customer)
                        .bind()

                /* With recurring payments, the payment card (source) must be stored. The
                   'saveCard' parameter is therefore ignored. */
                if (!saveCard) {
                    logger.warn("Ignoring request for deleting payment source after buying plan $sku for " +
                            "customer ${customer.id} as stored payment source is required when purchasing a plan")
                }

                if (sourceId != null) {
                    val sourceDetails = paymentProcessor.getSavedSources(customer.id)
                            .mapLeft {
                                BadGatewayError("Failed to fetch sources for customer: ${customer.id}",
                                        error = it)
                            }.bind()
                    if (!sourceDetails.any { sourceDetailsInfo -> sourceDetailsInfo.id == sourceId }) {
                        paymentProcessor.addSource(customer.id, sourceId)
                                .bind().id
                    }
                }

                subscribeToPlan(
                        customerId = customer.id,
                        planId = sku,
                        taxRegionId = taxRegionId,
                        transaction = transaction)
                        .mapLeft {
                            BadGatewayError("Failed to subscribe ${customer.id} to plan $sku",
                                    error = it)
                        }
                        .bind()
            }.fix()
        }.unsafeRunSync()
    }

    private fun subscribeToPlan(
            customerId: String,
            planId: String,
            taxRegionId: String?,
            trialEnd: Long = 0L,
            transaction: PrimeTransaction): Either<StoreError, SubscriptionDetailsInfo> {

        return IO {
            Either.monad<StoreError>().binding {
                val plan = plansStore.get(planId, transaction)
                        .bind()
                val profileInfo = paymentProcessor.getPaymentProfile(customerId)
                        .mapLeft {
                            NotFoundError(type = planEntity.name, id = "Failed to subscribe $customerId to ${plan.id}",
                                    error = it)
                        }.bind()

                /* At this point, we have either:
                     1) A new subscription to a plan is being created.
                     2) An attempt at buying a previously subscribed to plan but which has not been
                        paid for.
                   Both are OK. But in order to handle the second case correctly, the previous incomplete
                   subscription must be removed before we can proceed with creating the new subscription.

                   (In the second case there will be a "SUBSCRIBES_TO_PLAN" link between the customer
                   object and the plan object, but no "PURCHASED" link to the plans "product" object.)

                   The motivation for supporting the second case, is that it allows the subscriber to
                   reattempt to buy a plan using a different payment source.

                   Remove existing incomplete subscription if any. */
                customerStore.getRelated(customerId, subscribesToPlanRelation, transaction)
                        .map {
                            if (it.any { x -> x.id == planId }) {
                                removeSubscription(customerId, planId, invoiceNow = true)
                            }
                        }

                /* Lookup in payment backend will fail if no value found for 'planId'. */
                val planStripeId = plan.stripePlanId ?: SystemError(type = "", id = "", message = "")
                        .left()
                        .bind()

                val subscriptionDetailsInfo = paymentProcessor.createSubscription(
                        planId = planStripeId,
                        stripeCustomerId = profileInfo.id,
                        trialEnd = trialEnd,
                        taxRegionId = taxRegionId)
                        .mapLeft {
                            NotCreatedError(type = planEntity.name, id = "Failed to subscribe $customerId to ${plan.id}",
                                    error = it)
                        }.linkReversalActionToTransaction(transaction) {
                            paymentProcessor.cancelSubscription(it.id)
                        }.bind()

                /* Dispatch according to the charge result. */
                when (subscriptionDetailsInfo.status) {
                    PaymentStatus.PAYMENT_SUCCEEDED -> {
                    }
                    PaymentStatus.REQUIRES_PAYMENT_METHOD -> {
                        NotCreatedError(type = planEntity.name, id = "Failed to subscribe $customerId to ${plan.id}",
                                error = ForbiddenError("Payment method failed"))
                                .left().bind()
                    }
                    PaymentStatus.REQUIRES_ACTION,
                    PaymentStatus.TRIAL_START -> {
                        /* No action required. Charge for the subscription will eventually
                           be reported as a Stripe event. */
                        logger.info(
                                "Pending payment for subscription $planId for customer $customerId (${subscriptionDetailsInfo.status.name})")
                    }
                }

                /* Store information from payment backend for later use. */
                subscribesToPlanRelationStore.create(
                        fromId = customerId,
                        relation = PlanSubscription(
                                subscriptionId = subscriptionDetailsInfo.id,
                                created = subscriptionDetailsInfo.created,
                                trialEnd = subscriptionDetailsInfo.trialEnd),
                        toId = planId,
                        transaction = transaction)
                        .flatMap {
                            Either.right(plan)
                        }.bind()

                subscriptionDetailsInfo
            }.fix()
        }.unsafeRunSync()
    }

    private fun oneTimePurchase(
            customer: Customer,
            sourceId: String?,
            saveCard: Boolean,
            sku: String,
            price: Price,
            productLabel: String,
            taxRegionId: String?,
            transaction: PrimeTransaction): Either<PaymentError, InvoicePaymentInfo> = IO {

        Either.monad<PaymentError>().binding {

            /* A source must be associated with a payment profile with the payment vendor.
               Create the profile if it don't exists. */
            fetchOrCreatePaymentProfile(customer)
                    .bind()

            var addedSourceId: String? = null

            /* Add source if set and if it has not already been added to the payment profile. */
            if (sourceId != null) {
                val sourceDetails = paymentProcessor.getSavedSources(customer.id)
                        .mapLeft {
                            BadGatewayError("Failed to fetch sources for user", error = it)
                        }.bind()
                addedSourceId = sourceId

                if (!sourceDetails.any { sourceDetailsInfo -> sourceDetailsInfo.id == sourceId }) {
                    addedSourceId = paymentProcessor.addSource(customer.id, sourceId)
                            /* For the success case, saved source is removed after the invoice has been
                               paid if 'saveCard == false'. Make sure same happens even for failure
                               case by linking reversal action to transaction */
                            .finallyDo(transaction) {
                                removePaymentSource(saveCard, customer.id, it.id)
                            }.bind().id
                }
            }

            val invoice = paymentProcessor.createInvoice(
                    customerId = customer.id,
                    amount = price.amount,
                    currency = price.currency,
                    description = productLabel,
                    taxRegionId = taxRegionId,
                    sourceId = addedSourceId)
                    .mapLeft {
                        logger.error("Failed to create invoice for customer ${customer.id}, source $addedSourceId, sku $sku")
                        it
                    }.linkReversalActionToTransaction(transaction) {
                        paymentProcessor.removeInvoice(it.id)
                        logger.error(NOTIFY_OPS_MARKER,
                                """Failed to create or pay invoice for customer ${customer.id}, invoice-id: ${it.id}.
                                   Verify that the invoice has been deleted or voided in Stripe dashboard.
                                """.trimIndent())
                    }.bind()

            /* Force immediate payment of the invoice. */
            val invoicePaymentInfo = paymentProcessor.payInvoice(invoice.id)
                    .mapLeft {
                        logger.error("Payment of invoice ${invoice.id} failed for customer ${customer.id}.")
                        it
                    }.linkReversalActionToTransaction(transaction) {
                        paymentProcessor.refundCharge(it.chargeId)
                        logger.error(NOTIFY_OPS_MARKER,
                                """Refunded customer ${customer.id} for invoice: ${it.id}.
                                   Verify that the invoice has been refunded in Stripe dashboard.
                                """.trimIndent())
                    }.bind()

            invoicePaymentInfo
        }.fix()
    }.unsafeRunSync()

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

    private fun simpleDataProduct(
            customerId: String,
            sku: String,
            bytes: Long,
            transaction: PrimeTransaction): Either<PaymentError, Unit> = IO {

        Either.monad<PaymentError>().binding {

            if (bytes == 0L) {
                logger.error("Product with 0 bytes: sku = {}", sku)
            } else {
                /* Update balance with bought data. */
                /* TODO: Add rollback in case of errors later on. */
                write("""MATCH (cr:${customerEntity.name} { id:'$customerId' })-[:${customerToBundleRelation.name}]->(bundle:${bundleEntity.name})
                     SET bundle.balance = toString(toInteger(bundle.balance) + $bytes)
                  """.trimIndent(), transaction) {
                    Either.cond(
                            test = it.summary().counters().containsUpdates(),
                            ifTrue = {},
                            ifFalse = {
                                logger.error("Failed to update balance during purchase for customer: {}", customerId)
                                BadGatewayError(
                                        description = "Failed to update balance during purchase for customer: $customerId",
                                        message = "Failed to perform topup")
                            })
                }.bind()
            }

            Unit
        }.fix()
    }.unsafeRunSync()

    //
    // Purchase Records
    //

    override fun getPurchaseRecords(identity: org.ostelco.prime.model.Identity): Either<StoreError, Collection<PurchaseRecord>> =
            readTransaction {
                getCustomerId(identity = identity, transaction = transaction)
                        .flatMap { customerId ->
                            customerStore.getRelations(customerId, purchaseRecordRelation, transaction)
                        }
            }

    override fun addPurchaseRecord(customerId: String, purchase: PurchaseRecord): Either<StoreError, String> =
            writeTransaction {
                createPurchaseRecordRelation(customerId, purchase, transaction)
                        .ifFailedThenRollback(transaction)
            }

    private fun createPurchaseRecordRelation(customerId: String,
                                             purchase: PurchaseRecord,
                                             transaction: Transaction): Either<StoreError, String> {
        val invoiceId = if (purchase.properties.containsKey("invoiceId") && !purchase.properties["invoiceId"].isNullOrEmpty())
            purchase.properties["invoiceId"]
        else
            null

        /* Avoid charging for the same invoice twice if invoice information
           is present. */
        return if (invoiceId != null) {
            getPurchaseRecordUsingInvoiceId(customerId, invoiceId, transaction)
                    .fold({
                        customerStore.get(id = customerId, transaction = transaction).flatMap { customer ->
                            productStore.get(purchase.product.sku, transaction).flatMap { product ->
                                purchaseRecordRelationStore.create(customer, purchase, product, transaction)
                                        .map { purchase.id }
                            }
                        }
                    }, {
                        ValidationError(type = purchaseRecordRelation.name,
                                id = purchase.id,
                                message = "A purchase record for ${purchase.product} for customer ${customerId} alread exists")
                                .left()
                    })
        } else {
            customerStore.get(id = customerId, transaction = transaction).flatMap { customer ->
                productStore.get(purchase.product.sku, transaction).flatMap { product ->
                    purchaseRecordRelationStore.create(customer, purchase, product, transaction)
                            .map { purchase.id }
                }
            }
        }
    }

    /* As Stripes invoice-id is used as the 'id' of a purchase record, this method
       allows for detecting double charges etc. */
    private fun getPurchaseRecordUsingInvoiceId(customerId: String,
                                                invoiceId: String,
                                                transaction: Transaction): Either<StoreError, PurchaseRecord> =
            customerStore.getRelations(customerId, purchaseRecordRelation, transaction)
                    .map { records ->
                        records.find {
                            it.properties.containsKey("invoiceId") && it.properties["invoiceId"] == invoiceId
                        }
                    }.leftIfNull { NotFoundError(type = purchaseRecordRelation.name, id = invoiceId) }

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

    internal fun createCustomerRegionSetting(
            customerId: String,
            status: CustomerRegionStatus,
            regionCode: String): Either<StoreError, Unit> = writeTransaction {

        createCustomerRegionSetting(
                customerId = customerId,
                status = status,
                regionCode = regionCode,
                transaction = transaction)
    }

    private fun createCustomerRegionSetting(
            customerId: String,
            status: CustomerRegionStatus,
            regionCode: String,
            transaction: PrimeTransaction): Either<StoreError, Unit> =

            customerRegionRelationStore
                    .createIfAbsent(
                            fromId = customerId,
                            relation = CustomerRegion(
                                    status = status,
                                    kycStatusMap = getKycStatusMapForRegion(regionCode)),
                            toId = regionCode,
                            transaction = transaction)
                    .flatMap {
                        if (status == APPROVED) {
                            assignCustomerToSegment(
                                    customerId = customerId,
                                    segmentId = getInitialSegmentNameForRegion(regionCode, transaction),
                                    transaction = transaction)
                        } else {
                            Unit.right()
                        }
                    }

    private fun assignCustomerToSegment(
            customerId: String,
            segmentId: String,
            transaction: Transaction): Either<StoreError, Unit> =
            customerToSegmentStore.create(
                    fromId = customerId,
                    toId = segmentId,
                    transaction = transaction).mapLeft { storeError ->
                if (storeError is NotCreatedError && storeError.type == customerToSegmentRelation.name) {
                    ValidationError(type = customerEntity.name, id = customerId, message = "Unsupported segment: $segmentId")
                } else {
                    storeError
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
                    createCustomerRegionSetting(
                            customerId = customerId, status = PENDING, regionCode = regionCode.toLowerCase(), transaction = transaction)
                            .flatMap {
                                create { newScan }
                            }
                            .flatMap {
                                scanInformationRelationStore.createIfAbsent(customerId, newScan.id, transaction)
                            }
                            .flatMap {
                                setKycStatus(
                                        customerId = customerId,
                                        regionCode = regionCode.toLowerCase(),
                                        kycType = JUMIO,
                                        kycStatus = KycStatus.PENDING,
                                        transaction = transaction)
                            }
                            .flatMap {
                                newScan.right()
                            }
                }
                .ifFailedThenRollback(transaction)
    }

    private fun getCustomerUsingScanId(scanId: String, transaction: Transaction): Either<StoreError, Customer> {
        return scanInformationStore
                .getRelatedFrom(
                        id = scanId,
                        relationType = scanInformationRelation,
                        transaction = transaction)
                .flatMap { customers ->
                    customers.singleOrNull()?.right()
                            ?: NotFoundError(type = scanInformationEntity.name, id = scanId).left()
                }
    }

    override fun getCountryCodeForScan(scanId: String): Either<StoreError, String> = readTransaction {
        get(ScanInformation::class, scanId)
                .flatMap { scanInformation ->
                    scanInformation.countryCode.right()
                }
    }

    // TODO merge into a single query which will use customerId and scanId
    override fun getScanInformation(identity: org.ostelco.prime.model.Identity, scanId: String): Either<StoreError, ScanInformation> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    get(ScanInformation::class, scanId).flatMap { scanInformation ->
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

    private val appNotifier by lazy { getResource<AppNotifier>() }

    override fun updateScanInformation(scanInformation: ScanInformation, vendorData: MultivaluedMap<String, String>): Either<StoreError, Unit> = writeTransaction {
        logger.info("updateScanInformation : ${scanInformation.scanId} status: ${scanInformation.status}")
        getCustomerUsingScanId(scanInformation.scanId, transaction).flatMap { customer ->
            update { scanInformation }.flatMap {
                logger.info("updating scan Information for : ${customer.contactEmail} id: ${scanInformation.scanId} status: ${scanInformation.status}")
                val extendedStatus = scanInformationDatastore.getExtendedStatusInformation(scanInformation)
                if (scanInformation.status == ScanStatus.APPROVED) {

                    logger.info("Inserting scan Information to cloud storage : id: ${scanInformation.scanId} countryCode: ${scanInformation.countryCode}")
                    scanInformationDatastore.upsertVendorScanInformation(customer.id, scanInformation.countryCode, vendorData)
                            .flatMap {
                                appNotifier.notify(
                                        customerId = customer.id,
                                        title = FCMStrings.NOTIFICATION_TITLE.s,
                                        body = FCMStrings.JUMIO_IDENTITY_VERIFIED.s,
                                        data = extendedStatus
                                )
                                logger.info(NOTIFY_OPS_MARKER, "Jumio verification succeeded for ${customer.contactEmail} Info: ${extendedStatus}")
                                setKycStatus(
                                        customerId = customer.id,
                                        regionCode = scanInformation.countryCode.toLowerCase(),
                                        kycType = JUMIO,
                                        transaction = transaction)
                            }
                } else {
                    // TODO: find out what more information can be passed to the client.
                    appNotifier.notify(
                            customerId = customer.id,
                            title = FCMStrings.NOTIFICATION_TITLE.s,
                            body = FCMStrings.JUMIO_IDENTITY_FAILED.s,
                            data = extendedStatus
                    )
                    logger.warn(NOTIFY_OPS_MARKER, "Jumio verification failed for ${customer.contactEmail} Info: ${extendedStatus}")
                    setKycStatus(
                            customerId = customer.id,
                            regionCode = scanInformation.countryCode.toLowerCase(),
                            kycType = JUMIO,
                            kycStatus = REJECTED,
                            transaction = transaction)
                }
            }
        }.ifFailedThenRollback(transaction)
    }

    //
    // eKYC - MyInfo
    //

    private val myInfoKycService by lazy { getResource<MyInfoKycService>() }

    private val secureArchiveService by lazy { getResource<SecureArchiveService>() }

    override fun getCustomerMyInfoData(
            identity: org.ostelco.prime.model.Identity,
            authorisationCode: String): Either<StoreError, String> {
        return IO {
            Either.monad<StoreError>().binding {

                val customerId = getCustomerId(identity = identity).bind()

                // set MY_INFO KYC Status to Pending
                setKycStatus(
                        customerId = customerId,
                        regionCode = "sg",
                        kycType = MY_INFO,
                        kycStatus = KycStatus.PENDING).bind()

                val personData = try {
                    myInfoKycService.getPersonData(authorisationCode).right()
                } catch (e: Exception) {
                    logger.error("Failed to fetched MyInfo using authCode = $authorisationCode", e)
                    SystemError(
                            type = "MyInfo Auth Code",
                            id = authorisationCode,
                            message = "Failed to fetched MyInfo").left()
                }.bind()

                secureArchiveService.archiveEncrypted(
                        customerId = customerId,
                        fileName = "myInfoData",
                        regionCodes = listOf("sg"),
                        dataMap = mapOf("personData" to personData.toByteArray())
                ).bind()

                // set MY_INFO KYC Status to Approved
                setKycStatus(
                        customerId = customerId,
                        regionCode = "sg",
                        kycType = MY_INFO).bind()

                personData
            }.fix()
        }.unsafeRunSync()
    }

    //
    // eKYC - NRIC/FIN
    //

    private val daveKycService by lazy { getResource<DaveKycService>() }

    override fun checkNricFinIdUsingDave(
            identity: org.ostelco.prime.model.Identity,
            nricFinId: String): Either<StoreError, Unit> {

        return IO {
            Either.monad<StoreError>().binding {

                logger.info("checkNricFinIdUsingDave for $nricFinId")

                val customerId = getCustomerId(identity = identity).bind()

                // set NRIC_FIN KYC Status to Pending
                setKycStatus(
                        customerId = customerId,
                        regionCode = "sg",
                        kycType = NRIC_FIN,
                        kycStatus = KycStatus.PENDING).bind()

                if (daveKycService.validate(nricFinId)) {
                    logger.info("checkNricFinIdUsingDave validated $nricFinId")
                } else {
                    logger.info("checkNricFinIdUsingDave failed to validate $nricFinId")
                    ValidationError(type = "NRIC/FIN ID", id = nricFinId, message = "Invalid NRIC/FIN ID").left().bind()
                }

                secureArchiveService.archiveEncrypted(
                        customerId = customerId,
                        fileName = "nricFin",
                        regionCodes = listOf("sg"),
                        dataMap = mapOf("nricFinId" to nricFinId.toByteArray())
                ).bind()

                // set NRIC_FIN KYC Status to Approved
                setKycStatus(
                        customerId = customerId,
                        regionCode = "sg",
                        kycType = NRIC_FIN).bind()
            }.fix()
        }.unsafeRunSync()
    }

    //
    // eKYC - Address and Phone number
    //
    override fun saveAddressAndPhoneNumber(
            identity: org.ostelco.prime.model.Identity,
            address: String,
            phoneNumber: String): Either<StoreError, Unit> {

        return IO {
            Either.monad<StoreError>().binding {

                val customerId = getCustomerId(identity = identity).bind()

                // set ADDRESS_AND_PHONE_NUMBER KYC Status to Pending
                setKycStatus(
                        customerId = customerId,
                        regionCode = "sg",
                        kycType = ADDRESS_AND_PHONE_NUMBER,
                        kycStatus = KycStatus.PENDING).bind()

                secureArchiveService.archiveEncrypted(
                        customerId = customerId,
                        fileName = "addressAndPhoneNumber",
                        regionCodes = listOf("sg"),
                        dataMap = mapOf(
                                "address" to address.toByteArray(),
                                "phoneNumber" to phoneNumber.toByteArray())
                ).bind()

                // set ADDRESS_AND_PHONE_NUMBER KYC Status to Approved
                setKycStatus(
                        customerId = customerId,
                        regionCode = "sg",
                        kycType = ADDRESS_AND_PHONE_NUMBER).bind()
            }.fix()
        }.unsafeRunSync()
    }

    //
    // eKYC - Status Flags
    //

    internal fun setKycStatus(
            customerId: String,
            regionCode: String,
            kycType: KycType,
            kycStatus: KycStatus = KycStatus.APPROVED) = writeTransaction {

        setKycStatus(
                customerId = customerId,
                regionCode = regionCode,
                kycType = kycType,
                kycStatus = kycStatus,
                transaction = transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun setKycStatus(
            customerId: String,
            regionCode: String,
            kycType: KycType,
            kycStatus: KycStatus = KycStatus.APPROVED,
            transaction: Transaction): Either<StoreError, Unit> {

        return IO {
            Either.monad<StoreError>().binding {

                val approvedKycTypeSetList = getApprovedKycTypeSetList(regionCode)

                val existingCustomerRegion = customerRegionRelationStore.get(
                        fromId = customerId,
                        toId = regionCode,
                        transaction = transaction)
                        .getOrElse { CustomerRegion(status = PENDING, kycStatusMap = getKycStatusMapForRegion(regionCode)) }

                val newKycStatusMap = existingCustomerRegion.kycStatusMap.copy(key = kycType, value = kycStatus)

                val approved = approvedKycTypeSetList.any { kycTypeSet ->
                    newKycStatusMap.filter { it.value == KycStatus.APPROVED }.keys.containsAll(kycTypeSet)
                }

                val approvedNow = existingCustomerRegion.status == PENDING && approved

                val newStatus = if (approved) {
                    APPROVED
                } else {
                    existingCustomerRegion.status
                }

                if (approvedNow) {
                    assignCustomerToSegment(
                            customerId = customerId,
                            segmentId = getInitialSegmentNameForRegion(regionCode, transaction),
                            transaction = transaction).bind()
                }

                customerRegionRelationStore
                        .createOrUpdate(
                                fromId = customerId,
                                relation = CustomerRegion(status = newStatus, kycStatusMap = newKycStatusMap),
                                toId = regionCode,
                                transaction = transaction)
                        .bind()

            }.fix()
        }.unsafeRunSync()
    }

    private fun getKycStatusMapForRegion(regionCode: String): Map<KycType, KycStatus> {
        return when (regionCode) {
            "sg" -> setOf(JUMIO, MY_INFO, NRIC_FIN, ADDRESS_AND_PHONE_NUMBER)
            else -> setOf(JUMIO)
        }.map { it to KycStatus.PENDING }.toMap()
    }

    private fun getApprovedKycTypeSetList(regionCode: String): List<Set<KycType>> {
        return when (regionCode) {
            "sg" -> listOf(setOf(MY_INFO),
                    setOf(JUMIO, NRIC_FIN, ADDRESS_AND_PHONE_NUMBER))
            else -> listOf(setOf(JUMIO))
        }
    }

    private fun getInitialSegmentNameForRegion(regionCode: String, transaction: Transaction): String =
            segmentStore.get(getPlanSegmentNameFromCountryCode(regionCode), transaction)
                    .fold(
                            {
                                getSegmentNameFromCountryCode(regionCode)
                            },
                            {
                                it.id
                            }
                    )

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
        get(Plan::class, planId)
    }

    override fun getPlans(identity: org.ostelco.prime.model.Identity): Either<StoreError, List<Plan>> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap { customerId ->
                    customerStore.getRelated(id = customerId, relationType = subscribesToPlanRelation, transaction = transaction)
                }
    }

    override fun createPlan(
            plan: Plan,
            stripeProductName: String,
            planProduct: Product): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {

                get(Product::class, plan.id)
                        .fold(
                                { Unit.right() },
                                {
                                    Either.left(AlreadyExistsError(type = productEntity.name, id = "Failed to find product associated with plan ${plan.id}"))
                                }
                        ).bind()
                get(Plan::class, plan.id)
                        .fold(
                                { Unit.right() },
                                {
                                    Either.left(AlreadyExistsError(type = planEntity.name, id = "Failed to find plan ${plan.id}"))
                                }
                        ).bind()

                val productInfo = paymentProcessor.createProduct(stripeProductName)
                        .mapLeft {
                            NotCreatedError(type = planEntity.name, id = "Failed to create plan ${plan.id}",
                                    error = it)
                        }.linkReversalActionToTransaction(transaction) {
                            paymentProcessor.removeProduct(it.id)
                        }.bind()
                val planInfo = paymentProcessor.createPlan(
                        productInfo.id,
                        planProduct.price.amount,
                        planProduct.price.currency,
                        PaymentProcessor.Interval.valueOf(plan.interval.toUpperCase()), plan.intervalCount)
                        .mapLeft {
                            NotCreatedError(type = planEntity.name, id = "Failed to create plan ${plan.id}",
                                    error = it)
                        }.linkReversalActionToTransaction(transaction) {
                            paymentProcessor.removePlan(it.id)
                        }.bind()

                /* The associated product to the plan. Note that:
                         sku - name of the plan
                         property value 'productClass' is set to "plan"
                   TODO: Update to new backend model. */
                val product = planProduct.copy(
                        payment = planProduct.payment + mapOf(
                                "type" to SUBSCRIPTION.name)
                )

                /* Propagates errors from lower layer if any. */
                create { product }.bind()
                create {
                    plan.copy(
                            stripePlanId = planInfo.id,
                            stripeProductId = productInfo.id)
                }.bind()
                get(Plan::class, plan.id)
                        .bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun deletePlan(planId: String): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val plan = get(Plan::class, planId)
                        .bind()
                /* The name of the product is the same as the name of the corresponding plan. */
                get(Product::class, planId)
                        .bind()

                /* Not removing the product due to purchase references. */

                /* Removing the plan will remove the plan itself and all relations going to it. */
                plansStore.delete(plan.id, transaction)
                        .bind()

                /* Lookup in payment backend will fail if no value found for 'planId'. */
                plan.stripePlanId?.let { stripePlanId ->
                    paymentProcessor.removePlan(stripePlanId)
                            .mapLeft {
                                NotDeletedError(type = planEntity.name, id = "Failed to delete ${plan.id}",
                                        error = it)
                            }.bind()
                }

                /* Lookup in payment backend will fail if no value found for 'productId'. */
                plan.stripeProductId?.let { stripeProductId ->
                    paymentProcessor.removeProduct(stripeProductId)
                            .mapLeft {
                                NotDeletedError(type = planEntity.name, id = "Failed to delete ${plan.id}",
                                        error = it)
                            }.bind()
                }
                plan
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun subscribeToPlan(
            identity: org.ostelco.prime.model.Identity,
            planId: String,
            trialEnd: Long): Either<StoreError, Unit> = writeTransaction {

        IO {
            Either.monad<StoreError>().binding {

                val customer = getCustomer(identity = identity, transaction = transaction)
                        .bind()

                val product = getProduct(identity, planId)
                        .bind()

                subscribeToPlan(
                        customerId = customer.id,
                        planId = planId,
                        taxRegionId = product.paymentTaxRegionId,
                        transaction = transaction)
                        .bind()

                Unit
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun unsubscribeFromPlan(identity: org.ostelco.prime.model.Identity, planId: String, invoiceNow: Boolean): Either<StoreError, Plan> = readTransaction {
        getCustomerId(identity = identity, transaction = transaction)
                .flatMap {
                    removeSubscription(it, planId, invoiceNow)
                }
    }

    private fun removeSubscription(customerId: String, planId: String, invoiceNow: Boolean): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val plan = get(Plan::class, planId)
                        .bind()
                val planSubscription = subscribesToPlanRelationStore.get(customerId, planId, transaction)
                        .bind()
                paymentProcessor.cancelSubscription(planSubscription.subscriptionId, invoiceNow)
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

    override fun purchasedSubscription(
            customerId: String,
            invoiceId: String,
            chargeId: String,
            sku: String,
            amount: Long,
            currency: String): Either<StoreError, Plan> = readTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val product = get(Product::class, sku).bind()
                val plan = get(Plan::class, sku).bind()
                val purchaseRecord = PurchaseRecord(
                        id = chargeId,
                        product = product,
                        timestamp = Instant.now().toEpochMilli(),
                        properties = mapOf("invoiceId" to invoiceId)
                )

                createPurchaseRecordRelation(customerId, purchaseRecord, transaction)
                        .bind()

                /* Offer products to the newly signed up subscriber. */
                product.segmentIds.forEach { segmentId ->
                    assignCustomerToSegment(
                            customerId = customerId,
                            segmentId = segmentId,
                            transaction = transaction)
                            .bind()
                }
                logger.info("Customer $customerId completed payment of invoice $invoiceId for subscription to plan ${plan.id}")

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
                checkPurchaseRecordForRefund(purchaseRecord)
                        .bind()
                val refundId = paymentProcessor.refundCharge(
                        purchaseRecord.id,
                        purchaseRecord.product.price.amount)
                        .bind()
                val refund = RefundRecord(refundId, reason, Instant.now().toEpochMilli())
                val changedPurchaseRecord = purchaseRecord.copy(
                        refund = refund
                )
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

    private val offerEntity = Offer::class.entityType
    private val offerStore = Offer::class.entityStore

    private val segmentEntity = Segment::class.entityType
    private val segmentStore = Segment::class.entityStore

    private val offerToSegmentRelation = RelationType(OFFERED_TO_SEGMENT, offerEntity, segmentEntity, None::class.java)
    private val offerToSegmentStore = RelationStore(offerToSegmentRelation)

    private val offerToProductRelation = RelationType(OFFER_HAS_PRODUCT, offerEntity, productEntity, None::class.java)
    private val offerToProductStore = RelationStore(offerToProductRelation)

    private val customerToSegmentRelation = RelationType(BELONG_TO_SEGMENT, customerEntity, segmentEntity, None::class.java)
    private val customerToSegmentStore = RelationStore(customerToSegmentRelation)

    //
    // Segment
    //
    override fun createSegment(segment: org.ostelco.prime.model.Segment): Either<StoreError, Unit> = writeTransaction {
        createSegment(segment, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun createSegment(segment: org.ostelco.prime.model.Segment, transaction: Transaction): Either<StoreError, Unit> {
        return segmentStore.create(Segment(id = segment.id), transaction)
                .flatMap { customerToSegmentStore.create(segment.subscribers, segment.id, transaction) }
    }

    override fun updateSegment(segment: org.ostelco.prime.model.Segment): Either<StoreError, Unit> = writeTransaction {
        updateSegment(segment, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun updateSegment(segment: org.ostelco.prime.model.Segment, transaction: Transaction): Either<StoreError, Unit> {
        return customerToSegmentStore.removeAll(toId = segment.id, transaction = transaction)
                .flatMap { customerToSegmentStore.create(segment.subscribers, segment.id, transaction) }
    }

    //
    // Offer
    //
    override fun createOffer(offer: org.ostelco.prime.model.Offer): Either<StoreError, Unit> = writeTransaction {
        createOffer(offer, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun createOffer(offer: org.ostelco.prime.model.Offer, transaction: Transaction): Either<StoreError, Unit> {
        return offerStore
                .create(Offer(id = offer.id), transaction)
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
            offer: org.ostelco.prime.model.Offer,
            segments: Collection<org.ostelco.prime.model.Segment>,
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

        val actualOffer = org.ostelco.prime.model.Offer(
                id = offer.id,
                products = productIds,
                segments = segmentIds)

        IO {
            Either.monad<StoreError>().binding {

                products.forEach { product -> create { product }.bind() }
                segments.forEach { segment -> create { segment }.bind() }
                createOffer(actualOffer, transaction).bind()

            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    /**
     * Create Segments
     */
    override fun atomicCreateSegments(createSegments: Collection<org.ostelco.prime.model.Segment>): Either<StoreError, Unit> = writeTransaction {

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
    override fun atomicUpdateSegments(updateSegments: Collection<org.ostelco.prime.model.Segment>): Either<StoreError, Unit> = writeTransaction {

        updateSegments.fold(
                initial = Unit.right() as Either<StoreError, Unit>,
                operation = { acc, segment ->
                    acc.flatMap { updateSegment(segment, transaction) }
                })
                .ifFailedThenRollback(transaction)
    }

    override fun atomicAddToSegments(addToSegments: Collection<org.ostelco.prime.model.Segment>): Either<StoreError, Unit> {
        TODO()
    }

    override fun atomicRemoveFromSegments(removeFromSegments: Collection<org.ostelco.prime.model.Segment>): Either<StoreError, Unit> {
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

fun <K, V> Map<K, V>.copy(key: K, value: V): Map<K, V> {
    val mutableMap = this.toMutableMap()
    mutableMap[key] = value
    return mutableMap.toMap()
}
