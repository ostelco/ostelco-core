package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import arrow.core.right
import com.stripe.exception.ApiConnectionException
import com.stripe.exception.AuthenticationException
import com.stripe.exception.CardException
import com.stripe.exception.InvalidRequestException
import com.stripe.exception.RateLimitException
import com.stripe.exception.StripeException
import com.stripe.model.Card
import com.stripe.model.Charge
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.Invoice
import com.stripe.model.InvoiceItem
import com.stripe.model.PaymentSource
import com.stripe.model.Plan
import com.stripe.model.Product
import com.stripe.model.Refund
import com.stripe.model.Source
import com.stripe.model.Subscription
import com.stripe.model.TaxRate
import com.stripe.net.RequestOptions
import jdk.jfr.Percentage
import org.ostelco.prime.getLogger
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.InvoiceInfo
import org.ostelco.prime.paymentprocessor.core.InvoiceItemInfo
import org.ostelco.prime.paymentprocessor.core.NotFoundError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.PaymentStatus
import org.ostelco.prime.paymentprocessor.core.PlanInfo
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.paymentprocessor.core.SourceDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SourceInfo
import org.ostelco.prime.paymentprocessor.core.SubscriptionDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SubscriptionInfo
import org.ostelco.prime.paymentprocessor.core.TaxRateInfo
import java.math.BigDecimal
import java.time.Instant
import java.util.*


class StripePaymentProcessor : PaymentProcessor {

    private val logger by getLogger()

    override fun getSavedSources(stripeCustomerId: String): Either<PaymentError, List<SourceDetailsInfo>> =
            either("Failed to retrieve sources for customer $stripeCustomerId") {
                val customer = Customer.retrieve(stripeCustomerId)
                val sources: List<SourceDetailsInfo> = customer.sources.data.map {
                    val details = getAccountDetails(it)
                    SourceDetailsInfo(it.id, getAccountType(details), details)
                }
                sources.sortedByDescending { it.details["created"] as Long }
            }

    private fun getAccountType(details: Map<String, Any>): String {
        return details["type"].toString()
    }

    /* Returns 'source' details for the given Stripe source/account.
       Note that including the fields 'id', 'type' and 'created' are mandatory. */
    private fun getAccountDetails(paymentSource: PaymentSource): Map<String, Any> =
            when (paymentSource) {
                is Card -> {
                    mapOf("id" to paymentSource.id,
                            "type" to "card",
                            "addressLine1" to paymentSource.addressLine1,
                            "addressLine2" to paymentSource.addressLine2,
                            "addressZip" to paymentSource.addressZip,
                            "addressCity" to paymentSource.addressCity,
                            "addressState" to paymentSource.addressState,
                            "brand" to paymentSource.brand,              // "Visa", "Mastercard" etc.
                            "country" to paymentSource.country,
                            "currency" to paymentSource.currency,
                            "cvcCheck" to paymentSource.cvcCheck,
                            "created" to getCreatedTimestampFromMetadata(paymentSource.id,
                                    paymentSource.metadata),
                            "expMonth" to paymentSource.expMonth,
                            "expYear" to paymentSource.expYear,
                            "fingerprint" to paymentSource.fingerprint,
                            "funding" to paymentSource.funding,
                            "last4" to paymentSource.last4)              // Typ.: "credit" or "debit"
                            .filterValues { it != null }
                }
                is Source -> {
                    mapOf("id" to paymentSource.id,
                            "type" to "source",
                            "created" to paymentSource.created,
                            "owner" to paymentSource.owner,
                            "threeDSecure" to paymentSource.threeDSecure)
                }
                else -> {
                    logger.error("Received unsupported Stripe source/account type: {}",
                            paymentSource)
                    mapOf("id" to paymentSource.id,
                            "type" to "unsupported",
                            "created" to getSecondsSinceEpoch())
                }
            }

