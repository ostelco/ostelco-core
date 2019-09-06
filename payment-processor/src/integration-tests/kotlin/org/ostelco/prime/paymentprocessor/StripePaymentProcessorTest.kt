package org.ostelco.prime.paymentprocessor

import arrow.core.Either
import arrow.core.getOrElse
import com.stripe.Stripe
import com.stripe.model.Source
import com.stripe.model.Token
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.core.PaymentError
import java.time.Year
import java.util.*
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.fail

@Ignore
class StripePaymentProcessorTest {

    private val paymentProcessor = getResource<PaymentProcessor>()
    private val testCustomer = UUID.randomUUID().toString()
    private val emailTestCustomer = "test@internet.org"

    private var stripeCustomerId = ""

    private fun createPaymentTokenId() : String {

        val cardMap = mapOf(
                "number" to "4242424242424242",
                "exp_month" to 12,
                "exp_year" to nextYear(),
                "cvc" to "314")
        val tokenMap = mapOf("card" to cardMap)

        val token = Token.create(tokenMap)
        return token.id
    }

    private fun createPaymentSourceId() : String {

        val sourceMap = mapOf(
                "type" to "card",
                "card" to mapOf(
                        "number" to "4242424242424242",
                        "exp_month" to 8,
                        "exp_year" to 2022,
                        "cvc" to "314"),
                "owner" to mapOf(
                        "address" to mapOf(
                                "city" to "Oslo",
                                "country" to "Norway"
                        ),
                        "email" to "me@somewhere.com")
                )

        val source = Source.create(sourceMap)
        return source.id
    }

    private fun addCustomer() {
        val resultAdd = paymentProcessor.createPaymentProfile(customerId = testCustomer, email = emailTestCustomer)
        assertEquals(true, resultAdd.isRight())

        stripeCustomerId = resultAdd.fold({ "" }, { it.id })
    }

    @Before
    fun setUp() {
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")
        addCustomer()
    }

    @After
    fun cleanUp() {
        val resultDelete = paymentProcessor.deletePaymentProfile(stripeCustomerId)
        assertEquals(true, resultDelete.isRight())
    }

    /* Flag to ensure that tax rates for tests are only added once. */
    var taxesAdded = false

    /* Note! No corresponding delete, as this can't be done using the API. */
    @Before
    fun addTaxRates() {
        if (!taxesAdded) {
            val addedTaxRate = paymentProcessor.createTaxRateForTaxRegionId("sg", 7.0.toBigDecimal(), "GST")
            assertEquals(true, addedTaxRate.isRight())

            taxesAdded = true
        }
    }

    @Test
    fun unknownCustomerGetSavedSources() {
        val result = paymentProcessor.getSavedSources(stripeCustomerId = "unknown")
        assertEquals(true, result.isLeft())
    }

    @Test
    fun getPaymentProfile() {
        val result = paymentProcessor.getPaymentProfile(testCustomer)
        assertEquals(true, result.isRight())
        assertEquals(stripeCustomerId, result.fold({""}, {it.id}))
    }

    @Test
    fun getUnknownPaymentProfile() {
        val result = paymentProcessor.getPaymentProfile("not@fail.com")
        assertEquals(false, result.isRight())
    }

    @Test
    fun ensureSourcesSorted() {

        run {
            paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())
            // Ensure that not all sources falls within the same second.
            Thread.sleep(1_001)
            paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        }

        // Should be in descending sorted order by the "created" timestamp.
        val sources = paymentProcessor.getSavedSources(stripeCustomerId)

        val createdTimestamps = sources.getOrElse {
            fail("The 'created' field is missing from the list of sources: ${sources}")
        }.map { it.details["created"] as Long }

        val createdTimestampsSorted = createdTimestamps.sortedByDescending { it }

