package org.ostelco.prime.storage.graph

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherOf
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.leftIfNull
import arrow.core.right
import arrow.effects.IO
import arrow.instances.either.monad.monad
import org.neo4j.driver.v1.Transaction
import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.appnotifier.NotificationType
import org.ostelco.prime.auditlog.AuditLog
import org.ostelco.prime.dsl.ReadTransaction
import org.ostelco.prime.dsl.WriteTransaction
import org.ostelco.prime.dsl.forCustomer
import org.ostelco.prime.dsl.forPurchaseBy
import org.ostelco.prime.dsl.identifiedBy
import org.ostelco.prime.dsl.linkedToCustomer
import org.ostelco.prime.dsl.linkedToRegion
import org.ostelco.prime.dsl.linkedToSimProfile
import org.ostelco.prime.dsl.readTransaction
import org.ostelco.prime.dsl.referred
import org.ostelco.prime.dsl.referredBy
import org.ostelco.prime.dsl.subscribedBy
import org.ostelco.prime.dsl.suspendedWriteTransaction
import org.ostelco.prime.dsl.under
import org.ostelco.prime.dsl.withCode
import org.ostelco.prime.dsl.withId
import org.ostelco.prime.dsl.withKyc
import org.ostelco.prime.dsl.withMsisdn
import org.ostelco.prime.dsl.withSimProfile
import org.ostelco.prime.dsl.withSku
import org.ostelco.prime.dsl.withSubscription
import org.ostelco.prime.dsl.writeTransaction
import org.ostelco.prime.ekyc.DaveKycService
import org.ostelco.prime.ekyc.MyInfoKycService
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.ChangeSegment
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.CustomerRegionStatus
import org.ostelco.prime.model.CustomerRegionStatus.APPROVED
import org.ostelco.prime.model.CustomerRegionStatus.AVAILABLE
import org.ostelco.prime.model.CustomerRegionStatus.PENDING
import org.ostelco.prime.model.HasId
import org.ostelco.prime.model.KycStatus
import org.ostelco.prime.model.KycStatus.REJECTED
import org.ostelco.prime.model.KycType
import org.ostelco.prime.model.KycType.ADDRESS
import org.ostelco.prime.model.KycType.JUMIO
import org.ostelco.prime.model.KycType.MY_INFO
import org.ostelco.prime.model.KycType.NRIC_FIN
import org.ostelco.prime.model.MyInfoApiVersion
import org.ostelco.prime.model.MyInfoApiVersion.V3
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
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.model.SimProfileStatus
import org.ostelco.prime.model.SimProfileStatus.AVAILABLE_FOR_DOWNLOAD
import org.ostelco.prime.model.SimProfileStatus.DELETED
import org.ostelco.prime.model.SimProfileStatus.DOWNLOADED
import org.ostelco.prime.model.SimProfileStatus.INSTALLED
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
import org.ostelco.prime.paymentprocessor.core.PaymentTransactionInfo
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
import org.ostelco.prime.storage.graph.ConfigRegistry.config
import org.ostelco.prime.storage.graph.Graph.read
import org.ostelco.prime.storage.graph.Graph.write
import org.ostelco.prime.storage.graph.Graph.writeSuspended
import org.ostelco.prime.storage.graph.Relation.BELONG_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.FOR_PURCHASE_BY
import org.ostelco.prime.storage.graph.Relation.FOR_PURCHASE_OF
import org.ostelco.prime.storage.graph.Relation.HAD_SIM_PROFILE
import org.ostelco.prime.storage.graph.Relation.HAD_SUBSCRIPTION
import org.ostelco.prime.storage.graph.Relation.HAS_BUNDLE
import org.ostelco.prime.storage.graph.Relation.HAS_SIM_PROFILE
import org.ostelco.prime.storage.graph.Relation.HAS_SUBSCRIPTION
import org.ostelco.prime.storage.graph.Relation.IDENTIFIES
import org.ostelco.prime.storage.graph.Relation.LINKED_TO_BUNDLE
import org.ostelco.prime.storage.graph.Relation.OFFERED_TO_SEGMENT
import org.ostelco.prime.storage.graph.Relation.OFFER_HAS_PRODUCT
import org.ostelco.prime.storage.graph.Relation.REFERRED
import org.ostelco.prime.storage.graph.model.CustomerRegion
import org.ostelco.prime.storage.graph.model.ExCustomer
import org.ostelco.prime.storage.graph.model.Identifies
import org.ostelco.prime.storage.graph.model.Identity
import org.ostelco.prime.storage.graph.model.Offer
import org.ostelco.prime.storage.graph.model.PlanSubscription
import org.ostelco.prime.storage.graph.model.Segment
import org.ostelco.prime.storage.graph.model.SimProfile
import org.ostelco.prime.storage.graph.model.SubscriptionToBundle
import org.ostelco.prime.tracing.Trace
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.stream.Collectors
import javax.ws.rs.core.MultivaluedMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KClass
// Some of the model classes cannot be directly used in Graph Store as Entities.
// See documentation in [model/Model.kt] for more details.
import org.ostelco.prime.model.Identity as ModelIdentity
import org.ostelco.prime.model.Offer as ModelOffer
import org.ostelco.prime.model.Segment as ModelSegment
import org.ostelco.prime.model.SimProfile as ModelSimProfile
import org.ostelco.prime.paymentprocessor.core.NotFoundError as NotFoundPaymentError

