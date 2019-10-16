package org.ostelco.prime.support.resources

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorCode.FAILED_TO_FETCH_AUDIT_LOGS
import org.ostelco.prime.apierror.ApiErrorMapper
import org.ostelco.prime.apierror.InternalServerError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.apierror.responseBuilder
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Context
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.AuditLogStore
import java.util.regex.Pattern
import javax.validation.constraints.NotNull
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Resource used to handle the profile related REST calls.
 */
@Path("/support/profiles")
class ProfilesResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get the subscriber profile.
     */
    @GET
    @Path("{query}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getProfile(@Auth token: AccessTokenPrincipal?,
                   @NotNull
                   @PathParam("query")
                   query: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                if (!isEmail(query)) {
                    logger.info("${token.name} Accessing profile for msisdn:$query")
                    getProfileListForMsisdn(query)
                            .responseBuilder()
                } else {
                    logger.info("${token.name} Accessing profile for email:$query")
                    getProfileList(contactEmail = query)
                            .responseBuilder()
                }
            }.build()

    /**
     * Get the subscriptions for this subscriber.
     */
    @GET
    @Path("{id}/subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSubscriptions(@Auth token: AccessTokenPrincipal?,
                         @NotNull
                         @PathParam("id")
                         id: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing subscriptions for customerId: $id")
                getSubscriptions(customerId = id)
                        .responseBuilder()
            }.build()


    /**
     * Get all the eKYC scan information for this subscriber.
     */
    @GET
    @Path("{id}/scans")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllScanInformation(@Auth token: AccessTokenPrincipal?,
                              @NotNull
                              @PathParam("id")
                              id: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing scan information for customerId: $id")
                getAllScanInformation(customerId = id)
                        .responseBuilder()
            }.build()

    private fun getAllScanInformation(customerId: String): Either<ApiError, Collection<ScanInformation>> {
        return try {
            storage.getIdentityForCustomerId(id = customerId).flatMap { identity: Identity ->
                storage.getAllScanInformation(identity = identity)
            }.mapLeft {
                NotFoundError("Failed to fetch scan information.", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch scan information for customer with customerId: $customerId", e)
            Either.left(NotFoundError("Failed to fetch scan information", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION))
        }
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getProfileList(contactEmail: String): Either<ApiError, Collection<Customer>> {
        return try {
            storage.getIdentitiesForContactEmail(contactEmail = contactEmail).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER, it)
            }.flatMap { identityList ->
                val customerList = identityList.mapNotNull { identity: Identity ->
                    storage.getCustomer(identity).fold(
                            {
                                logger.error("Error fetching customer for $identity,  $it")
                                null
                            },
                            { it })
                }
                Either.right(customerList)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for customer with contactEmail - $contactEmail", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER))
        }
    }

    private fun isEmail(email: String): Boolean {
        val regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"
        val pattern = Pattern.compile(regex)
        return pattern.matcher(email).matches()
    }

    private fun getProfileListForMsisdn(msisdn: String): Either<ApiError, Collection<Customer>> {
        return try {
            storage.getCustomerForMsisdn(msisdn).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER, it)
            }.map {
                listOf(it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for msisdn $msisdn", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER))
        }
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getSubscriptions(customerId: String): Either<ApiError, Collection<Subscription>> {
        return try {
            storage.getIdentityForCustomerId(id = customerId).flatMap { identity: Identity ->
                storage.getSubscriptions(identity)
            }.mapLeft {
                NotFoundError("Failed to get subscriptions.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for customer with customerId: - $customerId", e)
            InternalServerError("Failed to get subscriptions", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS).left()
        }
    }
}

/**
 * Resource used to handle bundles related REST calls.
 */
@Path("/support/bundles")
class BundlesResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get all bundles for the subscriber.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getBundlesById(@Auth token: AccessTokenPrincipal?,
                       @NotNull
                       @PathParam("id")
                       id: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing bundles for customerId: $id")
                getBundles(customerId = id)
                        .responseBuilder()
            }.build()

    // TODO: Reuse the one from SubscriberDAO
    private fun getBundles(customerId: String): Either<ApiError, Collection<Bundle>> {
        return try {
            storage.getIdentityForCustomerId(id = customerId).flatMap { identity: Identity ->
                storage.getBundles(identity)
            }.mapLeft {
                NotFoundError("Failed to get bundles. ${it.message}", ApiErrorCode.FAILED_TO_FETCH_BUNDLES)
            }
        } catch (e: Exception) {
            logger.error("Failed to get bundles for customer with  customerId: $customerId", e)
            Either.left(NotFoundError("Failed to get bundles", ApiErrorCode.FAILED_TO_FETCH_BUNDLES))
        }
    }
}

/**
 * Resource used to handle purchase related REST calls.
 */
@Path("/support/purchases")
class PurchaseResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get all purchase history for the subscriber.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPurchaseHistoryByEmail(@Auth token: AccessTokenPrincipal?,
                                  @NotNull
                                  @PathParam("id")
                                  id: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing bundles for customerId: $id")
                getPurchaseHistory(customerId = id)
                        .responseBuilder()
            }.build()

    // TODO: Reuse the one from SubscriberDAO
    private fun getPurchaseHistory(customerId: String): Either<ApiError, Collection<PurchaseRecord>> {
        return try {
            storage.getIdentityForCustomerId(id = customerId).flatMap { identity: Identity ->
                storage.getPurchaseRecords(identity)
            }.bimap(
                    { NotFoundError("Failed to get purchase history.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY, it) },
                    { it.toList() })
        } catch (e: Exception) {
            logger.error("Failed to get purchase history for customer with customerId - $customerId", e)
            Either.left(InternalServerError("Failed to get purchase history", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY))
        }
    }
}

/**
 * Resource used to handle refund related REST calls.
 */
@Path("/support/refund")
class RefundResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Refund a specified purchase for the subscriber.
     */
    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun refundPurchaseById(@Auth token: AccessTokenPrincipal?,
                           @NotNull
                           @PathParam("id")
                           id: String,
                           @NotNull
                           @QueryParam("purchaseRecordId")
                           purchaseRecordId: String,
                           @NotNull
                           @QueryParam("reason")
                           reason: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Refunding purchase for customerId: $id at id: $purchaseRecordId")
                refundPurchase(id, purchaseRecordId, reason)
                        .map {
                            logger.info(NOTIFY_OPS_MARKER, "${token.name} refunded the purchase (id:$purchaseRecordId) for customerId: $id ")
                            it
                        }
                        .responseBuilder()
            }.build()

    private fun refundPurchase(customerId: String, purchaseRecordId: String, reason: String): Either<ApiError, ProductInfo> {
        return try {
            storage.getIdentityForCustomerId(id = customerId).flatMap { identity: Identity ->
                storage.refundPurchase(identity, purchaseRecordId, reason)
            }.mapLeft {
                when (it) {
                    is ForbiddenError -> org.ostelco.prime.apierror.ForbiddenError("Failed to refund purchase. ${it.description}", ApiErrorCode.FAILED_TO_REFUND_PURCHASE)
                    else -> NotFoundError("Failed to refund purchase. ${it.toString()}", ApiErrorCode.FAILED_TO_REFUND_PURCHASE)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to refund purchase for customer with customerId - $customerId, id: $purchaseRecordId", e)
            Either.left(InternalServerError("Failed to refund purchase", ApiErrorCode.FAILED_TO_REFUND_PURCHASE))
        }
    }
}

/**
 * Resource used to handle context REST call.
 */
@Path("/support/context")
class ContextResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get context for the subscriber.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getContextById(@Auth token: AccessTokenPrincipal?,
                       @NotNull
                       @PathParam("id")
                       id: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing context for customer with id: $id")
                getContext(customerId = id)
                        .responseBuilder()
            }.build()

    // TODO: Reuse the one from SubscriberDAO
    private fun getContext(customerId: String): Either<ApiError, Context> {
        return try {
            storage.getIdentityForCustomerId(id = customerId).flatMap { identity: Identity ->
                storage.getCustomer(identity).map { customer ->
                    storage.getAllRegionDetails(identity = identity)
                            .fold(
                                    { Context(customer = customer) },
                                    { regionDetailsCollection -> Context(customer = customer, regions = regionDetailsCollection) })
                }
            }.mapLeft {
                NotFoundError("Failed to fetch customer.", ApiErrorCode.FAILED_TO_FETCH_CONTEXT, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch context for customer with id - $customerId", e)
            Either.left(NotFoundError("Failed to fetch context", ApiErrorCode.FAILED_TO_FETCH_CONTEXT))
        }
    }
}

/**
 * Resource used to handle notification related REST calls.
 */
@Path("/support/notify")
class NotifyResource {
    private val logger by getLogger()
    private val notifier by lazy { getResource<AppNotifier>() }

    /**
     * Sends a notification to all devices for a subscriber.
     */
    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun sendNotificationByEmail(@Auth token: AccessTokenPrincipal?,
                                @NotNull
                                @PathParam("id")
                                id: String,
                                @NotNull
                                @QueryParam("title")
                                title: String,
                                @NotNull
                                @QueryParam("message")
                                message: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Sending notification to customerId: $id")
                val data = mapOf("timestamp" to "${System.currentTimeMillis()}")
                notifier.notify(id, title, message, data)
                id.right().responseBuilder("Message Sent")
            }.build()
}


/**
 * Resource used to handle audit log related REST calls.
 */
@Path("/support/auditLog")
class AuditLogResource {
    private val logger by getLogger()
    private val auditLogStore by lazy { getResource<AuditLogStore>() }

    /**
     * Fetch all audit logs for a subscriber.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun query(@Auth token: AccessTokenPrincipal?,
              @NotNull
              @PathParam("id")
              id: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} fetching audit log of customerId: $id")
                auditLogStore.getCustomerActivityHistory(customerId = id)
                        .mapLeft { errorMessage ->
                            InternalServerError(errorMessage, FAILED_TO_FETCH_AUDIT_LOGS)
                        }.responseBuilder()
            }.build()
}

/**
 * Resource used to manipulate customer record.
 */
@Path("/support/customer")
class CustomerResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Remove customer from Prime.
     */
    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun removeCustomer(@Auth token: AccessTokenPrincipal?,
                       @NotNull
                       @PathParam("id")
                       id: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Removing the customer for customerId: $id")
                removeCustomer(customerId = id)
                        .responseBuilder(Response.Status.NO_CONTENT)
            }.build()

    // TODO: Reuse the one from SubscriberDAO
    private fun removeCustomer(customerId: String): Either<ApiError, Unit> {
        return try {
            storage.getIdentityForCustomerId(id = customerId).flatMap { identity: Identity ->
                storage.removeCustomer(identity)
            }.mapLeft {
                NotFoundError("Failed to remove customer.", ApiErrorCode.FAILED_TO_REMOVE_CUSTOMER, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for customer with customerId - $customerId", e)
            Either.left(NotFoundError("Failed to remove customer", ApiErrorCode.FAILED_TO_REMOVE_CUSTOMER))
        }
    }
}
