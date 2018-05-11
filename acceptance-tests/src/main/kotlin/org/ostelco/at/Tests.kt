package org.ostelco.at

import org.junit.Test
import org.ostelco.topup.api.core.Consent
import org.ostelco.topup.api.core.Product
import org.ostelco.topup.api.core.Profile
import org.ostelco.topup.api.core.SubscriptionStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetBalanceTest {

    @Test
    fun testGetBalance() {
        val subscriptionStatus: SubscriptionStatus = get {
            path = "/subscription/status"
        }
        assertEquals(subscriptionStatus, SubscriptionStatus(1_000_000_000,
                listOf(Product("DataTopup3GB", 250f, "NOK"))))
    }
}

class GetProductsTest {

    @Test
    fun testGetProducts() {

        val products: List<Product> = get {
            path = "/products"
        }

        val expectedProducts: List<Product> = arrayListOf(
                Product("DataTopup3GB", 250f, "NOK"))

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
        assertTrue(acceptedConsent[0].isAccepted)

        put {
            path = "/consents/$consentId?accepted=false"
        }

        val rejectedConsent: List<Consent> = get {
            path = "/consents"
        }
        assertEquals(1, rejectedConsent.size)
        assertEquals(consentId, rejectedConsent[0].consentId)
        assertTrue(rejectedConsent[0].isAccepted)
    }
}

class ProfileTest {

    private val profile = Profile("vihang.patil@telenordigital.com")

    init {
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

