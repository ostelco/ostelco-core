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
import org.ostelco.prime.paymentprocessor.core.SourceDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SourceInfo
import java.time.Year
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.fail


class StripePaymentProcessorTest {

    private val paymentProcessor = getResource<PaymentProcessor>()
    private val testCustomer = UUID.randomUUID().toString()
    private val emailTestCustomer = "test@internet.org"

    private var customerId = ""

    private fun createPaymentTokenId(): String {

        val cardMap = mapOf(
                "number" to "4242424242424242",
                "exp_month" to 12,
                "exp_year" to nextYear(),
                "cvc" to "314")
        val tokenMap = mapOf("card" to cardMap)

        val token = Token.create(tokenMap)
        return token.id
    }

    private fun createPaymentSourceId(): String {

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
        resultAdd.isRight()

        customerId = resultAdd.fold({ "" }, { it.id })
    }

    @Before
    fun setUp() {
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")
        addCustomer()
    }

    @After
    fun cleanUp() {
        val resultDelete = paymentProcessor.removePaymentProfile(customerId)
        assertNotFailure(resultDelete)
    }

    /* Flag to ensure that tax rates for tests are only added once. */
    var taxesAdded = false

    /* Note! No corresponding delete, as this can't be done using the API. */
    @Before
    fun addTaxRates() {
        if (!taxesAdded) {
            val addedTaxRate = paymentProcessor.createTaxRateForTaxRegionId("sg", 7.0.toBigDecimal(), "GST")
            assertNotFailure(addedTaxRate)

            taxesAdded = true
        }
    }


    fun <T> assertFailure(result: Either<PaymentError, T>, msg: String? = null) {
        if (result.isRight()) {
            if (msg == null) {
                fail()
            } else {
                fail(msg)
            }
        }
    }

    fun <T> assertNotFailure(result: Either<PaymentError, T>, msg: String? = null) {
        if (result.isLeft()) {
            result.mapLeft { error ->
                if (msg == null) {
                    fail("Test failed with message:  ${error.message}.")
                } else {
                    fail(msg)
                }
            }
        }
    }

    @Test
    fun unknownCustomerGetSavedSources() {
        val result = paymentProcessor.getSavedSources(customerId = "unknown")

        assertFailure(result)
    }

    @Test
    fun getPaymentProfile() {
        val result = paymentProcessor.getPaymentProfile(testCustomer)
        assertNotFailure(result)
        assertEquals(customerId, result.fold({ "" }, { it.id }))
    }

    @Test
    fun getUnknownPaymentProfile() {
        val result = paymentProcessor.getPaymentProfile("not@fail.com")
        assertEquals(false, result.isRight())
    }

    @Test
    fun ensureSourcesSorted() {

        run {
            paymentProcessor.addSource(customerId, createPaymentTokenId())
            // Ensure that not all sources falls within the same second.
            Thread.sleep(1_001)
            paymentProcessor.addSource(customerId, createPaymentSourceId())
        }

        // Should be in descending sorted order by the "created" timestamp.
        val sources = paymentProcessor.getSavedSources(customerId)

        val createdTimestamps = sources.getOrElse {
            fail("The 'created' field is missing from the list of sources: ${sources}")
        }.map { it.details["created"] as Long }

        val createdTimestampsSorted = createdTimestamps.sortedByDescending { it }

        assertEquals(createdTimestamps, createdTimestampsSorted,
                "The list of sources is not in descending sorted order by 'created' timestamp: ${sources}")
    }

    @Test
    fun addAndRemoveMultipleSources() {

        val sources = listOf(
                paymentProcessor.addSource(customerId, createPaymentTokenId()),
                paymentProcessor.addSource(customerId, createPaymentSourceId())
        )

        val sourcesRemoved = sources.map {
            paymentProcessor.removeSource(customerId, it.getOrElse {
                fail("Failed to remove source ${it}")
            }.id)
        }

        sourcesRemoved.forEach { it ->
            assertNotFailure(it, "Unexpected failure when removing source $it")
        }
    }


