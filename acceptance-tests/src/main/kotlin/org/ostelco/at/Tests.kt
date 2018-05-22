package org.ostelco.at

import org.junit.Test
import org.ostelco.prime.client.model.Consent
import org.ostelco.prime.client.model.Price
import org.ostelco.prime.client.model.Product
import org.ostelco.prime.client.model.Profile
import org.ostelco.prime.client.model.SubscriptionStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetBalanceTest {

    @Test
    fun testGetBalance() {
        val subscriptionStatus: SubscriptionStatus = get {
            path = "/subscription/status"
        }

        val expectedSubscriptionStatus = SubscriptionStatus()
        expectedSubscriptionStatus.remaining = 1_000_000_000
        val product = Product()
        product.sku = "DataTopup3GB"
        product.price = Price()
        product.price.amount = 250
        product.price.currency = "NOK"
        expectedSubscriptionStatus.acceptedProducts = listOf(product)

        assertEquals(expectedSubscriptionStatus, subscriptionStatus)
    }
}

class GetProductsTest {

    @Test
    fun testGetProducts() {

        val products: List<Product> = get {
            path = "/products"
        }

        val product = Product()
        product.sku = "DataTopup3GB"
        product.price = Price()
        product.price.amount = 250
        product.price.currency = "NOK"
        val expectedProducts = listOf(product)

        assertEquals(expectedProducts, products)
    }
}

class PurchaseTest {

    @Test
    fun testPurchase() {

        val productSku = "DataTopup3GB"

        post {
            path = "/products/$productSku"
        }
    }
}

class AnalyticsTest {

    @Test
    fun testReportEvent() {

        post {
            path = "/analytics"
            body = "event"
        }
    }
}

class ConsentTest {

    private val consentId = "privacy"

    @Test
    fun testConsent() {

        val defaultConsent: List<Consent> = get {
            path = "/consents"
        }
        assertEquals(1, defaultConsent.size)
        assertEquals(consentId, defaultConsent[0].consentId)

        put {
            path = "/consents/$consentId"
        }

        val acceptedConsent: List<Consent> = get {
            path = "/consents"
        }
        assertEquals(1, acceptedConsent.size)
        assertEquals(consentId, acceptedConsent[0].consentId)
        assertTrue(acceptedConsent[0].isAccepted ?: false)

        put {
            path = "/consents/$consentId?accepted=false"
        }

        val rejectedConsent: List<Consent> = get {
            path = "/consents"
        }
        assertEquals(1, rejectedConsent.size)
        assertEquals(consentId, rejectedConsent[0].consentId)
        assertTrue(rejectedConsent[0].isAccepted ?: false)
    }
}

class ProfileTest {

    private val profile: Profile

    init {
        profile = Profile()
        profile.email = "vihang.patil@telenordigital.com"
        profile.name = "Vihang Patil"
    }

    @Test
    fun testProfile() {

        post {
            path = "/profile"
            body = profile
        }

        val profile: Profile = get {
            path = "/profile"
        }

        put {
            path = "/profile"
            body = profile
        }
    }
}