    /* Handle type conversion when reading the 'created' field from the
       metadata returned from Stripe. (It might seem like that Stripe
       returns stored metadata values as strings, even if they where stored
       using an another type. Needs to be verified.) */
    private fun getCreatedTimestampFromMetadata(id: String, metadata: Map<String, Any>): Long {
        val created: String? = metadata["created"] as? String
        return created?.toLongOrNull() ?: run {
            logger.warn("No 'created' timestamp found in metadata for Stripe account {}",
                    id)
            getSecondsSinceEpoch()
        }
    }

    /* Seconds since Epoch in UTC zone. */
    private fun getSecondsSinceEpoch(): Long {
        return System.currentTimeMillis() / 1000L
    }

    override fun createPaymentProfile(customerId: String, email: String): Either<PaymentError, ProfileInfo> =
            either("Failed to create profile for user $customerId") {
                val customerParams = mapOf(
                        "id" to customerId,
                        "email" to email,
                        "metadata" to mapOf("customerId" to customerId))
                ProfileInfo(Customer.create(customerParams).id)
            }

    override fun getPaymentProfile(customerId: String): Either<PaymentError, ProfileInfo> =
            Try {
                Customer.retrieve(customerId)
            }.fold(
                    ifSuccess = { customer ->
                        when {
                            customer.deleted == true -> Either.left(NotFoundError("Payment profile for user $customerId was previously deleted"))
                            else -> Either.right(ProfileInfo(customer.id))
                        }
                    },
                    ifFailure = {
                        Either.left(NotFoundError("Could not find a payment profile for user $customerId"))
                    }
            )

    override fun createPlan(productId: String, amount: Int, currency: String, interval: PaymentProcessor.Interval, intervalCount: Long): Either<PaymentError, PlanInfo> =
            either("Failed to create plan for product $productId amount $amount currency $currency interval ${interval.value}") {
                val planParams = mapOf(
                        "product" to productId,
                        "amount" to amount,
                        "interval" to interval.value,
                        "interval_count" to intervalCount,
                        "currency" to currency)
                val plan = Plan.create(planParams)
                PlanInfo(plan.id)
            }

    override fun removePlan(planId: String): Either<PaymentError, PlanInfo> =
            either("Failed to delete plan $planId") {
                Plan.retrieve(planId).let { plan ->
                    plan.delete()
                    PlanInfo(plan.id)
                }
            }

    override fun createProduct(sku: String): Either<PaymentError, ProductInfo> =
            either("Failed to create product with sku $sku") {
                val productParams = mapOf(
                        "name" to sku,
                        "type" to "service")
                ProductInfo(Product.create(productParams).id)
            }

    override fun removeProduct(productId: String): Either<PaymentError, ProductInfo> =
            either("Failed to delete product $productId") {
                val product = Product.retrieve(productId)
                ProductInfo(product.delete().id)
            }

    override fun addSource(stripeCustomerId: String, stripeSourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to add source $stripeSourceId to customer $stripeCustomerId") {
                val customer = Customer.retrieve(stripeCustomerId)
                val sourceParams = mapOf("source" to stripeSourceId,
                        "metadata" to mapOf("created" to getSecondsSinceEpoch()))
                SourceInfo(customer.sources.create(sourceParams).id)
            }

    override fun setDefaultSource(stripeCustomerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to set default source $sourceId for customer $stripeCustomerId") {
                val customer = Customer.retrieve(stripeCustomerId)
                val updateParams = mapOf("default_source" to sourceId)
                val customerUpdated = customer.update(updateParams)
                SourceInfo(customerUpdated.defaultSource)
            }

    override fun getDefaultSource(stripeCustomerId: String): Either<PaymentError, SourceInfo> =
            either("Failed to get default source for customer $stripeCustomerId") {
                SourceInfo(Customer.retrieve(stripeCustomerId).defaultSource)
            }

    override fun deletePaymentProfile(stripeCustomerId: String): Either<PaymentError, ProfileInfo> =
            either("Failed to delete customer $stripeCustomerId") {
                val customer = Customer.retrieve(stripeCustomerId)
                ProfileInfo(customer.delete().id)
            }