    private fun checkthatStoredResourcesMatchAddedResources(
            resultAddSource: Either<PaymentError, SourceInfo>,
            resultStoredSources: Either<PaymentError, List<SourceDetailsInfo>>) {
        assertNotFailure(resultAddSource)
        assertNotFailure(resultStoredSources)
        assertEquals(1, resultStoredSources.fold({ 0 }, { it.size }))

        resultAddSource.map { addedSource ->
            resultStoredSources.map { storedSources ->
                assertEquals(addedSource.id, storedSources.first().id)
            }.mapLeft { fail("Payment error: ${it}") }
        }.mapLeft { fail("Payment error: ${it}") }
    }

    @Test
    fun addSourceToCustomerAndRemove() {

        val resultAddSource = paymentProcessor.addSource(customerId, createPaymentTokenId())
        val resultStoredSources = paymentProcessor.getSavedSources(customerId)

        checkthatStoredResourcesMatchAddedResources(resultAddSource, resultStoredSources)

        val resultDeleteSource = paymentProcessor.removeSource(customerId, right(resultAddSource).id)
        assertNotFailure(resultDeleteSource)
    }

    @Test
    fun addSourceToCustomerTwice() {
        val resultAddSource = paymentProcessor.addSource(customerId, createPaymentTokenId())

        val resultStoredSources = paymentProcessor.getSavedSources(customerId)


        checkthatStoredResourcesMatchAddedResources(resultAddSource, resultStoredSources)

        val resultAddSecondSource = paymentProcessor.addSource(customerId, right(resultStoredSources).first().id)
        assertFailure(resultAddSecondSource)

        val resultDeleteSource = paymentProcessor.removeSource(customerId, right(resultAddSource).id)
        assertNotFailure(resultDeleteSource)
    }




    @Test
    fun addDefaultSourceAndRemove() {

        val resultAddSource = paymentProcessor.addSource(customerId, createPaymentTokenId())

        assertNotFailure(resultAddSource)

        val resultAddDefault = paymentProcessor.setDefaultSource(customerId, right(resultAddSource).id)
        assertNotFailure(resultAddDefault)

        val resultGetDefault = paymentProcessor.getDefaultSource(customerId)
        assertNotFailure(resultGetDefault)
        assertEquals(resultAddDefault.fold({ "" }, { it.id }), right(resultGetDefault).id)

        val resultRemoveDefault = paymentProcessor.removeSource(customerId, right(resultAddDefault).id)
        assertNotFailure(resultRemoveDefault)
    }

    @Test
    fun createAuthorizeChargeAndRefund() {
        val resultAddSource = paymentProcessor.addSource(customerId, createPaymentTokenId())
        assertNotFailure(resultAddSource)

        val amount = 1000
        val currency = "NOK"

        val resultAuthorizeCharge = paymentProcessor.authorizeCharge(customerId, right(resultAddSource).id, amount, currency)
        assertNotFailure(resultAuthorizeCharge)

        val resultRefundCharge = paymentProcessor.refundCharge(right(resultAuthorizeCharge), amount)
        assertNotFailure(resultRefundCharge)

        val resultRemoveSource = paymentProcessor.removeSource(customerId, right(resultAddSource).id)
        assertNotFailure(resultRemoveSource)
    }

    @Test
    fun createAuthorizeChargeAndRefundWithZeroAmount() {
        val resultAddSource = paymentProcessor.addSource(customerId, createPaymentTokenId())
        assertNotFailure(resultAddSource)

        val amount = 0
        val currency = "NOK"

        val resultAuthorizeCharge = paymentProcessor.authorizeCharge(customerId, right(resultAddSource).id, amount, currency)
        assertNotFailure(resultAuthorizeCharge)

        val resultRefundCharge = paymentProcessor.refundCharge(right(resultAuthorizeCharge), amount)
        assertNotFailure(resultRefundCharge)
        assertEquals(resultAuthorizeCharge.fold({ "" }, { it }), right(resultRefundCharge))

        val resultRemoveSource = paymentProcessor.removeSource(customerId, right(resultAddSource).id)
        assertNotFailure(resultRemoveSource)
    }