        assertEquals(createdTimestamps, createdTimestampsSorted,
                "The list of sources is not in descending sorted order by 'created' timestamp: ${sources}")
    }

    @Test
    fun addAndRemoveMultipleSources() {

        val sources= listOf(
            paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId()),
            paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        )

        val sourcesRemoved = sources.map {
            paymentProcessor.removeSource(stripeCustomerId, it.getOrElse {
                fail("Failed to remove source ${it}")
            }.id)
        }

        sourcesRemoved.forEach { it ->
            assertEquals(true, it.isRight(), "Unexpected failure when removing source $it")
        }
    }

    @Test
    fun addSourceToCustomerAndRemove() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())

        val resultStoredSources = paymentProcessor.getSavedSources(stripeCustomerId)
        assertEquals(1, resultStoredSources.fold({ 0 }, { it.size }))

        resultAddSource.map { addedSource ->
            resultStoredSources.map { storedSources ->
                assertEquals(addedSource.id, storedSources.first().id)
            }.mapLeft { fail() }
        }.mapLeft { fail() }

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId, right(resultAddSource).id)
        assertEquals(true, resultDeleteSource.isRight())
    }

    @Test
    fun addSourceToCustomerTwice() {
        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())

        val resultStoredSources = paymentProcessor.getSavedSources(stripeCustomerId)
        assertEquals(1, resultStoredSources.fold({ 0 }, { it.size }))

        resultAddSource.map { addedSource ->
            resultStoredSources.map { storedSources ->
                assertEquals(addedSource.id, storedSources.first().id)
            }.mapLeft { fail() }
        }.mapLeft { fail() }

        val resultAddSecondSource = paymentProcessor.addSource(stripeCustomerId, right(resultStoredSources).first().id)
        assertEquals(true, resultAddSecondSource.isLeft())

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId, right(resultAddSource).id)
        assertEquals(true, resultDeleteSource.isRight())
    }

    @Test
    fun addDefaultSourceAndRemove() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())
        assertEquals(true, resultAddSource.isRight())

        val resultAddDefault = paymentProcessor.setDefaultSource(stripeCustomerId, right(resultAddSource).id)
        assertEquals(true, resultAddDefault.isRight())

        val resultGetDefault = paymentProcessor.getDefaultSource(stripeCustomerId)
        assertEquals(true, resultGetDefault.isRight())
        assertEquals(resultAddDefault.fold({ "" }, { it.id }), right(resultGetDefault).id)

        val resultRemoveDefault = paymentProcessor.removeSource(stripeCustomerId, right(resultAddDefault).id)
        assertEquals(true, resultRemoveDefault.isRight())
    }

    @Test
    fun createAuthorizeChargeAndRefund() {
        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())
        assertEquals(true, resultAddSource.isRight())

        val amount = 1000
        val currency = "NOK"

        val resultAuthorizeCharge = paymentProcessor.authorizeCharge(stripeCustomerId, right(resultAddSource).id, amount, currency)
        assertEquals(true, resultAuthorizeCharge.isRight())

        val resultRefundCharge = paymentProcessor.refundCharge(right(resultAuthorizeCharge), amount)
        assertEquals(true, resultRefundCharge.isRight())

        val resultRemoveSource = paymentProcessor.removeSource(stripeCustomerId, right(resultAddSource).id)
        assertEquals(true, resultRemoveSource.isRight())
    }

    @Test
    fun createAuthorizeChargeAndRefundWithZeroAmount() {
        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())
        assertEquals(true, resultAddSource.isRight())

        val amount = 0
        val currency = "NOK"

        val resultAuthorizeCharge = paymentProcessor.authorizeCharge(stripeCustomerId, right(resultAddSource).id, amount, currency)
        assertEquals(true, resultAuthorizeCharge.isRight())

        val resultRefundCharge = paymentProcessor.refundCharge(right(resultAuthorizeCharge), amount)
        assertEquals(true, resultRefundCharge.isRight())
        assertEquals(resultAuthorizeCharge.fold({ "" }, { it } ), right(resultRefundCharge))

        val resultRemoveSource = paymentProcessor.removeSource(stripeCustomerId, right(resultAddSource).id)
        assertEquals(true, resultRemoveSource.isRight())
    }

    @Test
    fun createAndRemoveProduct() {
        val resultCreateProduct = paymentProcessor.createProduct("TestSku")
        assertEquals(true, resultCreateProduct.isRight())

        val resultRemoveProduct = paymentProcessor.removeProduct(resultCreateProduct.fold({ "" }, { it.id }))
        assertEquals(true, resultRemoveProduct.isRight())
    }

    @Test
    fun subscribeAndUnsubscribePlan() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())
        assertEquals(true, resultAddSource.isRight())

        val resultCreateProduct = paymentProcessor.createProduct("TestSku")
        assertEquals(true, resultCreateProduct.isRight())

        val resultCreatePlan = paymentProcessor.createPlan(right(resultCreateProduct).id, 1000, "NOK", PaymentProcessor.Interval.MONTH)
        assertEquals(true, resultCreatePlan.isRight())

        val resultSubscribePlan = paymentProcessor.createSubscription(right(resultCreatePlan).id, stripeCustomerId)
        assertEquals(true, resultSubscribePlan.isRight())

        val resultUnsubscribePlan = paymentProcessor.cancelSubscription(right(resultSubscribePlan).id, false)
        assertEquals(true, resultUnsubscribePlan.isRight())
        assertEquals(resultSubscribePlan.fold({ "" }, { it.id }), right(resultUnsubscribePlan).id)

        val resultDeletePlan = paymentProcessor.removePlan(right(resultCreatePlan).id)
        assertEquals(true, resultDeletePlan.isRight())
        assertEquals(resultCreatePlan.fold({ "" }, { it.id }), right(resultDeletePlan).id)

        val resultRemoveProduct = paymentProcessor.removeProduct(right(resultCreateProduct).id)
        assertEquals(true, resultRemoveProduct.isRight())
        assertEquals(resultCreateProduct.fold({ "" }, { it.id }), right(resultRemoveProduct).id)

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId, right(resultAddSource).id)
        assertEquals(true, resultDeleteSource.isRight())
    }

    @Test
    fun createAndDeleteInvoiceItem() {
        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())
        assertEquals(true, resultAddSource.isRight())

        val amount = 5000
        val currency = "SGD"

        val addedInvoiceItem = paymentProcessor.createInvoiceItem(stripeCustomerId, amount, currency, "SGD")
        assertEquals(true, addedInvoiceItem.isRight())

        val removedInvoiceItem = paymentProcessor.removeInvoiceItem(right(addedInvoiceItem).id)
        assertEquals(true, removedInvoiceItem.isRight())
    }

    @Test
    fun createAndDeleteInvoiceWithTaxes() {
        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentTokenId())
        assertEquals(true, resultAddSource.isRight())

        val amount = 5000
        val currency = "SGD"

        val addedInvoiceItem = paymentProcessor.createInvoiceItem(stripeCustomerId, amount, currency, "SGD")
        assertEquals(true, addedInvoiceItem.isRight())

        val taxRegionId = "sg"

        val taxRates = paymentProcessor.getTaxRatesForTaxRegionId(taxRegionId)
        assertEquals(true, taxRates.isRight())

        val addedInvoice = paymentProcessor.createInvoice(stripeCustomerId, right(taxRates))
        assertEquals(true, addedInvoice.isRight())

        val payedInvoice = paymentProcessor.payInvoice(right(addedInvoice).id)
        assertEquals(true, payedInvoice.isRight())

        val removedInvoice = paymentProcessor.removeInvoice(right(payedInvoice).id)
        assertEquals(true, removedInvoice.isRight())
    }

    /* Helper function to unpack the 'right' part of an 'either'. */
    private fun <T> right(arg: Either<PaymentError, T>): T =
        arg.fold({ fail("Invalid argument, expected a 'right' value but was ${it}") }, { it })

    private fun nextYear() = Year.now().value + 1
}