    /* The 'expand' part will cause an immediate attempt at charging for the
       subscription when creating it. For interpreting the result see:
       https://stripe.com/docs/billing/subscriptions/payment#signup-3b */
    override fun createSubscription(planId: String, stripeCustomerId: String, trialEnd: Long): Either<PaymentError, SubscriptionDetailsInfo> =
            either("Failed to subscribe customer $stripeCustomerId to plan $planId") {
                val item = mapOf("plan" to planId)
                val subscriptionParams = mapOf(
                        "customer" to stripeCustomerId,
                        "items" to mapOf("0" to item),
                        *(if (trialEnd > Instant.now().epochSecond)
                            arrayOf("trial_end" to trialEnd.toString())
                        else
                            arrayOf("expand" to arrayOf("latest_invoice.payment_intent"))))
                val subscription = Subscription.create(subscriptionParams)
                val status = subscriptionStatus(subscription)
                SubscriptionDetailsInfo(id = subscription.id,
                        status = status.first,
                        invoiceId = status.second,
                        chargeId = status.third,
                        created = subscription.created,
                        trialEnd = subscription.trialEnd ?: 0L)
            }

    private fun subscriptionStatus(subscription: Subscription): Triple<PaymentStatus, String, String> {
        val invoice = subscription.latestInvoiceObject
        val intent = invoice?.paymentIntentObject

        return when (subscription.status) {
            "active", "incomplete" -> {
                if (intent != null)
                    Triple(when (intent.status) {
                        "succeeded" -> PaymentStatus.PAYMENT_SUCCEEDED
                        "requires_payment_method" -> PaymentStatus.REQUIRES_PAYMENT_METHOD
                        "requires_action" -> PaymentStatus.REQUIRES_ACTION
                        else -> throw RuntimeException(
                                "Unexpected intent ${intent.status} for Stipe payment invoice ${invoice.id}")
                    }, invoice.id, invoice.charge)
                else {
                    throw RuntimeException(
                            "'Intent' absent in response when creating subscription ${subscription.id} with status ${subscription.status}")
                }
            }
            "trialing" -> {
                Triple(PaymentStatus.TRIAL_START, "", "")
            }
            else -> {
                throw RuntimeException(
                        "Got unexpected status ${subscription.status} when creating subscription ${subscription.id}")
            }
        }
    }

    override fun cancelSubscription(subscriptionId: String, invoiceNow: Boolean): Either<PaymentError, SubscriptionInfo> =
            either("Failed to unsubscribe subscription Id : $subscriptionId with 'invoice-now' set to $invoiceNow") {
                val subscription = Subscription.retrieve(subscriptionId)
                val subscriptionParams = mapOf("invoice_now" to invoiceNow)
                subscription.cancel(subscriptionParams)
                SubscriptionInfo(id = subscription.id)
            }

    override fun authorizeCharge(customerId: String, sourceId: String?, amount: Int, currency: String): Either<PaymentError, String> {
        val errorMessage = "Failed to authorize the charge for customerId $customerId sourceId $sourceId amount $amount currency $currency"
        return when (amount) {
            0 -> Either.right("ZERO_CHARGE_${UUID.randomUUID()}")
            else -> either(errorMessage) {
                val chargeParams = mutableMapOf(
                        "amount" to amount,
                        "currency" to currency,
                        "customer" to customerId,
                        "capture" to false)
                if (sourceId != null) {
                    chargeParams["source"] = sourceId
                }
                Charge.create(chargeParams)
            }.flatMap { charge: Charge ->
                val review = charge.review
                Either.cond(
                        test = (review == null),
                        ifTrue = { charge.id },
                        ifFalse = { ForbiddenError("Review required, $errorMessage $review") }
                )
            }
        }
    }

    override fun captureCharge(chargeId: String, customerId: String, amount: Int, currency: String): Either<PaymentError, String> {
        val errorMessage = "Failed to capture charge for customerId $customerId chargeId $chargeId"
        return when (amount) {
            0 -> Either.right(chargeId)
            else -> either(errorMessage) {
                Charge.retrieve(chargeId)
            }.flatMap { charge: Charge ->
                val review = charge.review
                Either.cond(
                        test = (review == null),
                        ifTrue = { charge },
                        ifFalse = { ForbiddenError("Review required, $errorMessage $review") }
                )
            }.flatMap { charge ->
                try {
                    charge.capture()
                    Either.right(charge.id)
                } catch (e: Exception) {
                    logger.warn(errorMessage, e)
                    Either.left(BadGatewayError(errorMessage))
                }
            }
        }
    }