enum class Relation(
        val from: KClass<out HasId>,
        val to: KClass<out HasId>) {

    IDENTIFIES(from = Identity::class, to = Customer::class),                           // (Identity) -[IDENTIFIES]-> (Customer)

    HAS_SUBSCRIPTION(from = Customer::class, to = Subscription::class),                 // (Customer) -[HAS_SUBSCRIPTION]-> (Subscription)

    HAD_SUBSCRIPTION(from = ExCustomer::class, to = Subscription::class),               // (ExCustomer) -[HAD_SUBSCRIPTION]-> (Subscription)

    HAS_BUNDLE(from = Customer::class, to = Bundle::class),                             // (Customer) -[HAS_BUNDLE]-> (Bundle)

    HAS_SIM_PROFILE(from = Customer::class, to = SimProfile::class),                    // (Customer) -[HAS_SIM_PROFILE]-> (SimProfile)

    HAD_SIM_PROFILE(from = ExCustomer::class, to = SimProfile::class),                  // (ExCustomer) -[HAD_SIM_PROFILE]-> (SimProfile)

    SUBSCRIBES_TO_PLAN(from = Customer::class, to = Plan::class),                       // (Customer) -[SUBSCRIBES_TO_PLAN]-> (Plan)

    LINKED_TO_BUNDLE(from = Subscription::class, to = Bundle::class),                   // (Subscription) -[LINKED_TO_BUNDLE]-> (Bundle)

    FOR_PURCHASE_BY(from = PurchaseRecord::class, to = Customer::class),                // (PurchaseRecord) -[FOR_PURCHASE_BY]-> (Customer)

    FOR_PURCHASE_OF(from = PurchaseRecord::class, to = Product::class),                 // (PurchaseRecord) -[FOR_PURCHASE_OF]-> (Product)

    REFERRED(from = Customer::class, to = Customer::class),                             // (Customer) -[REFERRED]-> (Customer)

    OFFERED_TO_SEGMENT(from = Offer::class, to = Segment::class),                       // (Offer) -[OFFERED_TO_SEGMENT]-> (Segment)

    OFFER_HAS_PRODUCT(from = Offer::class, to = Product::class),                        // (Offer) -[OFFER_HAS_PRODUCT]-> (Product)

    BELONG_TO_SEGMENT(from = Customer::class, to = Segment::class),                     // (Customer) -[BELONG_TO_SEGMENT]-> (Segment)

    EKYC_SCAN(from = Customer::class, to = ScanInformation::class),                     // (Customer) -[EKYC_SCAN]-> (ScanInformation)

    BELONG_TO_REGION(from = Customer::class, to = Region::class),                       // (Customer) -[BELONG_TO_REGION]-> (Region)

    BELONGED_TO_REGION(from = ExCustomer::class, to = Region::class),                   // (ExCustomer) -[BELONGED_TO_REGION]-> (Region)

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

    private val customerEntity = Customer::class.entityType

    private val exCustomerEntity = ExCustomer::class.entityType

    private val productEntity = Product::class.entityType

    private val purchaseRecordEntity = PurchaseRecord::class.entityType

    private val subscriptionEntity = Subscription::class.entityType

    private val bundleEntity = Bundle::class.entityType

    private val simProfileEntity = SimProfile::class.entityType

    private val planEntity = Plan::class.entityType

    private val regionEntity = Region::class.entityType

    private val scanInformationEntity = ScanInformation::class.entityType

    //
    // Relation
    //

    val identifiesRelation = RelationType(
            relation = IDENTIFIES,
            from = identityEntity,
            to = customerEntity,
            dataClass = Identifies::class.java)
            .also { RelationStore(it) }

    val subscriptionRelation = RelationType(
            relation = HAS_SUBSCRIPTION,
            from = customerEntity,
            to = subscriptionEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val exSubscriptionRelation = RelationType(
            relation = HAD_SUBSCRIPTION,
            from = exCustomerEntity,
            to = subscriptionEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val customerToBundleRelation = RelationType(
            relation = HAS_BUNDLE,
            from = customerEntity,
            to = bundleEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val subscriptionToBundleRelation = RelationType(
            relation = LINKED_TO_BUNDLE,
            from = subscriptionEntity,
            to = bundleEntity,
            dataClass = SubscriptionToBundle::class.java)
            .also { RelationStore(it) }

    val customerToSimProfileRelation = RelationType(
            relation = HAS_SIM_PROFILE,
            from = customerEntity,
            to = simProfileEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val exCustomerToSimProfileRelation = RelationType(
            relation = HAD_SIM_PROFILE,
            from = exCustomerEntity,
            to = simProfileEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val forPurchaseByRelation = RelationType(
            relation = FOR_PURCHASE_BY,
            from = purchaseRecordEntity,
            to = customerEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val forPurchaseOfRelation = RelationType(
            relation = FOR_PURCHASE_OF,
            from = purchaseRecordEntity,
            to = productEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val referredRelation = RelationType(
            relation = REFERRED,
            from = customerEntity,
            to = customerEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val subscribesToPlanRelation = RelationType(
            relation = Relation.SUBSCRIBES_TO_PLAN,
            from = customerEntity,
            to = planEntity,
            dataClass = PlanSubscription::class.java)
    private val subscribesToPlanRelationStore = UniqueRelationStore(subscribesToPlanRelation)

    val customerRegionRelation = RelationType(
            relation = Relation.BELONG_TO_REGION,
            from = customerEntity,
            to = regionEntity,
            dataClass = CustomerRegion::class.java)
    private val customerRegionRelationStore = UniqueRelationStore(customerRegionRelation)

    val exCustomerRegionRelation = RelationType(
            relation = Relation.BELONGED_TO_REGION,
            from = exCustomerEntity,
            to = regionEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val scanInformationRelation = RelationType(
            relation = Relation.EKYC_SCAN,
            from = customerEntity,
            to = scanInformationEntity,
            dataClass = None::class.java)
    private val scanInformationRelationStore = UniqueRelationStore(scanInformationRelation)

    val simProfileRegionRelation = RelationType(
            relation = Relation.SIM_PROFILE_FOR_REGION,
            from = simProfileEntity,
            to = regionEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    val subscriptionSimProfileRelation = RelationType(
            relation = Relation.SUBSCRIPTION_UNDER_SIM_PROFILE,
            from = subscriptionEntity,
            to = simProfileEntity,
            dataClass = None::class.java)
            .also { UniqueRelationStore(it) }

    private val onNewCustomerAction: OnNewCustomerAction = config.onNewCustomerAction.getKtsService()
    private val allowedRegionsService: AllowedRegionsService = config.allowedRegionsService.getKtsService()
    private val onKycApprovedAction: OnKycApprovedAction = config.onKycApprovedAction.getKtsService()
    private val onRegionApprovedAction: OnRegionApprovedAction = config.onRegionApprovedAction.getKtsService()
    private val hssNameLookup: HssNameLookupService = config.hssNameLookupService.getKtsService()

    // -------------
    // Client Store
    // -------------

    //
    // Identity
    //

    private fun ReadTransaction.getCustomerId(identity: ModelIdentity): Either<StoreError, String> =
            getCustomer(identity = identity)
                    .map { it.id }

    private fun ReadTransaction.getCustomerAndAnalyticsId(identity: ModelIdentity): Either<StoreError, Pair<String, String>> =
            getCustomer(identity = identity)
                    .map { Pair(it.id, it.analyticsId) }

    //
    // Balance (Customer - Bundle)
    //

    override fun getBundles(identity: ModelIdentity): Either<StoreError, Collection<Bundle>> = readTransaction {
        getCustomer(identity = identity)
                .flatMap { customer ->
                    get(Bundle forCustomer (Customer withId customer.id))
                }
    }

    override fun updateBundle(bundle: Bundle): Either<StoreError, Unit> = writeTransaction {
        update { bundle }.ifFailedThenRollback(transaction)
    }

    //
    // Customer
    //

    override fun getCustomer(identity: ModelIdentity): Either<StoreError, Customer> = readTransaction {
        getCustomer(identity = identity)
    }

    private fun ReadTransaction.getCustomer(identity: ModelIdentity): Either<StoreError, Customer> {
        return get(Customer identifiedBy (Identity withId identity.id))
                .flatMap {
                    if (it.isEmpty()) {
                        NotFoundError(type = identity.type, id = identity.id)
                                .left()
                    } else {
                        it.single().right()
                    }
                }
    }

    private fun validateCreateCustomerParams(customer: Customer, referredBy: String?): Either<StoreError, Unit> =
            if (customer.referralId == referredBy) {
                ValidationError(type = customerEntity.name, id = customer.id, message = "Referred by self")
                        .left()
            } else {
                Unit.right()
            }

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
                create { customer.copy(createdOn = utcTimeNow()) }.bind()
                fact { (Identity withId identity.id) identifies (Customer withId customer.id) using Identifies(provider = identity.provider) }.bind()
                create { Bundle(id = bundleId, balance = 0L) }.bind()
                fact { (Customer withId customer.id) hasBundle (Bundle withId bundleId) }.bind()
                if (referredBy != null) {
                    fact { (Customer withId referredBy) referred (Customer withId customer.id) }.bind()
                }
                onNewCustomerAction.apply(identity = identity, customer = customer, transaction = transaction).bind()
                AuditLog.info(customerId = customer.id, message = "Customer is created")
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun updateCustomer(
            identity: ModelIdentity,
            nickname: String?,
            contactEmail: String?): Either<StoreError, Unit> = writeTransaction {

        getCustomer(identity = identity)
                .flatMap { existingCustomer ->
                    update {
                        existingCustomer.copy(
                                nickname = nickname ?: existingCustomer.nickname,
                                contactEmail = contactEmail ?: existingCustomer.contactEmail)
                    }.map {
                        AuditLog.info(customerId = existingCustomer.id, message = "Updated nickname/contactEmail")
                    }
                }
                .ifFailedThenRollback(transaction)
    }

    override fun removeCustomer(identity: ModelIdentity): Either<StoreError, Unit> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                // get customer id
                val customer = getCustomer(identity).bind()
                val customerId = customer.id
                // create ex-customer with same id
                create { ExCustomer(id = customerId, terminationDate = LocalDate.now().toString(), createdOn = customer.createdOn) }.bind()
                // get all subscriptions and link them to ex-customer
                val subscriptions = get(Subscription subscribedBy (Customer withId customerId)).bind()
                for (subscription in subscriptions) {
                    fact { (ExCustomer withId customerId) subscribedTo (Subscription withMsisdn subscription.msisdn) }.bind()
                }
                // get all SIM profiles and link them to ex-customer.
                val simProfiles = get(SimProfile forCustomer (Customer withId customerId)).bind()
                val simProfileRegions = mutableSetOf<Region>()
                for (simProfile in simProfiles) {
                    fact { (ExCustomer withId customerId) had (SimProfile withId simProfile.id) }.bind()
                    // also get regions linked to those SimProfiles.
                    simProfileRegions.addAll(get(Region linkedToSimProfile (SimProfile withId simProfile.id)).bind())
                }
                // get Regions linked to Customer
                val regions = get(Region linkedToCustomer (Customer withId customerId)).bind()
                // TODO vihang: clear eKYC data for Regions without any SimProfile
//                val regionsWithoutSimProfile = regions - simProfileRegions
//                // Link regions with SIM profiles to ExCustomer
//                for (region in simProfileRegions) {
//                    fact { (ExCustomer withId customerId) belongedTo (Region withCode region.id) }.bind()
//                }
                // (For now) Link regions to ExCustomer
                for (region in regions) {
                    fact { (ExCustomer withId customerId) belongedTo (Region withCode region.id) }.bind()
                }

                // TODO vihang: When we read and then delete, it fails when deserialization does not work.
                write(query = """
                    MATCH (i:${identityEntity.name} {id:'${identity.id}'})-[:${identifiesRelation.name}]->(c:${customerEntity.name})
                    OPTIONAL MATCH (c)-[:${customerToBundleRelation.name}]->(b:${bundleEntity.name})
                    OPTIONAL MATCH (c)<-[:${forPurchaseByRelation.name}]-(pr:${purchaseRecordEntity.name})
                    OPTIONAL MATCH (c)-[:${scanInformationRelation.name}]->(s:${scanInformationEntity.name})
                    DETACH DELETE i, c, b, pr, s;
                """.trimIndent(), transaction = transaction) { statementResult ->
                    Either.cond(
                            test = statementResult.summary().counters().nodesDeleted() > 0,
                            ifTrue = {},
                            ifFalse = { NotFoundError(type = identityEntity.name, id = identity.id) })
                }.bind()

                /* If removal of payment profile fails, then the customer will be deleted
                   in neo4j but will still be present in payment backend. In that case the
                   profile must be removed from the payment backend manually. */
                paymentProcessor.removePaymentProfile(customerId)
                        .map {
                            Unit
                        }.flatMapLeft {
                            if (it is org.ostelco.prime.paymentprocessor.core.NotFoundError) {
                                /* Ignore. Customer has not bought products yet. */
                                Unit.right()
                            } else {
                                logger.error(NOTIFY_OPS_MARKER,
                                        "Removing corresponding payment profile when removing customer $customerId " +
                                                "failed with error ${it.message} : ${it.description}")
                                NotDeletedError(type = "Payment profile for customer",
                                        id = customerId,
                                        error = it).left()
                            }
                        }.bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    //
    // Customer Region
    //

    override fun getAllRegionDetails(identity: ModelIdentity): Either<StoreError, Collection<RegionDetails>> = readTransaction {
        getCustomer(identity = identity)
                .flatMap { customer ->
                    allowedRegionsService.get(customer, transaction).map { allowedIds ->
                        val allRegions = getAvailableRegionDetails(transaction)
                        val customerRegions = getRegionDetails(
                                customerId = customer.id,
                                transaction = transaction)
                        combineRegions(allRegions, customerRegions)
                                .filter { allowedIds.contains(it.region.id) }
                    }
                }
    }

    override fun getRegionDetails(
            identity: ModelIdentity,
            regionCode: String): Either<StoreError, RegionDetails> = readTransaction {

        getCustomer(identity = identity)
                .flatMap { customer ->
                    allowedRegionsService.get(customer, transaction).flatMap { allowedIds ->
                        getRegionDetails(
                                customerId = customer.id,
                                regionCode = regionCode,
                                transaction = transaction).singleOrNull { allowedIds.contains(it.region.id) }
                                ?.right()
                                ?: NotFoundError(type = customerRegionRelation.name, id = "${customer.id} -> $regionCode").left()
                    }
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
                                                hlr = hssNameLookup.getHssName(regionCode = region.id),
                                                iccId = simProfile.iccId)
                                                .map { simEntry ->
                                                    ModelSimProfile(
                                                            iccId = simProfile.iccId,
                                                            status = simEntry.status,
                                                            eSimActivationCode = simEntry.eSimActivationCode,
                                                            alias = simProfile.alias,
                                                            requestedOn = simProfile.requestedOn,
                                                            downloadedOn = simProfile.downloadedOn,
                                                            installedOn = simProfile.installedOn,
                                                            installedReportedByAppOn = simProfile.installedReportedByAppOn,
                                                            deletedOn = simProfile.deletedOn
                                                    )
                                                }
                                                .getOrHandle { error ->
                                                    logger.error("Failed to fetch SIM Profile: {} for region: {}. Reason: {}",
                                                            simProfile.iccId,
                                                            region.id,
                                                            error)
                                                    null
                                                }
                                    }
                        }
                        RegionDetails(
                                region = region,
                                status = cr.status,
                                kycStatusMap = cr.kycStatusMap,
                                kycExpiryDateMap = cr.kycExpiryDateMap,
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

    private fun getAvailableRegionDetails(transaction: Transaction): Collection<RegionDetails> {
        // Make list of details using regions in present in graphDB with default values
        val query = "MATCH (r:${regionEntity.name}) RETURN r;"
        return read(query, transaction) { it ->
            it.list { record ->
                val region = regionEntity.createEntity(record["r"].asMap())
                RegionDetails(
                        region = region,
                        status = AVAILABLE,
                        kycStatusMap = getKycStatusMapForRegion(region.id.toLowerCase()),
                        simProfiles = emptyList())
            }.requireNoNulls()
        }
    }

    private fun combineRegions(allRegions: Collection<RegionDetails>, customerRegions: Collection<RegionDetails>): Collection<RegionDetails> {
        // Create a map with default region details
        val combined = allRegions.associateBy { it.region.id }.toMutableMap()
        // Overwrite default region details with items from actual region-relations for customer
        customerRegions.forEach {
            combined[it.region.id] = it
        }
        return combined.values
    }

    //
    // SIM Profile
    //

    private val trace by lazy { getResource<Trace>() }

    private val simManager = object : SimManager {

        private val simManager by lazy { getResource<SimManager>() }

        override fun allocateNextEsimProfile(hlr: String, phoneType: String?): Either<String, SimEntry> {
            return if (hlr == "TEST" || phoneType == "TEST") {
                SimEntry(
                        iccId = "TEST-${UUID.randomUUID()}",
                        status = AVAILABLE_FOR_DOWNLOAD,
                        eSimActivationCode = "Dummy eSIM",
                        msisdnList = emptyList()).right()
            } else {
                trace.childSpan("simManager.allocateNextEsimProfile") {
                    simManager.allocateNextEsimProfile(hlr, phoneType)
                }
            }
        }

        override fun getSimProfile(hlr: String, iccId: String): Either<String, SimEntry> {
            return if (hlr == "TEST" || iccId.startsWith("TEST-")) {
                SimEntry(
                        iccId = iccId,
                        status = INSTALLED,
                        eSimActivationCode = "Dummy eSIM",
                        msisdnList = emptyList()).right()
            } else {
                trace.childSpan("simManager.getSimProfile") {
                    simManager.getSimProfile(hlr, iccId)
                }
            }
        }

        override fun addSimProfileStatusUpdateListener(listener: (iccId: String, status: SimProfileStatus) -> Unit) {
            simManager.addSimProfileStatusUpdateListener(listener)
        }
    }

    fun subscribeToSimProfileStatusUpdates() {
        simManager.addSimProfileStatusUpdateListener { iccId, status ->
            writeTransaction {
                IO {
                    Either.monad<StoreError>().binding {
                        logger.info("Received status {} for iccId {}", status, iccId)
                        val simProfiles = getSimProfilesUsingIccId(iccId = iccId, transaction = transaction)
                        if (simProfiles.size != 1) {
                            logger.warn("Found {} SIM Profiles with iccId {}", simProfiles.size, iccId)
                        }
                        simProfiles.forEach { simProfile ->
                            val customers = get(Customer withSimProfile (SimProfile withId simProfile.id)).bind()
                            customers.forEach { customer ->
                                AuditLog.info(customerId = customer.id, message = "Sim Profile (iccId = $iccId) is $status")
                            }
                            when(status) {
                                DOWNLOADED -> update { simProfile.copy(downloadedOn = utcTimeNow()) }.bind()
                                INSTALLED -> update { simProfile.copy(installedOn = utcTimeNow()) }.bind()
                                DELETED -> update { simProfile.copy(deletedOn = utcTimeNow()) }.bind()
                                else -> logger.warn("Not storing timestamp for simProfile: {} for status: {}", iccId, status)
                            }
                            val subscriptions = get(Subscription under (SimProfile withId simProfile.id)).bind()
                            subscriptions.forEach { subscription ->
                                logger.info("Notify status {} for subscription.analyticsId {}", status, subscription.analyticsId)
                                analyticsReporter.reportSubscriptionStatusUpdate(subscription.analyticsId, status)
                            }
                        }
                    }.fix()
                }.unsafeRunSync()
                // Skipping transaction rollback since it is just updating timestamps
            }
        }
    }

    private fun getSimProfilesUsingIccId(iccId: String, transaction: Transaction): Collection<SimProfile> {
        return read("""MATCH (sp:${simProfileEntity.name} {iccId:"$iccId"}) RETURN sp""", transaction) { statementResult ->
            statementResult.list { record ->
                simProfileEntity.createEntity(record["sp"].asMap())
            }
        }
    }

    private val emailNotifier by lazy { getResource<EmailNotifier>() }

    private fun validateBundleList(bundles: List<Bundle>, customerId: String): Either<StoreError, Unit> =
            if (bundles.isEmpty()) {
                Either.left(NotFoundError(type = customerToBundleRelation.name, id = "$customerId -> *"))
            } else {
                Unit.right()
            }

    override fun provisionSimProfile(
            identity: ModelIdentity,
            regionCode: String,
            profileType: String?): Either<StoreError, ModelSimProfile> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = getCustomerId(identity = identity).bind()
                val bundles = get(Bundle forCustomer (Customer withId customerId)).bind()
                validateBundleList(bundles, customerId).bind()
                val customer = get(Customer withId customerId).bind()
                val status = customerRegionRelationStore
                        .get(fromId = customerId, toId = regionCode.toLowerCase(), transaction = transaction)
                        .bind()
                        .status
                isApproved(
                        status = status,
                        customerId = customerId,
                        regionCode = regionCode.toLowerCase()).bind()
                val region = get(Region withCode regionCode.toLowerCase()).bind()
                val simEntry = simManager.allocateNextEsimProfile(hlr = hssNameLookup.getHssName(region.id.toLowerCase()), phoneType = profileType)
                        .mapLeft { NotFoundError("eSIM profile", id = "Loltel") }
                        .bind()
                val simProfile = SimProfile(id = UUID.randomUUID().toString(), iccId = simEntry.iccId, requestedOn = utcTimeNow())
                create { simProfile }.bind()
                fact { (Customer withId customerId) has (SimProfile withId simProfile.id) }.bind()
                fact { (SimProfile withId simProfile.id) isFor (Region withCode regionCode.toLowerCase()) }.bind()
                simEntry.msisdnList.forEach { msisdn ->
                    create { Subscription(msisdn = msisdn) }.bind()
                    val subscription = get(Subscription withMsisdn msisdn).bind()

                    // Report the new provisioning to analytics
                    analyticsReporter.reportSimProvisioning(
                            subscriptionAnalyticsId = subscription.analyticsId,
                            customerAnalyticsId = customer.analyticsId,
                            regionCode = regionCode
                    )

                    bundles.forEach { bundle ->
                        fact { (Subscription withMsisdn msisdn) consumesFrom (Bundle withId bundle.id) using SubscriptionToBundle() }.bind()
                    }
                    fact { (Customer withId customerId) subscribesTo (Subscription withMsisdn msisdn) }.bind()
                    fact { (Subscription withMsisdn msisdn) isUnder (SimProfile withId simProfile.id) }.bind()
                }
                if (!setOf("android", "iphone", "test").contains(profileType)) {
                    emailNotifier.sendESimQrCodeEmail(
                            email = customer.contactEmail,
                            name = customer.nickname,
                            qrCode = simEntry.eSimActivationCode)
                            .mapLeft {
                                logger.error(NOTIFY_OPS_MARKER, "Failed to send email to {}", customer.contactEmail)
                                AuditLog.warn(customerId = customerId, message = "Failed to send email with QR code of provisioned SIM Profile")
                            }
                }
                AuditLog.info(customerId = customerId, message = "Provisioned SIM Profile")
                ModelSimProfile(
                        iccId = simEntry.iccId,
                        alias = simProfile.alias,
                        eSimActivationCode = simEntry.eSimActivationCode,
                        status = simEntry.status,
                        requestedOn = simProfile.requestedOn,
                        downloadedOn = simProfile.downloadedOn,
                        installedOn = simProfile.installedOn,
                        installedReportedByAppOn = simProfile.installedReportedByAppOn,
                        deletedOn = simProfile.deletedOn
                )
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
            identity: ModelIdentity,
            regionCode: String?): Either<StoreError, Collection<ModelSimProfile>> {

        val map = mutableMapOf<String, String>()
        val simProfiles = readTransaction {
            IO {
                Either.monad<StoreError>().binding {

                    val customerId = getCustomerId(identity = identity).bind()
                    val simProfiles = get(SimProfile forCustomer (Customer withId customerId))
                            .bind()
                    if (regionCode == null) {
                        simProfiles.forEach { simProfile ->
                            val region = get(Region linkedToSimProfile (SimProfile withId simProfile.id))
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
            Either.monad<StoreError>().binding {
                simProfiles.bind().map { simProfile ->

                    val regionId = (regionCode ?: map[simProfile.id])

                    val simEntry = if (regionId != null) {
                        simManager.getSimProfile(
                                hlr = hssNameLookup.getHssName(regionId),
                                iccId = simProfile.iccId)
                                .getOrHandle { error ->
                                    logger.warn("SimProfile not found in SIM Manager DB. region: {}, iccId: {}, error: {}", regionId, simProfile.iccId, error)
                                    SimEntry(
                                            iccId = simProfile.iccId,
                                            status = NOT_READY,
                                            eSimActivationCode = "Dummy eSIM",
                                            msisdnList = emptyList()
                                    )
                                }
                    } else {
                        logger.warn("SimProfile not linked to any region. iccId: {}", simProfile.iccId)
                        SimEntry(
                                iccId = simProfile.iccId,
                                status = NOT_READY,
                                eSimActivationCode = "Dummy eSIM",
                                msisdnList = emptyList()
                        )
                    }

                    ModelSimProfile(
                            iccId = simProfile.iccId,
                            alias = simProfile.alias,
                            eSimActivationCode = simEntry.eSimActivationCode,
                            status = simEntry.status,
                            requestedOn = simProfile.requestedOn,
                            downloadedOn = simProfile.downloadedOn,
                            installedOn = simProfile.installedOn,
                            installedReportedByAppOn = simProfile.installedReportedByAppOn,
                            deletedOn = simProfile.deletedOn
                    )
                }
            }.fix()
        }.unsafeRunSync()
    }

    override fun updateSimProfile(
            identity: ModelIdentity,
            regionCode: String,
            iccId: String,
            alias: String): Either<StoreError, ModelSimProfile> {
        val simProfileEither = writeTransaction {
            IO {
                Either.monad<StoreError>().binding {

                    val customerId = getCustomerId(identity = identity).bind()
                    val simProfile = get(SimProfile forCustomer (Customer withId customerId))
                            .bind()
                            .firstOrNull { simProfile -> simProfile.iccId == iccId }
                            ?: NotFoundError(type = simProfileEntity.name, id = iccId).left().bind<SimProfile>()

                    val updatedSimProfile = simProfile.copy(alias = alias)
                    update { updatedSimProfile }.bind()
                    AuditLog.info(customerId = customerId, message = "Updated alias of SIM Profile (iccId = $iccId)")
                    updatedSimProfile
                }.fix()
            }.unsafeRunSync().ifFailedThenRollback(transaction)
        }

        return IO {
            Either.monad<StoreError>().binding {
                val simProfile = simProfileEither.bind()
                val simEntry = simManager.getSimProfile(
                        hlr = hssNameLookup.getHssName(regionCode),
                        iccId = iccId)
                        .getOrHandle { error ->
                            logger.warn("SimProfile not found in SIM Manager DB. region: {}, iccId: {}, error: {}", regionCode, simProfile.iccId, error)
                            SimEntry(
                                    iccId = simProfile.iccId,
                                    status = NOT_READY,
                                    eSimActivationCode = "Dummy eSIM",
                                    msisdnList = emptyList()
                            )
                        }

                ModelSimProfile(
                        iccId = simProfile.iccId,
                        alias = simProfile.alias,
                        eSimActivationCode = simEntry.eSimActivationCode,
                        status = simEntry.status,
                        requestedOn = simProfile.requestedOn,
                        downloadedOn = simProfile.downloadedOn,
                        installedOn = simProfile.installedOn,
                        installedReportedByAppOn = simProfile.installedReportedByAppOn,
                        deletedOn = simProfile.deletedOn
                )
            }.fix()
        }.unsafeRunSync()
    }

    override fun markSimProfileAsInstalled(
            identity: ModelIdentity,
            regionCode: String,
            iccId: String): Either<StoreError, ModelSimProfile> {

        val simProfileEither = writeTransaction {
            IO {
                Either.monad<StoreError>().binding {

                    val customerId = getCustomerId(identity = identity).bind()
                    val simProfile = get(SimProfile forCustomer (Customer withId customerId))
                            .bind()
                            .firstOrNull { simProfile -> simProfile.iccId == iccId }
                            ?: NotFoundError(type = simProfileEntity.name, id = iccId).left().bind<SimProfile>()

                    val updatedSimProfile = simProfile.copy(installedReportedByAppOn = utcTimeNow())
                    update { updatedSimProfile }.bind()
                    AuditLog.info(customerId = customerId, message = "App reported SIM Profile (iccId = $iccId) as installed.")
                    updatedSimProfile
                }.fix()
            }.unsafeRunSync().ifFailedThenRollback(transaction)
        }

        return IO {
            Either.monad<StoreError>().binding {
                val simProfile = simProfileEither.bind()
                val simEntry = simManager.getSimProfile(
                        hlr = hssNameLookup.getHssName(regionCode),
                        iccId = iccId)
                        .getOrHandle { error ->
                            logger.warn("SimProfile not found in SIM Manager DB. region: {}, iccId: {}, error: {}", regionCode, simProfile.iccId, error)
                            SimEntry(
                                    iccId = simProfile.iccId,
                                    status = NOT_READY,
                                    eSimActivationCode = "Dummy eSIM",
                                    msisdnList = emptyList()
                            )
                        }

                ModelSimProfile(
                        iccId = simProfile.iccId,
                        alias = simProfile.alias,
                        eSimActivationCode = simEntry.eSimActivationCode,
                        status = simEntry.status,
                        requestedOn = simProfile.requestedOn,
                        downloadedOn = simProfile.downloadedOn,
                        installedOn = simProfile.installedOn,
                        installedReportedByAppOn = simProfile.installedReportedByAppOn,
                        deletedOn = simProfile.deletedOn
                )
            }.fix()
        }.unsafeRunSync()
    }

    override fun sendEmailWithActivationQrCode(
            identity: ModelIdentity,
            regionCode: String,
            iccId: String): Either<StoreError, ModelSimProfile> {

        val infoEither = readTransaction {
            IO {
                Either.monad<StoreError>().binding {

                    val customer = getCustomer(identity = identity).bind()
                    val simProfile = get(SimProfile forCustomer (Customer withId customer.id))
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
                        hlr = hssNameLookup.getHssName(regionCode),
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

                ModelSimProfile(
                        iccId = simEntry.iccId,
                        eSimActivationCode = simEntry.eSimActivationCode,
                        status = simEntry.status,
                        alias = simProfile.alias,
                        requestedOn = simProfile.requestedOn,
                        downloadedOn = simProfile.downloadedOn,
                        installedOn = simProfile.installedOn,
                        installedReportedByAppOn = simProfile.installedReportedByAppOn,
                        deletedOn = simProfile.deletedOn
                )
            }.fix()
        }.unsafeRunSync()
    }

    override fun deleteSimProfileWithSubscription(regionCode: String, iccId: String): Either<StoreError, Unit> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val simProfiles = get(SimProfile linkedToRegion (Region withCode regionCode)).bind()
                simProfiles.forEach { simProfile ->
                    val subscriptions = get(Subscription under (SimProfile withId simProfile.id)).bind()
                    subscriptions.forEach { subscription ->
                        delete(Subscription withMsisdn subscription.msisdn).bind()
                    }
                    delete(SimProfile withId simProfile.id).bind()
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
            identity: ModelIdentity,
            regionCode: String,
            iccId: String,
            alias: String,
            msisdn: String): Either<StoreError, Unit> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = getCustomerId(identity = identity).bind()

                val simProfile = SimProfile(
                        id = UUID.randomUUID().toString(),
                        iccId = iccId,
                        alias = alias)
                create { simProfile }.bind()
                fact { (SimProfile withId simProfile.id) isFor (Region withCode regionCode.toLowerCase()) }.bind()
                fact { (Customer withId customerId) has (SimProfile withId simProfile.id) }.bind()

                create { Subscription(msisdn) }.bind()
                fact { (Subscription withMsisdn msisdn) isUnder (SimProfile withId simProfile.id) }.bind()
                fact { (Customer withId customerId) subscribesTo (Subscription withMsisdn msisdn) }.bind()

                val bundles = get(Bundle forCustomer (Customer withId customerId)).bind()
                validateBundleList(bundles, customerId).bind()
                bundles.forEach { bundle ->
                    fact { (Subscription withMsisdn msisdn) consumesFrom (Bundle withId bundle.id) using SubscriptionToBundle() }.bind()
                }
                AuditLog.info(customerId = customerId, message = "Added SIM Profile and Subscription by Admin")
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun getSubscriptions(identity: ModelIdentity, regionCode: String?): Either<StoreError, Collection<Subscription>> = readTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val customerId = getCustomerId(identity = identity).bind()
                if (regionCode == null) {
                    get(Subscription subscribedBy (Customer withId customerId)).bind()
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

    override fun getProducts(identity: ModelIdentity): Either<StoreError, Map<String, Product>> {
        return readTransaction {

            getCustomerId(identity = identity)
                    .flatMap { customerId ->
                        read<Either<StoreError, Map<String, Product>>>("""
                            MATCH (:${customerEntity.name} {id: '$customerId'})
                            -[:${customerToSegmentRelation.name}]->(:${segmentEntity.name})
                            <-[:${offerToSegmentRelation.name}]-(:${offerEntity.name})
                            -[:${offerToProductRelation.name}]->(product:${productEntity.name})
                            RETURN DISTINCT product;
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

    override fun getProduct(identity: ModelIdentity, sku: String): Either<StoreError, Product> {
        return readTransaction {
            getProduct(identity, sku)
        }
    }

    private fun ReadTransaction.getProduct(identity: ModelIdentity, sku: String): Either<StoreError, Product> {
        return getCustomerId(identity = identity)
                .flatMap { customerId ->
                    read("""
                            MATCH (:${customerEntity.name} {id: '$customerId'})
                            -[:${customerToSegmentRelation.name}]->(:${segmentEntity.name})
                            <-[:${offerToSegmentRelation.name}]-(:${offerEntity.name})
                            -[:${offerToProductRelation.name}]->(product:${productEntity.name} {sku: '$sku'})
                            RETURN DISTINCT product;
                            """.trimIndent(),
                            transaction) { statementResult ->

                        val products = statementResult
                                    .list { productEntity.createEntity(it["product"].asMap()) }
                                    .toList()
                        when(products.size) {
                            0 -> NotFoundError(type = productEntity.name, id = sku).left()
                            1 -> products.single().right()
                            else -> {
                                logger.warn("Found multiple products: {} with same sku:{} for customerId: {}", products, sku, customerId)
                                products.first().right()
                            }
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
                            SET bundle._LOCK_ = true, r._LOCK_ = true, sn.lastActiveOn="${utcTimeNow()}"
                            WITH r, bundle, sn.analyticsId AS msisdnAnalyticsId, (CASE WHEN ((toInteger(bundle.balance) + toInteger(r.reservedBytes) - $usedBytes) > 0) THEN (toInteger(bundle.balance) + toInteger(r.reservedBytes) - $usedBytes) ELSE 0 END) AS tmpBalance
                            WITH r, bundle, msisdnAnalyticsId, tmpBalance, (CASE WHEN (tmpBalance < $requestedBytes) THEN tmpBalance ELSE $requestedBytes END) as tmpGranted
                            SET r.reservedBytes = toString(tmpGranted), r.reservedOn = "${utcTimeNow()}", bundle.balance = toString(tmpBalance - tmpGranted), bundle.lastConsumedOn="${utcTimeNow()}"
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

    override fun purchaseProduct(identity: ModelIdentity,
                                 sku: String,
                                 sourceId: String?,
                                 saveCard: Boolean): Either<PaymentError, ProductInfo> = writeTransaction {
        IO {
            Either.monad<PaymentError>().binding {

                val customer = getCustomer(identity = identity)
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
                                    taxRegionId = product.paymentTaxRegionId)
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
                            timestamp = Instant.now().toEpochMilli(),
                            properties = mapOf("invoiceId" to invoiceId))

                    /* If this step fails, the previously added 'removeInvoice' call added to the transaction
                    will ensure that the invoice will be voided. */
                    createPurchaseRecord(customer.id, purchaseRecord)
                            .mapLeft {
                                logger.error("Failed to save purchase record for customer ${customer.id}, invoice-id $invoiceId, invoice will be voided in Stripe")
                                AuditLog.error(customerId = customer.id, message = "Failed to save purchase record - invoice-id $invoiceId, invoice will be voided in Stripe")
                                BadGatewayError("Failed to save purchase record",
                                        error = it)
                            }.bind()

                    /* TODO: While aborting transactions, send a record with "reverted" status. */
                    analyticsReporter.reportPurchase(
                            customerAnalyticsId = customer.analyticsId,
                            purchaseId = purchaseRecord.id,
                            sku = product.sku,
                            priceAmountCents = product.price.amount,
                            priceCurrency = product.price.currency)
                }

                applyProduct(
                        customerId = customer.id,
                        product = product
                ).mapLeft {
                    BadGatewayError(description = it.message, error = it.error).left().bind()
                }.bind()

                ProductInfo(product.sku)
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }
    // << END

    fun WriteTransaction.applyProduct(customerId: String, product: Product) = IO {
        Either.monad<StoreError>().binding {
            when (product.productClass) {
                MEMBERSHIP -> {
                    product.segmentIds.forEach { segmentId ->
                        assignCustomerToSegment(customerId = customerId,
                                segmentId = segmentId,
                                transaction = transaction)
                                .mapLeft {
                                    SystemError(
                                            type = "Customer -> Segment",
                                            id = "$customerId -> $segmentId",
                                            message = "Failed to assign Membership",
                                            error = it)
                                }
                                .bind()
                    }
                }
                SIMPLE_DATA -> {
                    /* Topup. */
                    simpleDataProduct(
                            customerId = customerId,
                            sku = product.sku,
                            bytes = product.noOfBytes,
                            transaction = transaction)
                            .mapLeft {
                                SystemError(
                                        type = "Customer",
                                        id = product.sku,
                                        message = "Failed to update balance for customer: $customerId",
                                        error = it)
                            }
                            .bind()
                }
                else -> {
                    SystemError(
                            type = "Product",
                            id = product.sku,
                            message = "Missing product class in properties of product: ${product.sku}").left().bind()
                }
            }
        }.fix()
    }.unsafeRunSync()

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
    private fun WriteTransaction.purchasePlan(customer: Customer,
                                              sku: String,
                                              taxRegionId: String?,
                                              sourceId: String?,
                                              saveCard: Boolean): Either<PaymentError, SubscriptionDetailsInfo> {
        return IO {
            Either.monad<PaymentError>().binding {

                /* Bail out if subscriber tries to buy an already bought plan.
                   Note: Already verified above that 'customer' (subscriber) exists. */
                get(PurchaseRecord forPurchaseBy (Customer withId customer.id))
                        .map { purchaseRecords ->
                            if (purchaseRecords.any { x:PurchaseRecord -> x.product.sku == sku }) {
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
                        taxRegionId = taxRegionId)
                        .mapLeft {
                            AuditLog.error(customerId = customer.id, message = "Failed to subscribe to plan $sku")
                            BadGatewayError("Failed to subscribe ${customer.id} to plan $sku",
                                    error = it)
                        }
                        .bind()
            }.fix()
        }.unsafeRunSync()
    }

    private fun WriteTransaction.subscribeToPlan(
            customerId: String,
            planId: String,
            taxRegionId: String?,
            trialEnd: Long = 0L): Either<StoreError, SubscriptionDetailsInfo> {

        return IO {
            Either.monad<StoreError>().binding {
                val plan = get(Plan withId planId)
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
                get(Plan forCustomer (Customer withId customerId))
                        .map {
                            if (it.any { x -> x.id == planId }) {
                                removeSubscription(customerId, planId, invoiceNow = true)
                            }
                        }

                /* Lookup in payment backend will fail if no value found for 'stripePlanId'. */
                val planStripeId = plan.stripePlanId ?: SystemError(type = planEntity.name, id = plan.id,
                        message = "No reference to Stripe plan found in ${plan.id}")
                        .left()
                        .bind()

                val subscriptionDetailsInfo = paymentProcessor.createSubscription(
                        planId = planStripeId,
                        customerId = profileInfo.id,
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
                fact {
                    (Customer withId customerId) subscribesTo (Plan withId planId) using
                            PlanSubscription(
                                    subscriptionId = subscriptionDetailsInfo.id,
                                    created = subscriptionDetailsInfo.created,
                                    trialEnd = subscriptionDetailsInfo.trialEnd)
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
            transaction: PrimeTransaction): Either<StoreError, Unit> = IO {

        Either.monad<StoreError>().binding {

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
                                logger.error("Failed to update balance for customer: {}", customerId)
                                NotUpdatedError(
                                        type = "Balance of Customer",
                                        id = customerId)
                            })
                }.bind()
                AuditLog.info(customerId = customerId, message = "Added $bytes bytes to data bundle")
            }
            Unit
        }.fix()
    }.unsafeRunSync()

    //
    // Purchase Records
    //

    override fun getPurchaseRecords(identity: ModelIdentity): Either<StoreError, Collection<PurchaseRecord>> =
            readTransaction {
                getCustomerId(identity = identity)
                        .flatMap { customerId ->
                            get(PurchaseRecord forPurchaseBy (Customer withId customerId))
                        }
            }

    override fun addPurchaseRecord(customerId: String, purchase: PurchaseRecord): Either<StoreError, String> =
            writeTransaction {
                createPurchaseRecord(customerId, purchase)
                        .ifFailedThenRollback(transaction)
            }

    fun WriteTransaction.createPurchaseRecord(customerId: String,
                                              purchaseRecord: PurchaseRecord): Either<StoreError, String> {

        val invoiceId = purchaseRecord.properties["invoiceId"]

        return IO {
            Either.monad<StoreError>().binding {

                if (invoiceId != null
                        && getPurchaseRecordUsingInvoiceId(customerId, invoiceId).isRight()) {
                    /* Avoid charging for the same invoice twice if invoice information is present. */

                    ValidationError(type = purchaseRecordEntity.name,
                            id = purchaseRecord.id,
                            message = "A purchase record for ${purchaseRecord.product} for customer $customerId already exists")
                            .left()
                            .bind()
                }
                create { purchaseRecord }.bind()
                fact { (PurchaseRecord withId purchaseRecord.id) forPurchaseBy (Customer withId customerId) }.bind()
                fact { (PurchaseRecord withId purchaseRecord.id) forPurchaseOf (Product withSku purchaseRecord.product.sku) }.bind()
                purchaseRecord.id
            }.fix()
        }.unsafeRunSync()
    }

    /* As Stripes invoice-id is used as the 'id' of a purchase record, this method
       allows for detecting double charges etc. */
    private fun ReadTransaction.getPurchaseRecordUsingInvoiceId(
            customerId: String,
            invoiceId: String): Either<StoreError, PurchaseRecord> =

            get(PurchaseRecord forPurchaseBy (Customer withId customerId))
                    .map { records ->
                        records.find {
                            it.properties["invoiceId"] == invoiceId
                        }
                    }.leftIfNull { NotFoundError(type = purchaseRecordEntity.name, id = invoiceId) }

    //
    // Referrals
    //

    override fun getReferrals(identity: ModelIdentity): Either<StoreError, Collection<String>> = readTransaction {
        getCustomerId(identity = identity)
                .flatMap { customerId ->
                    get(Customer referredBy (Customer withId customerId))
                            .map { list -> list.map { it.nickname } }
                }
    }

    override fun getReferredBy(identity: ModelIdentity): Either<StoreError, String?> = readTransaction {
        getCustomerId(identity = identity)
                .flatMap { customerId ->
                    get(Customer referred (Customer withId customerId))
                            .map { it.singleOrNull()?.nickname }
                }
    }

    internal fun createCustomerRegionSetting(
            customer: Customer,
            status: CustomerRegionStatus,
            regionCode: String): Either<StoreError, Unit> = writeTransaction {

        createCustomerRegionSetting(
                customer = customer,
                status = status,
                regionCode = regionCode,
                transaction = transaction)
    }

    private fun createCustomerRegionSetting(
            customer: Customer,
            status: CustomerRegionStatus,
            regionCode: String,
            transaction: PrimeTransaction): Either<StoreError, Unit> =

            customerRegionRelationStore
                    .createIfAbsent(
                            fromId = customer.id,
                            relation = CustomerRegion(
                                    status = status,
                                    kycStatusMap = getKycStatusMapForRegion(regionCode)),
                            toId = regionCode,
                            transaction = transaction)
                    .flatMap {
                        if (status == APPROVED) {
                            onRegionApprovedAction.apply(
                                    customer = customer,
                                    regionCode = regionCode,
                                    transaction = PrimeTransaction(transaction)
                            )
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
                    transaction = transaction)
                    .map {
                        AuditLog.info(customerId = customerId, message = "Assigned to segment - $segmentId")
                    }
                    .mapLeft { storeError ->
                        if (storeError is NotCreatedError && storeError.type == customerToSegmentRelation.name) {
                            logger.error("Failed to assign Customer - {} to a Segment - {}", customerId, segmentId)
                            ValidationError(type = customerEntity.name, id = customerId, message = "Unsupported segment: $segmentId")
                        } else {
                            storeError
                        }
                    }

    //
    // eKYC - Jumio
    //

    override fun createNewJumioKycScanId(
            identity: ModelIdentity,
            regionCode: String): Either<StoreError, ScanInformation> = writeTransaction {

        getCustomer(identity = identity)
                .flatMap { customer ->
                    // Generate new id for the scan
                    val scanId = UUID.randomUUID().toString()
                    val newScan = ScanInformation(scanId = scanId, countryCode = regionCode, status = ScanStatus.PENDING, scanResult = null)
                    createCustomerRegionSetting(
                            customer = customer, status = PENDING, regionCode = regionCode.toLowerCase(), transaction = transaction)
                            .flatMap {
                                create { newScan }
                            }
                            .flatMap {
                                scanInformationRelationStore.createIfAbsent(customer.id, newScan.id, transaction)
                            }
                            .flatMap {
                                setKycStatus(
                                        customer = customer,
                                        regionCode = regionCode.toLowerCase(),
                                        kycType = JUMIO,
                                        kycStatus = KycStatus.PENDING,
                                        transaction = transaction)
                            }
                            .map {
                                AuditLog.info(customerId = customer.id, message = "Created new Jumio scan id - ${newScan.id}")
                                newScan
                            }
                }
                .ifFailedThenRollback(transaction)
    }

    private fun ReadTransaction.getCustomerUsingScanId(scanId: String): Either<StoreError, Customer> {
        return get(Customer withKyc (ScanInformation withId scanId))
                .flatMap { customers ->
                    customers.singleOrNull()?.right()
                            ?: NotFoundError(type = scanInformationEntity.name, id = scanId).left()
                }
    }

    override fun getCountryCodeForScan(scanId: String): Either<StoreError, String> = readTransaction {
        get(ScanInformation withId scanId)
                .flatMap { scanInformation ->
                    scanInformation.countryCode.right()
                }
    }

    // TODO merge into a single query which will use customerId and scanId
    override fun getScanInformation(identity: ModelIdentity, scanId: String): Either<StoreError, ScanInformation> = readTransaction {
        getCustomerId(identity = identity)
                .flatMap { customerId ->
                    get(ScanInformation withId scanId).flatMap { scanInformation ->
                        getCustomerUsingScanId(scanInformation.scanId).flatMap { customer ->
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

    override fun getAllScanInformation(identity: ModelIdentity): Either<StoreError, Collection<ScanInformation>> = readTransaction {
        getCustomerId(identity = identity)
                .flatMap { customerId ->
                    get(ScanInformation forCustomer (Customer withId customerId))
                }
    }

    private val appNotifier by lazy { getResource<AppNotifier>() }

    override fun updateScanInformation(scanInformation: ScanInformation, vendorData: MultivaluedMap<String, String>): Either<StoreError, Unit> = writeTransaction {
        logger.info("updateScanInformation : ${scanInformation.scanId} status: ${scanInformation.status}")
        val updatedScanInformation = scanInformation.copy(status = verifyAndUpdateScanStatus(scanInformation))
        getCustomerUsingScanId(updatedScanInformation.scanId).flatMap { customer ->
            update { updatedScanInformation }.flatMap {
                logger.info("updating scan Information for : ${customer.contactEmail} id: ${updatedScanInformation.scanId} status: ${updatedScanInformation.status}")
                val extendedStatus = scanInformationDatastore.getExtendedStatusInformation(updatedScanInformation)
                if (updatedScanInformation.status == ScanStatus.APPROVED) {

                    logger.info("Inserting scan Information to cloud storage : id: ${updatedScanInformation.scanId} countryCode: ${updatedScanInformation.countryCode}")
                    scanInformationDatastore.upsertVendorScanInformation(customer.id, updatedScanInformation.countryCode, vendorData)
                            .flatMap {
                                appNotifier.notify(
                                        notificationType = NotificationType.JUMIO_VERIFICATION_SUCCEEDED,
                                        customerId = customer.id,
                                        data = extendedStatus
                                )
                                setKycStatus(
                                        customer = customer,
                                        regionCode = updatedScanInformation.countryCode.toLowerCase(),
                                        kycType = JUMIO,
                                        kycExpiryDate = updatedScanInformation.scanResult?.expiry,
                                        kycIdType = updatedScanInformation.scanResult?.type,
                                        transaction = transaction)
                            }
                } else {
                    // Do not notify the app when the verification fails because the customer has cancelled and hence did not upload an ID.
                    if (updatedScanInformation.scanResult?.verificationStatus != "NO_ID_UPLOADED") {
                        // TODO: find out what more information can be passed to the client.
                        appNotifier.notify(
                                notificationType = NotificationType.JUMIO_VERIFICATION_FAILED,
                                customerId = customer.id,
                                data = extendedStatus
                        )
                    }
                    if(updatedScanInformation.scanResult?.verificationStatus != "NO_ID_UPLOADED") {
                        logger.info(NOTIFY_OPS_MARKER, "Jumio verification failed for ${customer.contactEmail} Info: $extendedStatus")
                    }
                    setKycStatus(
                            customer = customer,
                            regionCode = updatedScanInformation.countryCode.toLowerCase(),
                            kycType = JUMIO,
                            kycStatus = REJECTED,
                            transaction = transaction)
                }
            }
        }.ifFailedThenRollback(transaction)
    }

    private fun verifyAndUpdateScanStatus(scanInformation: ScanInformation): ScanStatus {

        val above18yrsOfAge = scanInformation.scanResult
                ?.dob
                ?.let(LocalDate::parse)
                .isLessThan18yrsAgo()
                .not()

        return if (above18yrsOfAge) {
            scanInformation.status
        } else {
            ScanStatus.REJECTED
        }
    }

    /**
     * Return true if value is null or if it is more than 18 years ago.
     */
    private fun LocalDate?.isLessThan18yrsAgo() = this
            ?.plusYears(18)
            ?.isAfter(LocalDate.now())
            ?: false
    //
    // eKYC - MyInfo
    //

    private val myInfoKycV3Service by lazy { getResource<MyInfoKycService>("v3") }

    private val secureArchiveService by lazy { getResource<SecureArchiveService>() }

    override fun getCustomerMyInfoData(
            identity: ModelIdentity,
            version: MyInfoApiVersion,
            authorisationCode: String): Either<StoreError, String> {
        return IO {
            Either.monad<StoreError>().binding {

                val customer = getCustomer(identity = identity).bind()

                // set MY_INFO KYC Status to Pending
                setKycStatus(
                        customer = customer,
                        regionCode = "sg",
                        kycType = MY_INFO,
                        kycStatus = KycStatus.PENDING).bind()

                val myInfoData = try {
                    when (version) {
                        V3 -> myInfoKycV3Service
                    }.getPersonData(authorisationCode)
                } catch (e: Exception) {
                    logger.error("Failed to fetched MyInfo $version using authCode = $authorisationCode", e)
                    null
                } ?: SystemError(
                        type = "MyInfo Auth Code",
                        id = authorisationCode,
                        message = "Failed to fetched MyInfo $version").left().bind()

                val personData = myInfoData.personData ?: SystemError(
                        type = "MyInfo Auth Code",
                        id = authorisationCode,
                        message = "Failed to fetched MyInfo $version").left().bind()

                val kycStatus = if(myInfoData.birthDate.isLessThan18yrsAgo()) {
                    AuditLog.warn(customerId = customer.id, message = "Customer age is less than 18yrs.")
                    REJECTED
                } else {
                    // store data only if approved
                    secureArchiveService.archiveEncrypted(
                            customerId = customer.id,
                            fileName = "myInfoData",
                            regionCodes = listOf("sg"),
                            dataMap = mapOf(
                                    "uinFin" to myInfoData.uinFin.toByteArray(),
                                    "personData" to personData.toByteArray()
                            )
                    ).bind()
                    KycStatus.APPROVED
                }

                // set MY_INFO KYC Status to Approved
                setKycStatus(
                        customer = customer,
                        regionCode = "sg",
                        kycType = MY_INFO,
                        kycStatus = kycStatus,
                        kycExpiryDate = myInfoData.passExpiryDate?.toString()).bind()

                personData
            }.fix()
        }.unsafeRunSync()
    }

    //
    // eKYC - NRIC/FIN
    //

    private val daveKycService by lazy { getResource<DaveKycService>() }

    override fun checkNricFinIdUsingDave(
            identity: ModelIdentity,
            nricFinId: String): Either<StoreError, Unit> {

        return IO {
            Either.monad<StoreError>().binding {

                logger.info("checkNricFinIdUsingDave for $nricFinId")

                val customer = getCustomer(identity = identity).bind()

                // set NRIC_FIN KYC Status to Pending
                setKycStatus(
                        customer = customer,
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
                        customerId = customer.id,
                        fileName = "nricFin",
                        regionCodes = listOf("sg"),
                        dataMap = mapOf("nricFinId" to nricFinId.toByteArray())
                ).bind()

                // set NRIC_FIN KYC Status to Approved
                setKycStatus(
                        customer = customer,
                        regionCode = "sg",
                        kycType = NRIC_FIN).bind()
            }.fix()
        }.unsafeRunSync()
    }

    //
    // eKYC - Address and Phone number
    //
    override fun saveAddress(
            identity: ModelIdentity,
            address: String,
            regionCode: String): Either<StoreError, Unit> {

        return IO {
            Either.monad<StoreError>().binding {

                val customer = getCustomer(identity = identity).bind()

                // set ADDRESS KYC Status to Pending
                setKycStatus(
                        customer = customer,
                        regionCode = regionCode,
                        kycType = ADDRESS,
                        kycStatus = KycStatus.PENDING).bind()

                secureArchiveService.archiveEncrypted(
                        customerId = customer.id,
                        fileName = "address",
                        regionCodes = listOf(regionCode),
                        dataMap = mapOf(
                                "address" to address.toByteArray())
                ).bind()

                // set ADDRESS KYC Status to Approved
                setKycStatus(
                        customer = customer,
                        regionCode = regionCode,
                        kycType = ADDRESS).bind()
            }.fix()
        }.unsafeRunSync()
    }

    //
    // eKYC - Status Flags
    //

    internal fun setKycStatus(
            customer: Customer,
            regionCode: String,
            kycType: KycType,
            kycExpiryDate: String? = null,
            kycStatus: KycStatus = KycStatus.APPROVED) = writeTransaction {

        setKycStatus(
                customer = customer,
                regionCode = regionCode,
                kycType = kycType,
                kycStatus = kycStatus,
                kycExpiryDate = kycExpiryDate,
                transaction = transaction)
                .ifFailedThenRollback(transaction)
    }

    // FIXME: vihang This implementation has risk of loss of data due during concurrency to stale read since it does
    // READ-UPDATE-WRITE.
    fun setKycStatus(
            customer: Customer,
            regionCode: String,
            kycType: KycType,
            kycStatus: KycStatus = KycStatus.APPROVED,
            kycExpiryDate: String? = null,
            kycIdType: String? = null,
            transaction: Transaction): Either<StoreError, Unit> {

        return IO {
            Either.monad<StoreError>().binding {

                // get combinations of KYC needed for this region to be Approved
                val approvedKycTypeSetList = getApprovedKycTypeSetList(regionCode)

                // fetch existing values from DB
                val existingCustomerRegion = customerRegionRelationStore.get(
                        fromId = customer.id,
                        toId = regionCode,
                        transaction = transaction)
                        .flatMapLeft { storeError ->
                            if(storeError is NotFoundError && storeError.type == customerRegionRelation.name) {
                                // default value if absent in DB
                                CustomerRegion(
                                        status = PENDING,
                                        kycStatusMap = getKycStatusMapForRegion(regionCode),
                                        initiatedOn = utcTimeNow()
                                ).right()
                            } else {
                                storeError.left()
                            }
                        }.bind()

                // using existing and received KYC status, compute new KYC status
                val existingKycStatusMap = existingCustomerRegion.kycStatusMap
                val existingKycStatus = existingKycStatusMap[kycType]
                val newKycStatus = when (existingKycStatus) {
                    // APPROVED is end state. No more state change.
                    KycStatus.APPROVED -> KycStatus.APPROVED
                    // REJECTED and PENDING to 'any' is allowed
                    else -> kycStatus
                }

                // if new status is different from existing status
                if (existingKycStatus != newKycStatus) {
                    if (kycStatus == newKycStatus) {
                        AuditLog.info(customerId = customer.id, message = "Setting $kycType status from $existingKycStatus to $newKycStatus")
                    } else {
                        AuditLog.info(customerId = customer.id, message = "Setting $kycType status from $existingKycStatus to $newKycStatus instead of $kycStatus")
                    }
                    if (newKycStatus == KycStatus.APPROVED) {
                        onKycApprovedAction.apply(
                                customer = customer,
                                regionCode = regionCode,
                                kycType = kycType,
                                kycExpiryDate = kycExpiryDate,
                                kycIdType = kycIdType,
                                allowedRegionsService = allowedRegionsService,
                                transaction = PrimeTransaction(transaction)
                        ).bind()
                    }
                } else {
                    AuditLog.info(customerId = customer.id, message = "Ignoring setting $kycType status to $kycStatus since it is already $existingKycStatus")
                }

                // update KYC status map with new value. This map will then be stored in DB.
                val newKycStatusMap = existingKycStatusMap.copy(key = kycType, value = newKycStatus)

                // check if Region is Approved.
                val isRegionApproved = approvedKycTypeSetList.any { kycTypeSet ->
                    // Region is approved if the set of Approved KYCs is a superset of any one of the set configured in the list - approvedKycTypeSetList.
                    newKycStatusMap.filter { it.value == KycStatus.APPROVED }.keys.containsAll(kycTypeSet)
                }

                // if the Region status is Approved, but the existing status was not Approved, then it has been approved now.
                val isRegionApprovedNow = existingCustomerRegion.status != APPROVED && isRegionApproved

                // Save Region status as APPROVED, if it is approved. Do not change Region status otherwise.
                val newRegionStatus = if (isRegionApproved) {
                    APPROVED
                } else {
                    existingCustomerRegion.status
                }

                // timestamp for region approval
                val regionApprovedOn = if (isRegionApprovedNow) {

                    AuditLog.info(customerId = customer.id, message = "Approved for region - $regionCode")

                    onRegionApprovedAction.apply(
                            customer = customer,
                            regionCode = regionCode,
                            transaction = PrimeTransaction(transaction)
                    ).bind()

                    utcTimeNow()
                } else {
                    existingCustomerRegion.approvedOn
                }

                // Save KYC expiry date if it is not null.
                val newKycExpiryDateMap = kycExpiryDate
                        ?.let { existingCustomerRegion.kycExpiryDateMap.copy(key = kycType, value = it) }
                        ?: existingCustomerRegion.kycExpiryDateMap

                customerRegionRelationStore
                        .createOrUpdate(
                                fromId = customer.id,
                                relation = CustomerRegion(
                                        status = newRegionStatus,
                                        kycStatusMap = newKycStatusMap,
                                        kycExpiryDateMap = newKycExpiryDateMap,
                                        initiatedOn = existingCustomerRegion.initiatedOn,
                                        approvedOn = regionApprovedOn
                                ),
                                toId = regionCode,
                                transaction = transaction)
                        .bind()

            }.fix()
        }.unsafeRunSync()
    }

    private fun getKycStatusMapForRegion(regionCode: String): Map<KycType, KycStatus> {
        return when (regionCode) {
            "sg" -> setOf(JUMIO, MY_INFO, NRIC_FIN, ADDRESS)
            "my" -> setOf(JUMIO, ADDRESS)
            else -> setOf(JUMIO)
        }.map { it to KycStatus.PENDING }.toMap()
    }

    private fun getApprovedKycTypeSetList(regionCode: String): List<Set<KycType>> {
        return when (regionCode) {
            "sg" -> listOf(
                    setOf(MY_INFO, ADDRESS),
                    setOf(JUMIO, ADDRESS)
            )
            "my" -> listOf(setOf(JUMIO, ADDRESS))
            else -> listOf(setOf(JUMIO))
        }
    }

    // ------------
    // Admin Store
    // ------------

    override fun approveRegionForCustomer(
            customerId: String,
            regionCode: String): Either<StoreError, Unit> = writeTransaction {

        AuditLog.info(customerId = customerId, message = "Approved for region - $regionCode by Admin")

        // create, but fail if already exists
        customerRegionRelationStore.create(
                fromId = customerId,
                relation = CustomerRegion(
                        status = APPROVED,
                        kycStatusMap = getKycStatusMapForRegion(regionCode = regionCode)
                ),
                toId = regionCode,
                transaction = transaction
        ).flatMapLeft { storeError ->
            // if already exists
            if(storeError is AlreadyExistsError) {
                // then get the stored relation value
                customerRegionRelationStore.get(
                        fromId = customerId,
                        toId = regionCode,
                        transaction = transaction)
                        .map { customerRegion ->
                            if (customerRegion.status != APPROVED) {
                                // and store the updated relation value
                                customerRegionRelationStore.createOrUpdate(
                                        fromId = customerId,
                                        relation = customerRegion.copy(status = APPROVED), // with only status modified to APPROVED
                                        toId = regionCode,
                                        transaction = transaction)
                            }
                        }
            } else {
                // return other store errors which are not AlreadyExistsError
                storeError.left()
            }
        }
    }

    private inline fun <LEFT, RIGHT, NEWLEFT> EitherOf<LEFT, RIGHT>.flatMapLeft(f: (LEFT) -> Either<NEWLEFT, RIGHT>): Either<NEWLEFT, RIGHT> =
            fix().let {
                when (it) {
                    is Right -> it
                    is Left -> f(it.a)
                }
            }

    //
    // Balance (Customer - Subscription - Bundle)
    //

    override fun getCustomersForMsisdn(msisdn: String): Either<StoreError, Collection<Customer>> = readTransaction {
        get(Customer withSubscription (Subscription withMsisdn msisdn))
    }

    override fun getAnyIdentityForCustomerId(id: String): Either<StoreError, ModelIdentity> = readTransaction {
        read("""
                MATCH (:${customerEntity.name} { id:'$id' })<-[r:${identifiesRelation.name}]-(identity:${identityEntity.name})
                RETURN identity, r.provider as provider
                """.trimIndent(),
                transaction) {
            if (it.hasNext()) {
                val record = it.list().first()
                val identity = identityEntity.createEntity(record.get("identity").asMap())
                val provider = record.get("provider").asString()
                Either.right(ModelIdentity(id = identity.id, type = identity.type, provider = provider))
            } else {
                Either.left(NotFoundError(type = customerEntity.name, id = id))
            }
        }
    }

    override fun getIdentitiesFor(queryString: String): Either<StoreError, Collection<ModelIdentity>> = readTransaction {
        val parameters: Map<String, Any> = mapOf("queryString" to queryString.toLowerCase())
        read(query = """
                MATCH (c:${customerEntity.name})<-[r:${identifiesRelation.name}]-(identity:${identityEntity.name})
                WHERE toLower(c.contactEmail) contains ${'$'}queryString or toLower(c.nickname) contains ${'$'}queryString or toLower(c.id) contains ${'$'}queryString
                RETURN identity, r.provider as provider
                """.trimIndent(),
                transaction = transaction,
                parameters = parameters) {
            if (it.hasNext()) {
                val identityList = mutableListOf<ModelIdentity>()
                it.forEach { record ->
                    val identity = identityEntity.createEntity(record.get("identity").asMap())
                    val provider = record.get("provider").asString()
                    identityList.add(ModelIdentity(id = identity.id, type = identity.type, provider = provider))
                }
                Either.right(identityList)
            } else {
                Either.left(NotFoundError(type = customerEntity.name, id = queryString))
            }
        }
    }

    override fun getAllIdentities(): Either<StoreError, Collection<ModelIdentity>> = readTransaction {
        read("""
                MATCH (:${customerEntity.name})<-[r:${identifiesRelation.name}]-(identity:${identityEntity.name})
                RETURN identity, r.provider as provider
                """.trimIndent(),
                transaction) { statementResult ->
            statementResult.list { record ->
                val identity = identityEntity.createEntity(record.get("identity").asMap())
                val provider = record.get("provider").asString()
                ModelIdentity(id = identity.id, type = identity.type, provider = provider)
            }.right()
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
                MATCH (customer:${customerEntity.name})<-[:${forPurchaseByRelation.name}]-(purchaseRecord:${purchaseRecordEntity.name})
                WHERE purchaseRecord.`product/price/amount` > 0
                RETURN count(customer) AS count
                """.trimIndent(), transaction) { result ->
            result.single().get("count").asLong()
        }
    }

    //
    // For plans and subscriptions
    //

    override fun getPlan(planId: String): Either<StoreError, Plan> = readTransaction {
        get(Plan withId planId)
    }

    override fun getPlans(identity: ModelIdentity): Either<StoreError, List<Plan>> = readTransaction {
        getCustomerId(identity = identity)
                .flatMap { customerId ->
                    get(Plan forCustomer (Customer withId customerId))
                }
    }

    override fun createPlan(
            plan: Plan,
            stripeProductName: String,
            planProduct: Product): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {

                get(Product withSku plan.id)
                        .map {
                            AlreadyExistsError(type = productEntity.name, id = "Failed to find product associated with plan ${plan.id}")
                                    .left()
                                    .bind()
                        }

                get(Plan withId plan.id)
                        .map {
                            AlreadyExistsError(type = planEntity.name, id = "Failed to find plan ${plan.id}")
                                    .left()
                                    .bind()
                        }

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
                get(Plan withId plan.id)
                        .bind()
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun deletePlan(planId: String): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val plan = get(Plan withId planId)
                        .bind()
                /* The name of the product is the same as the name of the corresponding plan. */
                get(Product withSku planId)
                        .bind()

                /* Not removing the product due to purchase references. */

                /* Removing the plan will remove the plan itself and all relations going to it. */
                delete(Plan withId plan.id)
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
            identity: ModelIdentity,
            planId: String,
            trialEnd: Long): Either<StoreError, Unit> = writeTransaction {

        IO {
            Either.monad<StoreError>().binding {

                val customer = getCustomer(identity = identity)
                        .bind()

                val product = getProduct(identity, planId)
                        .bind()

                subscribeToPlan(
                        customerId = customer.id,
                        planId = planId,
                        taxRegionId = product.paymentTaxRegionId)
                        .bind()

                Unit
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun unsubscribeFromPlan(identity: ModelIdentity, planId: String, invoiceNow: Boolean): Either<StoreError, Plan> = readTransaction {
        getCustomerId(identity = identity)
                .flatMap {
                    removeSubscription(it, planId, invoiceNow)
                }
    }

    private fun removeSubscription(customerId: String, planId: String, invoiceNow: Boolean): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val plan = get(Plan withId planId)
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
            currency: String): Either<StoreError, Plan> = writeTransaction {
        IO {
            Either.monad<StoreError>().binding {
                val product = get(Product withSku sku).bind()
                val plan = get(Plan withId sku).bind()
                val purchaseRecord = PurchaseRecord(
                        id = chargeId,
                        product = product,
                        timestamp = Instant.now().toEpochMilli(),
                        properties = mapOf("invoiceId" to invoiceId)
                )

                /* Will exit if an existing purchase record matches on 'invoiceId'. */
                createPurchaseRecord(customerId, purchaseRecord)
                        .bind()

                // FIXME Moving customer to new segments should be done only based on productClass.
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
    // For verifying payment transactions
    //

    override fun getPaymentTransactions(start: Long, end: Long): Either<PaymentError, List<PaymentTransactionInfo>> =
            paymentProcessor.getPaymentTransactions(start, end)

    override fun getPurchaseTransactions(start: Long, end: Long): Either<StoreError, List<PurchaseRecord>> = readTransaction {
        read("""
                MATCH(pr:${purchaseRecordEntity.name})-[:${forPurchaseOfRelation.name}]->(:${productEntity.name})
                WHERE toInteger(pr.timestamp) >= ${start} AND toInteger(pr.timestamp) <= ${end} AND toInteger(pr.`product/price/amount`) > 0
                RETURN pr
                """.trimIndent(), transaction) { statementResult ->
            statementResult.list { record ->
                purchaseRecordEntity.createEntity(record["pr"].asMap())
            }.right()
        }
    }

    override fun checkPaymentTransactions(start: Long, end: Long): Either<PaymentError, List<Map<String, Any?>>> = readTransaction {
        IO {
            Either.monad<PaymentError>().binding {

                /* To account for time differences for when a payment transaction is stored
                   to payment backend and to the purchase record DB, the search for
                   corresponding payment and purchase records are done with a wider time range
                   that what is specificed with the start and end timestamps.

                   When all common records has been removed using, the remaining records, if any,
                   are then againg checked for records that lies excactly within the start and
                   end timestamps. */

                val padding = 600000L   /* 10 min in milliseconds. */

                val startPadded = if (start < padding) start else start - padding
                val endPadded = end + padding

                val purchaseRecords = getPurchaseTransactions(startPadded, endPadded)
                        .mapLeft {
                            BadGatewayError("Error when fetching purchase records",
                                    error = it)
                        }.bind()
                val paymentRecords = getPaymentTransactions(startPadded, endPadded)
                        .bind()

                /* TODO: For handling amounts and currencies consider to use
                         JSR-354 Currency and Money API. */

                purchaseRecords.map {
                    mapOf("type" to "purchaseRecord",
                            "chargeId" to it.id,
                            "amount" to it.product.price.amount,
                            "currency" to it.product.price.currency.toLowerCase(),
                            "refunded" to (it.refund != null),
                            "created" to it.timestamp)
                }.plus(
                        paymentRecords.map {
                            mapOf("type" to "paymentRecord",
                                    "chargeId" to it.id,
                                    "amount" to it.amount,
                                    "currency" to it.currency,   /* (Stripe) Always lower case. */
                                    "refunded" to it.refunded,
                                    "created" to it.created)
                        }
                ).groupBy {
                    it["chargeId"].hashCode() + it["amount"].hashCode() +
                            it["currency"].hashCode() + it["refunded"].hashCode()
                }.map {
                    /* Report if payment backend and/or purchase record store have
                       duplicates or more of the same transaction. */
                    if (it.value.size > 2)
                        logger.error(NOTIFY_OPS_MARKER,
                                "${it.value.size} duplicates found for payment transaction/purchase record ${it.value.first()["chargeId"]}")
                    it
                }.filter {
                    it.value.size == 1
                }.map {
                    it.value.first()
                }.filter {
                    /* Filter down to records that lies excactly within the start
                       and end timestamps. */
                    val ts = it["created"] as? Long ?: 0L

                    if (ts == 0L) {
                        logger.error(NOTIFY_OPS_MARKER, if (it["type"] == "purchaseRecord")
                            "Purchase record ${it["chargeId"]} has 'created' timestamp set to 0"
                        else
                            "Payment transaction record ${it["chargeId"]} has 'created' timestamp set to 0")
                        true
                    } else {
                        ts >= start && ts <= end
                    }
                }.map {
                    logger.error(NOTIFY_OPS_MARKER, if (it["type"] == "purchaseRecord")
                        "Found no matching payment transaction record for purchase record ${it["chargeId"]}"
                    else
                        "Found no matching purchase record for payment transaction record ${it["chargeId"]}")
                    it
                }
            }.fix()
        }.unsafeRunSync()
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

    override fun refundPurchase(
            identity: ModelIdentity,
            purchaseRecordId: String,
            reason: String): Either<PaymentError, ProductInfo> = writeTransaction {
        IO {
            Either.monad<PaymentError>().binding {
                val (_, customerAnalyticsId) = getCustomerAndAnalyticsId(identity = identity)
                        .mapLeft {
                            logger.error("Failed to find customer with identity - $identity")
                            NotFoundPaymentError("Failed to find customer with identity - $identity",
                                    error = it)
                        }.bind()
                val purchaseRecord = get(PurchaseRecord withId purchaseRecordId)
                        // If we can't find the record, return not-found
                        .mapLeft {
                            org.ostelco.prime.paymentprocessor.core.NotFoundError("Purchase Record unavailable",
                                    error = it)
                        }.bind()
                checkPurchaseRecordForRefund(purchaseRecord)
                        .bind()

                // TODO: (kmm) Move this last.
                val refundId = paymentProcessor.refundCharge(
                        purchaseRecord.id,
                        purchaseRecord.product.price.amount)
                        .bind()
                val refund = RefundRecord(refundId, reason, Instant.now().toEpochMilli())
                val changedPurchaseRecord = purchaseRecord.copy(
                        refund = refund
                )
                update { changedPurchaseRecord }
                        .mapLeft {
                            logger.error("failed to update purchase record, for refund $refund.id, chargeId $purchaseRecordId, payment has been refunded in Stripe")
                            BadGatewayError("Failed to update purchase record for refund ${refund.id}",
                                    error = it)
                        }.bind()

                analyticsReporter.reportRefund(
                        customerAnalyticsId = customerAnalyticsId,
                        purchaseId = purchaseRecord.id,
                        reason = reason
                )

                ProductInfo(purchaseRecord.product.sku)
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    //
    // Stores
    //

    private val offerEntity = Offer::class.entityType

    private val segmentEntity = Segment::class.entityType

    private val offerToSegmentRelation = RelationType(OFFERED_TO_SEGMENT, offerEntity, segmentEntity, None::class.java)
    private val offerToSegmentStore = RelationStore(offerToSegmentRelation)

    private val offerToProductRelation = RelationType(OFFER_HAS_PRODUCT, offerEntity, productEntity, None::class.java)
    private val offerToProductStore = RelationStore(offerToProductRelation)

    val customerToSegmentRelation = RelationType(BELONG_TO_SEGMENT, customerEntity, segmentEntity, None::class.java)
    private val customerToSegmentStore = RelationStore(customerToSegmentRelation)

    //
    // Segment
    //
    override fun createSegment(segment: ModelSegment): Either<StoreError, Unit> = writeTransaction {
        createSegment(segment)
                .ifFailedThenRollback(transaction)
    }

    private fun WriteTransaction.createSegment(segment: ModelSegment): Either<StoreError, Unit> =
            create {
                Segment(id = segment.id)
            }.flatMap {
                customerToSegmentStore.create(segment.subscribers, segment.id, transaction)
            }

    override fun updateSegment(segment: ModelSegment): Either<StoreError, Unit> = writeTransaction {
        updateSegment(segment, transaction)
                .ifFailedThenRollback(transaction)
    }

    private fun updateSegment(segment: ModelSegment, transaction: Transaction): Either<StoreError, Unit> {
        return customerToSegmentStore.removeAll(toId = segment.id, transaction = transaction)
                .flatMap { customerToSegmentStore.create(segment.subscribers, segment.id, transaction) }
    }

    //
    // Offer
    //
    override fun createOffer(offer: ModelOffer): Either<StoreError, Unit> = writeTransaction {
        createOffer(offer)
                .ifFailedThenRollback(transaction)
    }

    private fun WriteTransaction.createOffer(offer: ModelOffer): Either<StoreError, Unit> {
        return create { Offer(id = offer.id) }
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
            offer: ModelOffer,
            segments: Collection<ModelSegment>,
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

        val actualOffer = ModelOffer(
                id = offer.id,
                products = productIds,
                segments = segmentIds)

        IO {
            Either.monad<StoreError>().binding {

                products.forEach { product -> create { product }.bind() }
                segments.forEach { segment -> create { Segment(id = segment.id) }.bind() }
                createOffer(actualOffer).bind()

            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    /**
     * Create Segments
     */
    override fun atomicCreateSegments(createSegments: Collection<ModelSegment>): Either<StoreError, Unit> = writeTransaction {

        IO {
            Either.monad<StoreError>().binding {
                createSegments.forEach { segment -> createSegment(segment).bind() }
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    /**
     * Update segments
     */
    override fun atomicUpdateSegments(updateSegments: Collection<ModelSegment>): Either<StoreError, Unit> = writeTransaction {

        IO {
            Either.monad<StoreError>().binding {
                updateSegments.forEach { segment -> updateSegment(segment).bind() }
            }.fix()
        }.unsafeRunSync()
                .ifFailedThenRollback(transaction)
    }

    override fun atomicAddToSegments(addToSegments: Collection<ModelSegment>): Either<StoreError, Unit> {
        TODO()
    }

    override fun atomicRemoveFromSegments(removeFromSegments: Collection<ModelSegment>): Either<StoreError, Unit> {
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
        write(query = "CREATE INDEX ON :${customerEntity.name}(id)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${productEntity.name}(id)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${productEntity.name}(sku)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${subscriptionEntity.name}(id)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${subscriptionEntity.name}(msisdn)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${bundleEntity.name}(id)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${simProfileEntity.name}(id)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${planEntity.name}(id)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${regionEntity.name}(id)", transaction = transaction) {}
        write(query = "CREATE INDEX ON :${scanInformationEntity.name}(id)", transaction = transaction) {}
    }
}

fun <K, V> Map<K, V>.copy(key: K, value: V): Map<K, V> {
    val mutableMap = this.toMutableMap()
    mutableMap[key] = value
    return mutableMap.toMap()
}

fun utcTimeNow() = ZonedDateTime.now(ZoneOffset.UTC).toString()