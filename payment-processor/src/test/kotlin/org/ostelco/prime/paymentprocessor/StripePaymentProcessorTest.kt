package org.ostelco.prime.paymentprocessor

import com.stripe.Stripe
import com.stripe.model.Token
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.module.getResource
import kotlin.test.assertEquals


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

        stripeCustomerId = resultAdd.get().id
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
    fun addSourceToCustomerAndRemove() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        assertEquals(true, resultAddSource.isRight())

        val resultStoredSources = paymentProcessor.getSavedSources(stripeCustomerId)
        assertEquals(true, resultStoredSources.isRight())
        assertEquals(1, resultStoredSources.get().size)
        assertEquals(resultAddSource.get().id, resultStoredSources.get().first().id)

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId ,resultAddSource.get().id)
        assertEquals(true, resultDeleteSource.isRight())
    }

    @Test
    fun addDefaultSourceAndRemove() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        assertEquals(true, resultAddSource.isRight())

        val resultAddDefault = paymentProcessor.setDefaultSource(stripeCustomerId, resultAddSource.get().id)
        assertEquals(true, resultAddDefault.isRight())

        val resultGetDefault = paymentProcessor.getDefaultSource(stripeCustomerId)
        assertEquals(true, resultGetDefault.isRight())
        assertEquals(resultAddDefault.get().id, resultGetDefault.get().id)

        val resultRemoveDefault = paymentProcessor.removeSource(stripeCustomerId, resultAddDefault.get().id)
        assertEquals(true, resultRemoveDefault.isRight())
    }

    @Test
    fun createAndRemoveProduct() {
        val resultCreateProduct = paymentProcessor.createProduct("TestSku")
        assertEquals(true, resultCreateProduct.isRight())

        val resultRemoveProduct = paymentProcessor.removeProduct(resultCreateProduct.get().id)
        assertEquals(true, resultRemoveProduct.isRight())
    }


    @Test
    fun subscribeAndUnsubscribePlan() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        assertEquals(true, resultAddSource.isRight())

        val resultCreateProduct = paymentProcessor.createProduct("TestSku")
        assertEquals(true, resultCreateProduct.isRight())

        val resultCreatePlan = paymentProcessor.createPlan(resultCreateProduct.get().id, 1000, "NOK", PaymentProcessor.Interval.MONTH)
        assertEquals(true, resultCreatePlan.isRight())

        val resultSubscribePlan = paymentProcessor.subscribeToPlan(resultCreatePlan.get().id, stripeCustomerId)
        assertEquals(true, resultSubscribePlan.isRight())

        val resultUnsubscribePlan = paymentProcessor.cancelSubscription(resultSubscribePlan.get().id, false)
        assertEquals(true, resultUnsubscribePlan.isRight())
        assertEquals(resultSubscribePlan.get().id, resultUnsubscribePlan.get().id)

        val resultDeletePlan = paymentProcessor.removePlan(resultCreatePlan.get().id)
        assertEquals(true, resultDeletePlan.isRight())
        assertEquals(resultCreatePlan.get().id, resultDeletePlan.get().id)

        val resultRemoveProduct = paymentProcessor.removeProduct(resultCreateProduct.get().id)
        assertEquals(true, resultRemoveProduct.isRight())
        assertEquals(resultCreateProduct.get().id, resultRemoveProduct.get().id)

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId ,resultAddSource.get().id)
        assertEquals(true, resultDeleteSource.isRight())
    }
}