    override fun refundCharge(chargeId: String, amount: Int, currency: String): Either<PaymentError, String> =
            when (amount) {
                0 -> Either.right(chargeId)
                else -> either("Failed to refund charge $chargeId") {
                    val refundParams = mapOf("charge" to chargeId)
                    Refund.create(refundParams).id
                }
            }

    override fun removeSource(stripeCustomerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to remove source $sourceId for stripeCustomerId $stripeCustomerId") {
                val accountInfo = Customer.retrieve(stripeCustomerId).sources.retrieve(sourceId)
                when (accountInfo) {
                    is Card -> accountInfo.delete()
                    is Source -> accountInfo.detach()
                    else ->
                        Either.left(BadGatewayError("Attempt to remove unsupported account-type $accountInfo"))
                }
                SourceInfo(sourceId)
            }

    override fun getStripeEphemeralKey(customerId: String, email: String, apiVersion: String): Either<PaymentError, String> =
            getPaymentProfile(customerId)
                    .fold(
                            { createPaymentProfile(customerId, email) },
                            { profileInfo -> profileInfo.right() }
                    ).flatMap { profileInfo ->
                        either("Failed to create stripe ephemeral key") {
                            EphemeralKey.create(
                                    mapOf("customer" to profileInfo.id),
                                    RequestOptions.builder().setStripeVersionOverride(apiVersion).build())
                                    .rawJson
                        }
                    }

    override fun createInvoiceItem(customerId: String, amount: Int, currency: String, description: String): Either<PaymentError, InvoiceItemInfo> =
            either("Failed to create an invoice item for customer ${customerId} with Stripe") {
                val params = mapOf(
                        "customer" to customerId,
                        "amount" to amount,
                        "currency" to currency,
                        "description" to description)
                InvoiceItemInfo(InvoiceItem.create(params).id)
            }

    override fun removeInvoiceItem(invoiceItemId: String): Either<PaymentError, InvoiceItemInfo> =
            either("Failed to remove invoice item ${invoiceItemId} with Stripe") {
                val invoiceItem = InvoiceItem.retrieve(invoiceItemId).delete()
                InvoiceItemInfo(invoiceItem.id)
            }

    override fun createInvoice(customerId: String, taxRates: List<TaxRateInfo>): Either<PaymentError, InvoiceInfo> =
            either("Failed to create an invoice for ${customerId} with Stripe") {
                val params = mapOf(
                        "customer" to customerId,
                        "auto_advance" to true,
                        *(if (taxRates.isNotEmpty())
                            arrayOf("default_tax_rates" to taxRates.map { it.id })
                        else arrayOf())
                )
                InvoiceInfo(Invoice.create(params).id)
            }

    override fun createInvoice(customerId: String): Either<PaymentError, InvoiceInfo> =
            createInvoice(customerId, listOf<TaxRateInfo>())

    override fun createInvoice(customerId: String, region: String, amount: Int, currency: String, description: String): Either<PaymentError, InvoiceInfo> =
            createInvoiceItem(customerId, amount, currency, description)
                    .flatMap {
                        getTaxRatesForRegion(region)
                                .flatMap { taxRates -> createInvoice(customerId, taxRates) }
                    }

    override fun payInvoice(invoiceId: String): Either<PaymentError, InvoiceInfo> =
            either("Failed to complete payment of invoice ${invoiceId}") {
                InvoiceInfo(Invoice.retrieve(invoiceId).pay()
                        .id)
            }