    @Test
    fun createAndRemoveProduct() {
        val resultCreateProduct = paymentProcessor.createProduct("TEST_SKU","TestSku")
        assertNotFailure(resultCreateProduct)

        val resultRemoveProduct = paymentProcessor.removeProduct(resultCreateProduct.fold({ "" }, { it.id }))
        assertNotFailure(resultRemoveProduct)
    }

    @Test
    fun subscribeAndUnsubscribePlan() {

        val resultAddSource = paymentProcessor.addSource(customerId, createPaymentTokenId())
        assertNotFailure(resultAddSource)

        val resultCreateProduct = paymentProcessor.createProduct("TEST_SKU","TestSku")
        assertNotFailure(resultCreateProduct)

        val resultCreatePlan = paymentProcessor.createPlan(right(resultCreateProduct).id, 1000, "NOK", PaymentProcessor.Interval.MONTH)
        assertNotFailure(resultCreatePlan)

        val resultSubscribePlan = paymentProcessor.createSubscription(right(resultCreatePlan).id, customerId)
        assertNotFailure(resultSubscribePlan)

        val resultUnsubscribePlan = paymentProcessor.cancelSubscription(right(resultSubscribePlan).id, false)
        assertNotFailure(resultUnsubscribePlan)
        assertEquals(resultSubscribePlan.fold({ "" }, { it.id }), right(resultUnsubscribePlan).id)

        val resultDeletePlan = paymentProcessor.removePlan(right(resultCreatePlan).id)
        assertNotFailure(resultDeletePlan)
        assertEquals(resultCreatePlan.fold({ "" }, { it.id }), right(resultDeletePlan).id)

        val resultRemoveProduct = paymentProcessor.removeProduct(right(resultCreateProduct).id)
        assertNotFailure(resultRemoveProduct)
        assertEquals(resultCreateProduct.fold({ "" }, { it.id }), right(resultRemoveProduct).id)

        val resultDeleteSource = paymentProcessor.removeSource(customerId, right(resultAddSource).id)
        assertNotFailure(resultDeleteSource)
    }

    @Test
    fun createAndDeleteInvoiceItem() {
        val resultAddSource = paymentProcessor.addSource(customerId, createPaymentTokenId())
        assertNotFailure(resultAddSource)

        val amount = 5000
        val currency = "SGD"

        val addedInvoiceItem = paymentProcessor.createInvoiceItem(customerId, amount, currency, "SGD")
        assertNotFailure(addedInvoiceItem)

        val removedInvoiceItem = paymentProcessor.removeInvoiceItem(right(addedInvoiceItem).id)
        assertNotFailure(removedInvoiceItem)
    }

    @Test
    fun createAndDeleteInvoiceWithTaxes() {
        val resultAddSource = paymentProcessor.addSource(customerId, createPaymentTokenId())
        assertNotFailure(resultAddSource)

        val amount = 5000
        val currency = "SGD"

        val addedInvoiceItem = paymentProcessor.createInvoiceItem(customerId, amount, currency, "SGD")
        assertNotFailure(addedInvoiceItem)

        val taxRegionId = "sg"

        val taxRates = paymentProcessor.getTaxRatesForTaxRegionId(taxRegionId)
        assertNotFailure(taxRates)

        val addedInvoice = paymentProcessor.createInvoice(customerId, right(taxRates))
        assertNotFailure(addedInvoice)

        val payedInvoice = paymentProcessor.payInvoice(right(addedInvoice).id)
        assertNotFailure(payedInvoice)

        val removedInvoice = paymentProcessor.removeInvoice(right(payedInvoice).id)
        assertNotFailure(removedInvoice)
    }

    /* Helper function to unpack the 'right' part of an 'either'. */
    private fun <T> right(arg: Either<PaymentError, T>): T =
            arg.fold({ fail("Invalid argument, expected a 'right' value but was ${it}") }, { it })

    private fun nextYear() = Year.now().value + 1
}
