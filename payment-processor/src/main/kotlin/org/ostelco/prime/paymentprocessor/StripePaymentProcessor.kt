package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.stripe.model.Card
import com.stripe.model.Charge
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.Invoice
import com.stripe.model.InvoiceItem
import com.stripe.model.PaymentIntent
import com.stripe.model.PaymentSource
import com.stripe.model.Plan
import com.stripe.model.Product
import com.stripe.model.Refund
import com.stripe.model.Source
import com.stripe.model.Subscription
import com.stripe.model.TaxRate
import com.stripe.net.RequestOptions
import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.StripeUtils.either
import org.ostelco.prime.paymentprocessor.core.BadGatewayError
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.InvoicePaymentInfo
import org.ostelco.prime.paymentprocessor.core.InvoiceInfo
import org.ostelco.prime.paymentprocessor.core.InvoiceItemInfo
import org.ostelco.prime.paymentprocessor.core.NotFoundError
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.PaymentStatus
import org.ostelco.prime.paymentprocessor.core.PaymentTransactionInfo
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

    override fun getSavedSources(customerId: String): Either<PaymentError, List<SourceDetailsInfo>> =
            either("Failed to retrieve sources for customer $customerId") {
                val customer = Customer.retrieve(customerId)
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
                    mapOf(
                            "id" to paymentSource.id,
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
                            "funding" to paymentSource.funding,         // "credit" or "debit"
                            "last4" to paymentSource.last4)
                            .filterValues { it != null }
                }
                is Source -> {
                    mapOf(
                            "id" to paymentSource.id,
                            "type" to "source",
                            "created" to Instant.ofEpochSecond(paymentSource.created).toEpochMilli(),
                            "owner" to mapOf(
                                    "email" to paymentSource.owner.email,
                                    "name" to paymentSource.owner.name,
                                    "phone" to paymentSource.owner.phone,
                                    "address" to
                                            if (paymentSource.owner.address != null)
                                                mapOf(
                                                        "city" to paymentSource.owner.address.city,
                                                        "country" to paymentSource.owner.address.country,
                                                        "line1" to paymentSource.owner.address.line1,
                                                        "line2" to paymentSource.owner.address.line2,
                                                        "postalCode" to paymentSource.owner.address.postalCode,
                                                        "state" to paymentSource.owner.address.state)
                                                        .filterValues { it != null }
                                            else
                                                paymentSource.owner.address),
                            "threeDSecure" to paymentSource.threeDSecure)
                            .filterValues { it != null }
                }
                else -> {
                    logger.error("Received unsupported Stripe source/account type: {}",
                            paymentSource)
                    mapOf("id" to paymentSource.id,
                            "type" to "unsupported",
                            "created" to Instant.now().toEpochMilli())
                }
            }

    /* Handle type conversion when reading the 'created' field from the
       metadata returned from Stripe. (It might seem like that Stripe
       returns stored metadata values as strings, even if they where stored
       using an another type. Needs to be verified.) */
    private fun getCreatedTimestampFromMetadata(id: String, metadata: Map<String, Any>): Long {
        val created: String? = metadata["created"] as? String
        return with (created?.toLongOrNull()) {
            if (this != null) {
                Instant.ofEpochSecond(this).toEpochMilli()
            } else {
                logger.warn("No 'created' timestamp found in metadata for Stripe account {}",
                        id)
                Instant.now().toEpochMilli()
            }
        }
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
            getCustomer(customerId)
                    .flatMap { customer ->
                        ProfileInfo(customer.id)
                                .right()
                    }

    /* Fetch customer from Stripe with result checks. */
    private fun getCustomer(customerId: String): Either<PaymentError, Customer> =
            Try {
                Customer.retrieve(customerId)
            }.fold(
                    ifSuccess = { customer ->
                        when {
                            customer.deleted == true -> NotFoundError("Payment profile for user $customerId was previously deleted")
                                    .left()
                            else -> customer
                                    .right()
                        }
                    },
                    ifFailure = {
                        NotFoundError("Could not find a payment profile for customer $customerId")
                                .left()
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

    override fun createProduct(name: String): Either<PaymentError, ProductInfo> =
            either("Failed to create product with name $name") {
                val productParams = mapOf(
                        "name" to name,
                        "type" to "service")
                ProductInfo(Product.create(productParams).id)
            }

    override fun removeProduct(productId: String): Either<PaymentError, ProductInfo> =
            either("Failed to delete product $productId") {
                val product = Product.retrieve(productId)
                ProductInfo(product.delete().id)
            }

    override fun addSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to add source $sourceId to customer $customerId") {
                val customer = Customer.retrieve(customerId)
                val sourceParams = mapOf("source" to sourceId,
                        "metadata" to mapOf("created" to ofEpochMilliToSecond(Instant.now().toEpochMilli())))
                SourceInfo(customer.sources.create(sourceParams).id)
            }

    override fun setDefaultSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to set default source $sourceId for customer $customerId") {
                val customer = Customer.retrieve(customerId)
                val updateParams = mapOf("default_source" to sourceId)
                val customerUpdated = customer.update(updateParams)
                SourceInfo(customerUpdated.defaultSource)
            }

    override fun getDefaultSource(customerId: String): Either<PaymentError, SourceInfo> =
            either("Failed to get default source for customer $customerId") {
                SourceInfo(Customer.retrieve(customerId).defaultSource)
            }

    override fun removePaymentProfile(customerId: String): Either<PaymentError, ProfileInfo> =
            getCustomer(customerId)
                    .flatMap { customer ->
                        ProfileInfo(customer.delete().id)
                                .right()
                    }

    /* The 'expand' part will cause an immediate attempt at charging for the
       subscription when creating it. For interpreting the result see:
       https://stripe.com/docs/billing/subscriptions/payment#signup-3b */
    override fun createSubscription(planId: String, customerId: String, trialEnd: Long, taxRegionId: String?): Either<PaymentError, SubscriptionDetailsInfo> =
            either("Failed to subscribe customer $customerId to plan $planId") {
                val item = mapOf("plan" to planId)
                val taxRates = getTaxRatesForTaxRegionId(taxRegionId)
                        .fold(
                                { emptyList<TaxRateInfo>() },
                                { it }
                        )
                val subscriptionParams = mapOf(
                        "customer" to customerId,
                        "items" to mapOf("0" to item),
                        *(if (trialEnd > Instant.now().epochSecond)
                            arrayOf("trial_end" to trialEnd.toString())
                        else
                            arrayOf("expand" to arrayOf("latest_invoice.payment_intent"))),
                        *(if (taxRates.isNotEmpty())
                            arrayOf("default_tax_rates" to taxRates.map { it.id })
                        else arrayOf()))
                val subscription = Subscription.create(subscriptionParams)
                val status = subscriptionStatus(subscription)
                SubscriptionDetailsInfo(id = subscription.id,
                        status = status.first,
                        invoiceId = status.second,
                        chargeId = status.third,
                        created = Instant.ofEpochSecond(subscription.created).toEpochMilli(),
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
            0 -> "ZERO_CHARGE_${UUID.randomUUID()}"
                    .right()
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
            0 -> chargeId.right()
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
                    charge.id.right()
                } catch (e: Exception) {
                    logger.warn(errorMessage, e)
                    BadGatewayError(errorMessage).left()
                }
            }
        }
    }

    override fun refundCharge(chargeId: String): Either<PaymentError, String> =
            either("Failed to refund charge $chargeId") {
                val refundParams = mapOf("charge" to chargeId)
                Refund.create(refundParams).id
            }

    override fun refundCharge(chargeId: String, amount: Int): Either<PaymentError, String> =
            when (amount) {
                0 -> chargeId.right()
                else -> either("Failed to refund charge $chargeId") {
                    val refundParams = mapOf(
                            "charge" to chargeId,
                            "amount" to amount)
                    Refund.create(refundParams).id
                }
            }

    override fun removeSource(customerId: String, sourceId: String): Either<PaymentError, SourceInfo> =
            either("Failed to remove source $sourceId for customerId $customerId") {
                val accountInfo = Customer.retrieve(customerId).sources.retrieve(sourceId)
                when (accountInfo) {
                    is Card -> accountInfo.delete()
                    is Source -> accountInfo.detach()
                    else ->
                        BadGatewayError("Attempt to remove unsupported account-type $accountInfo")
                                .left()
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

    override fun createInvoice(customerId: String, taxRates: List<TaxRateInfo>, sourceId: String?): Either<PaymentError, InvoiceInfo> =
            createAndGetInvoiceDetails(customerId, taxRates, sourceId)
                    .flatMap {
                        InvoiceInfo(it.id).right()
                    }

    override fun createInvoice(customerId: String, amount: Int, currency: String, description: String, taxRegionId: String?, sourceId: String?): Either<PaymentError, InvoiceInfo> =
            createInvoiceItem(customerId, amount, currency, description)
                    .flatMap {
                        getTaxRatesForTaxRegionId(taxRegionId)
                    }
                    .flatMap {
                        createAndGetInvoiceDetails(customerId, it, sourceId)
                    }
                    .flatMap { invoice ->
                        /* If there are more than one line item in the invoice, then line item(s)
                           added by the (same) customer has accidentally been picked up by this
                           invoice (can happen if the customer uses multiple clients or because of
                           network/infrastructure issues).
                           In this icase the invoice is invalid and can't be processed. */
                        val items = invoice.lines.list(emptyMap()).data

                        if (items.size > 1) {
                            /* Deletes the invoice. Strictly speaking it is enough to just
                               delete the invoice itself. */
                            logger.error(NOTIFY_OPS_MARKER,
                                    "Unexpected number of line items got added to invoice ${invoice.id}, " +
                                            "when attempting to create the invoice - expected one but was ${items.size}")

                            val errorMessage = "Incorrect number of line items when attempting to create invoice ${invoice.id} " +
                                    "- expected one but was ${items.size}"

                            removeInvoice(invoice)
                                    .fold({
                                        BadGatewayError(errorMessage, error = it)
                                    }, {
                                        BadGatewayError(errorMessage)
                                    }).left()
                        } else {
                            InvoiceInfo(invoice.id).right()
                        }
                    }

    /* Create and return invoice details. */
    private fun createAndGetInvoiceDetails(customerId: String, taxRates: List<TaxRateInfo>, sourceId: String?): Either<PaymentError, Invoice> =
            either("") {
                val params = mapOf(
                        "customer" to customerId,
                        "auto_advance" to true,
                        *(if (taxRates.isNotEmpty())
                            arrayOf("default_tax_rates" to taxRates.map { it.id })
                        else arrayOf()),
                        *(if (sourceId != null)
                            arrayOf("default_source" to sourceId)
                        else arrayOf()))
                Invoice.create(params)
            }

    override fun payInvoice(invoiceId: String): Either<PaymentError, InvoicePaymentInfo> =
            either("Failed to complete payment of invoice ${invoiceId}") {
                val receipt = Invoice.retrieve(invoiceId).pay()
                InvoicePaymentInfo(receipt.id, receipt?.charge ?: UUID.randomUUID().toString())
            }

    override fun removeInvoice(invoiceId: String): Either<PaymentError, InvoiceInfo> =
            Try {
                Invoice.retrieve(invoiceId)
            }.fold(
                    ifSuccess = {
                        removeInvoice(it)
                    },
                    ifFailure = {
                        logger.error("Unexpected error {} when attempting to delete invoice {}",
                                it.message, invoiceId)
                        NotFoundError("Unexpected error ${it.message} when attempting to delete invoice ${invoiceId}")
                                .left()
                    }
            )

    private fun removeInvoice(invoice: Invoice): Either<PaymentError, InvoiceInfo> =
            either("Error when attempting to remove invoice ${invoice.id} with Stripe") {

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

    /* Metadata key used to assoiciate a specific tax rate with a specific
       regional area. Typically such a region will be a country. */
    companion object {
        val TAX_REGION = "tax-region"
    }

    /* NOTE! This method creates a 'tax rate' with Stipe, unless there already
             exists a 'tax-rate' with the same information. */
    override fun createTaxRateForTaxRegionId(taxRegionId: String, percentage: BigDecimal, displayName: String, inclusive: Boolean): Either<PaymentError, TaxRateInfo> =
            getTaxRatesForTaxRegionId(taxRegionId)
                    .flatMap { taxRates ->
                        val match = taxRates.find {
                            it.percentage == percentage && it.displayName == displayName && it.inclusive == inclusive
                        }

                        if (match == null) {
                            val param = mapOf(
                                    "percentage" to percentage,
                                    "display_name" to displayName,
                                    "inclusive" to inclusive,
                                    "metadata" to mapOf(TAX_REGION to taxRegionId))
                            TaxRateInfo(TaxRate.create(param).id, percentage, displayName, inclusive)
                        } else {
                            match
                        }.right()
                    }

    override fun getTaxRatesForTaxRegionId(taxRegionId: String?): Either<PaymentError, List<TaxRateInfo>> =
            if (taxRegionId != null)
                getTaxRates()
                        .flatMap {
                            val lst = it.filter { x ->
                                !x.metadata[TAX_REGION].isNullOrEmpty() &&
                                        x.metadata[TAX_REGION].equals(taxRegionId, true)
                            }
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
            else
                emptyList<TaxRateInfo>()
                        .right()

    private fun getTaxRates(): Either<PaymentError, List<TaxRate>> =
            either("Failed to fetch list with tax-rates from Stripe") {
                val taxRateParameters = mapOf(
                        "active" to true
                )
                TaxRate.list(taxRateParameters)
                        .data
            }

    override fun getPaymentTransactions(start: Long, end: Long): Either<PaymentError, List<PaymentTransactionInfo>> =
            either("Failed to fetch payment transactions from Stripe") {
                val param = mapOf(
                        "created[gte]" to ofEpochMilliToSecond(start),
                        "created[lte]" to ofEpochMilliToSecond(end))
                /* A payment-intent with status "succeeded" equals to a "payment transaction". */
                PaymentIntent.list(param)
                        .autoPagingIterable()
                        .filter {
                            it.status == "succeeded"
                        }.map { intent ->
                            intent.charges.data.map {
                                PaymentTransactionInfo(id = it.id,      /* 'chargeId' */
                                        amount = it.amount.toInt(),     /* Note: 'int' is used internally for amounts. */
                                        currency = it.currency,
                                        created = Instant.ofEpochSecond(it.created).toEpochMilli(),
                                        refunded = it.refunded)
                            }
                        }.flatMap {
                            it
                        }.toList()
            }

    /* Timestamps in Stripe must be in seconds. */
    private fun ofEpochMilliToSecond(ts: Long): Long = ts.div(1000L)
}