    override fun removeInvoice(invoiceId: String): Either<PaymentError, InvoiceInfo> =
            either("Failed to remove invoice ${invoiceId} with Stripe") {
                val invoice = Invoice.retrieve(invoiceId)

                /* Wether an invoice can be deleted or not, depends on what
                   status the invoice has.
                   Ref.: https://stripe.com/docs/billing/invoices/workflow */
                when (invoice.status) {
                    "draft" -> {
                        val items = invoice.lines.list(emptyMap()).data

                        items.forEach {
                            InvoiceItem.retrieve(it.id).delete()
                        }
                        invoice.delete()
                    }
                    "open", "uncollectible" -> {
                        invoice.voidInvoice()
                    }
                    "paid", "void" -> {
                        /* Nothing to do. */
                    }
                    else -> {
                        logger.error("Unexpected invoice status {} when attempting to delete invoice {}",
                                invoice.status, invoice.id)
                    }
                }
                InvoiceInfo(invoice.id)
            }

    /* NOTE! This method creates a 'tax rate' with Stipe, unless there already
             exists a 'tax-rate' with the same information. */
    override fun createTaxRateForRegion(regionCode: String, percentage: BigDecimal, displayName: String, inclusive: Boolean): Either<PaymentError, TaxRateInfo> =
            getTaxRatesForRegion(regionCode)
                    .flatMap { taxRates ->
                        val match = taxRates.find {
                            it.percentage == percentage && it.displayName == displayName && it.inclusive == inclusive
                        }

                        if (match == null) {
                            val param = mapOf(
                                    "percentage" to percentage,
                                    "display_name" to displayName,
                                    "inclusive" to inclusive,
                                    "metadata" to mapOf("region-code" to regionCode))
                            TaxRateInfo(TaxRate.create(param).id, percentage, displayName, inclusive)
                        } else {
                            match
                        }.right()
                    }

    /* TODO: (kmm) Will have to come up with a "scheme" for finding the correct 'tax' entry
             given the where the customer is residing. Currently the 'region' code is used
             for finding the correct 'tax' entries, matching on "region-code" stored in
             the 'TaxRate' metadata. */
    override fun getTaxRatesForRegion(region: String): Either<PaymentError, List<TaxRateInfo>> =
            getTaxRates()
                    .flatMap {
                        val lst = it.filter { x -> !x.metadata["region-code"].isNullOrEmpty() &&
                                x.metadata["region-code"].equals(region, true) }
                        if (lst.isEmpty())
                            emptyList<TaxRateInfo>()
                                    .right()
                        else {
                            lst.map {
                                TaxRateInfo(id = it.id,
                                        percentage = it.percentage,
                                        displayName = it.displayName,   /* VAT, GST, etc. */
                                        inclusive = it.inclusive)
                            }.right()
                        }
                    }

    private fun getTaxRates(): Either<PaymentError, List<TaxRate>> =
            either("Failed to fetch list with tax-rates from Stripe") {
                val taxRateParameters = mapOf(
                        "active" to true
                )
                TaxRate.list(taxRateParameters)
                        .data
            }

    private fun <RETURN> either(errorDescription: String, action: () -> RETURN): Either<PaymentError, RETURN> {
        return try {
            Either.right(action())
        } catch (e: CardException) {
            // If something is decline with a card purchase, CardException will be caught
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}")
            Either.left(ForbiddenError(errorDescription, e.message))
        } catch (e: RateLimitException) {
            // Too many requests made to the API too quickly
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}")
            Either.left(BadGatewayError(errorDescription, e.message))
        } catch (e: InvalidRequestException) {
            // Invalid parameters were supplied to Stripe's API
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}")
            Either.left(ForbiddenError(errorDescription, e.message))
        } catch (e: AuthenticationException) {
            // Authentication with Stripe's API failed
            // (maybe you changed API keys recently)
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: ApiConnectionException) {
            // Network communication with Stripe failed
            logger.warn("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: StripeException) {
            // Unknown Stripe error
            logger.error("Payment error : $errorDescription , Stripe Error Code: ${e.code}", e)
            Either.left(BadGatewayError(errorDescription))
        } catch (e: Exception) {
            // Something else happened, could be completely unrelated to Stripe
            logger.error(errorDescription, e)
            Either.left(BadGatewayError(errorDescription))
        }
    }
}
