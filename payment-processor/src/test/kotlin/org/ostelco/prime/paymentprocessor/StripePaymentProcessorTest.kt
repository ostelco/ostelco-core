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

    fun addCustomer() {
        val resultAdd = paymentProcessor.createPaymentProfile(testCustomer)
        assertEquals(true, resultAdd.isRight())

        stripeCustomerId = resultAdd.get().id
    }

    @Test
    fun addSourceToCustomerAndRemove() {

        val resultAddSource = paymentProcessor.addSource(stripeCustomerId, createPaymentSourceId())
        assertEquals(true, resultAddSource.isRight())

        val resultDeleteSource = paymentProcessor.removeSource(stripeCustomerId ,resultAddSource.get().id)
        assertEquals(true, resultDeleteSource.isRight())
    }

    /*
    @Test
    fun addSameSourceTwise() {
        val resultAddCustomer = paymentProcessor.createPaymentProfile(testCustomer)
        assertEquals(true, resultAddCustomer.isRight())

        val resultAddSource1 = paymentProcessor.addSource(resultAddCustomer.get().id, createPaymentSourceId())
        assertEquals(true, resultAddSource1.isRight())

        val resultAddSource2 = paymentProcessor.addSource(resultAddCustomer.get().id, createPaymentSourceId())
        assertEquals(true, resultAddSource2.isRight())

        assertEquals(resultAddSource1.get().id, resultAddSource2.get().id)

        val resultDeleteSource1 = paymentProcessor.removeSource(resultAddCustomer.get().id ,resultAddSource1.get().id)
        assertEquals(true, resultDeleteSource1.isRight())

        val resultDeleteSource2 = paymentProcessor.removeSource(resultAddCustomer.get().id ,resultAddSource2.get().id)
        assertEquals(true, resultDeleteSource2.isRight())

        val resultDeleteCustomer = paymentProcessor.deletePaymentProfile(resultAddCustomer.get().id)
        assertEquals(true, resultDeleteCustomer.isRight())
    }
    */

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

}