package org.ostelco.at

import org.junit.Test
import org.ostelco.prime.client.model.Consent
import org.ostelco.prime.client.model.Price
import org.ostelco.prime.client.model.Product
import org.ostelco.prime.client.model.Profile
import org.ostelco.prime.client.model.SubscriptionStatus
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetBalanceTest {

    @Test
    fun testGetBalance() {

        get<SubscriptionStatus> {
            path = "/subscription/status"
        }
    }
}

class GetProductsTest {

    @Test
    fun testGetProducts() {

        val products: List<Product> = get {
            path = "/products"
        }

        assertEquals(expectedProducts(), products)
    }
}

class PurchaseTest {

    @Test
    fun testPurchase() {

        val productSku = "1GB_249NOK"

        post<String> {
            path = "/products/$productSku"
        }
    }
}

class AnalyticsTest {

    @Test
    fun testReportEvent() {

        post<String> {
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

        val acceptedConsent: Consent = put {
            path = "/consents/$consentId"
        }

        assertEquals(consentId, acceptedConsent.consentId)
        assertTrue(acceptedConsent.isAccepted ?: false)

        val rejectedConsent: Consent = put {
            path = "/consents/$consentId"
            queryParams = mapOf("accepted" to "false")
        }

        assertEquals(consentId, rejectedConsent.consentId)
        assertFalse(rejectedConsent.isAccepted ?: true)
    }
}

class ProfileTest {

    @Test
    fun testProfile() {

        val profile: Profile = get {
            path = "/profile"
        }

        assertEquals("foo@bar.com", profile.email)
        assertEquals("Test User", profile.name)

        profile.address = "Some place"
        profile.postCode = "418"
        profile.city = "Udacity"
        profile.country = "Online"

        val updatedProfile: Profile = put {
            path = "/profile"
            body = profile
        }

        assertEquals("foo@bar.com", updatedProfile.email)
        assertEquals("Test User", updatedProfile.name)
        assertEquals("Some place", updatedProfile.address)
        assertEquals("418", updatedProfile.postCode)
        assertEquals("Udacity", updatedProfile.city)
        assertEquals("Online", updatedProfile.country)

        updatedProfile.address = ""
        updatedProfile.postCode = ""
        updatedProfile.city = ""
        updatedProfile.country = ""

        val clearedProfile: Profile = put {
            path = "/profile"
            body = updatedProfile
        }

        assertEquals("foo@bar.com", clearedProfile.email)
        assertEquals("Test User", clearedProfile.name)
        assertEquals("", clearedProfile.address)
        assertEquals("", clearedProfile.postCode)
        assertEquals("", clearedProfile.city)
        assertEquals("", clearedProfile.country)
    }
}

private fun expectedProducts(): List<Product> {
    return listOf(
            createProduct("1GB_249NOK", 24900),
            createProduct("2GB_299NOK", 29900),
            createProduct("3GB_349NOK", 34900),
            createProduct("5GB_399NOK", 39900))
}

private fun createProduct(sku: String, amount: Int): Product {
    val product = Product()
    product.sku = sku
    product.price = Price()
    product.price.amount = amount
    product.price.currency = "NOK"
    return product
}