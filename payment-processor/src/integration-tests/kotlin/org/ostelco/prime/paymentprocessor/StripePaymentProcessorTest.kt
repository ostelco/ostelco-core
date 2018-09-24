package org.ostelco.prime.paymentprocessor

import arrow.core.flatMap
import arrow.core.right
import com.stripe.Stripe
import com.stripe.model.Token
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.module.getResource
import kotlin.test.assertEquals
import kotlin.test.fail


class StripePaymentProcessorTest {

    private val paymentProcessor = getResource<PaymentProcessor>()
    private val testCustomer = "testuser@StripePaymentProcessorTest.ok"

    private var stripeCustomerId = ""

    fun createPaymentSourceId(): String {

        val cardMap = mapOf(
                "number" to "4242424242424242",
                "exp_month" to 8,
                "exp_year" to 2019,
                "cvc" to "314")

        val tokenMap = mapOf("card" to cardMap)
        val token = Token.create(tokenMap)
        return token.id
    }

    private fun addCustomer() {
        val resultAdd = paymentProcessor.createPaymentProfile(testCustomer)
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

    @Test
    fun unknownCustomerGetSavedSources() {
        val result = paymentProcessor.getSavedSources(customerId = "unknown")
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
    fun ensureSortedSourceList() {

        val addedSources = listOf(
                paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId()),
                paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId()),
                paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        )

        // Should now be sorted descending by the "created" timestamp.
        val sortedSources = paymentProcessor.getSavedSources(stripeCustomerId)

        assertEquals(true, sortedSources.isRight())

        println(">>> stored sources: ${sortedSources}")
    }

    @Test
    fun addSourceToCustomerAndRemove() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())

        val resultStoredSources = paymentProcessor.getSavedSources(stripeCustomerId)
        assertEquals(1, resultStoredSources.fold({ 0 }, { it.size }))

        resultAddSource.map { addedSource ->
            resultStoredSources.map { storedSources ->
                assertEquals(addedSource.id, storedSources.first().id)
            }.mapLeft { fail() }
        }.mapLeft { fail() }

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId, resultAddSource.fold({ "" }, { it.id }))
        assertEquals(true, resultDeleteSource.isRight())
    }

    @Test
    fun addSourceToCustomerTwise() {
        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())

        val resultStoredSources = paymentProcessor.getSavedSources(stripeCustomerId)
        assertEquals(1, resultStoredSources.fold({ 0 }, { it.size }))

        resultAddSource.map { addedSource ->
            resultStoredSources.map { storedSources ->
                assertEquals(addedSource.id, storedSources.first().id)
            }.mapLeft { fail() }
        }.mapLeft { fail() }

        val resultAddSecondSource = paymentProcessor.addSource(stripeCustomerId, resultStoredSources.fold({ "" }, { it.first().id }))
        assertEquals(true, resultAddSecondSource.isLeft())

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId, resultAddSource.fold({ "" }, { it.id }))
        assertEquals(true, resultDeleteSource.isRight())
    }

    @Test
    fun addDefaultSourceAndRemove() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        assertEquals(true, resultAddSource.isRight())

        val resultAddDefault = paymentProcessor.setDefaultSource(stripeCustomerId, resultAddSource.fold({ "" }, { it.id }))
        assertEquals(true, resultAddDefault.isRight())

        val resultGetDefault = paymentProcessor.getDefaultSource(stripeCustomerId)
        assertEquals(true, resultGetDefault.isRight())
        assertEquals(resultAddDefault.fold({ "" }, { it.id }), resultGetDefault.fold({ "" }, { it.id }))

        val resultRemoveDefault = paymentProcessor.removeSource(stripeCustomerId, resultAddDefault.fold({ "" }, { it.id }))
        assertEquals(true, resultRemoveDefault.isRight())
    }

    @Test
    fun createAuthorizeChargeAndRefund() {
        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        assertEquals(true, resultAddSource.isRight())

        val resultAuthorizeCharge = paymentProcessor.authorizeCharge(stripeCustomerId, resultAddSource.fold({ "" }, { it.id }), 1000, "nok")
        assertEquals(true, resultAuthorizeCharge.isRight())

        val resultRefundCharge = paymentProcessor.refundCharge(resultAuthorizeCharge.fold({ "" }, { it } ))
        assertEquals(true, resultRefundCharge.isRight())
        assertEquals(resultAuthorizeCharge.fold({ "" }, { it } ), resultRefundCharge.fold({ "" }, { it } ))

        val resultRemoveSource = paymentProcessor.removeSource(stripeCustomerId, resultAddSource.fold({ "" }, { it.id }))
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

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        assertEquals(true, resultAddSource.isRight())

        val resultCreateProduct = paymentProcessor.createProduct("TestSku")
        assertEquals(true, resultCreateProduct.isRight())

        val resultCreatePlan = paymentProcessor.createPlan(resultCreateProduct.fold({ "" }, { it.id }), 1000, "NOK", PaymentProcessor.Interval.MONTH)
        assertEquals(true, resultCreatePlan.isRight())

        val resultSubscribePlan = paymentProcessor.subscribeToPlan(resultCreatePlan.fold({ "" }, { it.id }), stripeCustomerId)
        assertEquals(true, resultSubscribePlan.isRight())

        val resultUnsubscribePlan = paymentProcessor.cancelSubscription(resultSubscribePlan.fold({ "" }, { it.id }), false)
        assertEquals(true, resultUnsubscribePlan.isRight())
        assertEquals(resultSubscribePlan.fold({ "" }, { it.id }), resultUnsubscribePlan.fold({ "" }, { it.id }))

        val resultDeletePlan = paymentProcessor.removePlan(resultCreatePlan.fold({ "" }, { it.id }))
        assertEquals(true, resultDeletePlan.isRight())
        assertEquals(resultCreatePlan.fold({ "" }, { it.id }), resultDeletePlan.fold({ "" }, { it.id }))

        val resultRemoveProduct = paymentProcessor.removeProduct(resultCreateProduct.fold({ "" }, { it.id }))
        assertEquals(true, resultRemoveProduct.isRight())
        assertEquals(resultCreateProduct.fold({ "" }, { it.id }), resultRemoveProduct.fold({ "" }, { it.id }))

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId, resultAddSource.fold({ "" }, { it.id }))
        assertEquals(true, resultDeleteSource.isRight())
    }
}