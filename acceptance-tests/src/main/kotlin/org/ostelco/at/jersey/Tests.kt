package org.ostelco.at.jersey

import org.junit.Test
import org.ostelco.at.common.expectedProducts
import org.ostelco.prime.client.model.ActivePseudonyms
import org.ostelco.prime.client.model.Consent
import org.ostelco.prime.client.model.Product
import org.ostelco.prime.client.model.Profile
import org.ostelco.prime.client.model.SubscriptionStatus
import org.ostelco.prime.logger
import org.ostelco.prime.model.ApplicationToken
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetBalanceTest {

    private val LOG by logger()

    @Test
    fun testGetBalance() {

        val subscriptionStatus: SubscriptionStatus = get {
            path = "/subscription/status"
        }

        LOG.info("Balance: ${subscriptionStatus.remaining}")
        subscriptionStatus.purchaseRecords.forEach {
            assertEquals("4747900184", it.msisdn, "Incorrect 'MSISDN' in purchase record")
            assertEquals(expectedProducts().first(), it.product, "Incorrect 'Product' in purchase record")
        }
    }
}

class GetPseudonymsTest {

    private val LOG by logger()

    @Test
    fun testGetActivePseudonyms() {

        val activePseudonyms: ActivePseudonyms = get {
            path = "/subscription/activePseudonyms"
        }

        LOG.info("Current: ${activePseudonyms.current.pseudonym}")
        LOG.info("Next: ${activePseudonyms.next.pseudonym}")
        assertNotNull(activePseudonyms.current.pseudonym,"Empty current pseudonym")
        assertNotNull(activePseudonyms.next.pseudonym,"Empty next pseudonym")
        assertEquals(activePseudonyms.current.end+1, activePseudonyms.next.start, "The pseudonyms are not in order")
    }
}

class GetProductsTest {

    @Test
    fun testGetProducts() {

        val products: List<Product> = get {
            path = "/products"
        }

        assertEquals(expectedProducts(), products, "Incorrect 'Products' fetched")
    }
}

class PurchaseTest {

    @Test
    fun testPurchase() {

//        val subscriptionStatusBefore: SubscriptionStatus = get {
//            path = "/subscription/status"
//        }
//        val balanceBefore = subscriptionStatusBefore.remaining

        val productSku = "1GB_249NOK"

        post<String> {
            path = "/products/$productSku"
        }

        val subscriptionStatusAfter: SubscriptionStatus = get {
            path = "/subscription/status"
        }

        // TODO Test to check updated balance after purchase is not working
//        val balanceAfter = subscriptionStatusBefore.remaining
//        assertEquals(1L*1024*124*1024, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")
        assert(Instant.now().toEpochMilli() - subscriptionStatusAfter.purchaseRecords.last().timestamp < 10_000, { "Missing Purchase Record" })
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
        assertEquals(1, defaultConsent.size, "Incorrect number of consents fetched")
        assertEquals(consentId, defaultConsent[0].consentId, "Incorrect 'consent id' in fetched consent")

        val acceptedConsent: Consent = put {
            path = "/consents/$consentId"
        }

        assertEquals(consentId, acceptedConsent.consentId, "Incorrect 'consent id' in response after accepting consent")
        assertTrue(acceptedConsent.isAccepted ?: false, "Accepted consent not reflected in response after accepting consent")

        val rejectedConsent: Consent = put {
            path = "/consents/$consentId"
            queryParams = mapOf("accepted" to "false")
        }

        assertEquals(consentId, rejectedConsent.consentId, "Incorrect 'consent id' in response after rejecting consent")
        assertFalse(rejectedConsent.isAccepted ?: true, "Accepted consent not reflected in response after rejecting consent")
    }
}

class ProfileTest {

    @Test
    fun testProfile() {

        val profile: Profile = get {
            path = "/profile"
        }

        assertEquals("foo@bar.com", profile.email, "Incorrect 'email' in fetched profile")
        assertEquals("Test User", profile.name, "Incorrect 'name' in fetched profile")

        profile.address = "Some place"
        profile.postCode = "418"
        profile.city = "Udacity"
        profile.country = "Online"

        val updatedProfile: Profile = put {
            path = "/profile"
            body = profile
        }

        assertEquals("foo@bar.com", updatedProfile.email, "Incorrect 'email' in response after updating profile")
        assertEquals("Test User", updatedProfile.name, "Incorrect 'name' in response after updating profile")
        assertEquals("Some place", updatedProfile.address, "Incorrect 'address' in response after updating profile")
        assertEquals("418", updatedProfile.postCode, "Incorrect 'postcode' in response after updating profile")
        assertEquals("Udacity", updatedProfile.city, "Incorrect 'city' in response after updating profile")
        assertEquals("Online", updatedProfile.country, "Incorrect 'country' in response after updating profile")

        updatedProfile.address = ""
        updatedProfile.postCode = ""
        updatedProfile.city = ""
        updatedProfile.country = ""

        val clearedProfile: Profile = put {
            path = "/profile"
            body = updatedProfile
        }

        assertEquals("foo@bar.com", clearedProfile.email, "Incorrect 'email' in response after clearing profile")
        assertEquals("Test User", clearedProfile.name, "Incorrect 'name' in response after clearing profile")
        assertEquals("", clearedProfile.address, "Incorrect 'address' in response after clearing profile")
        assertEquals("", clearedProfile.postCode, "Incorrect 'postcode' in response after clearing profile")
        assertEquals("", clearedProfile.city, "Incorrect 'city' in response after clearing profile")
        assertEquals("", clearedProfile.country, "Incorrect 'country' in response after clearing profile")
    }

    @Test
    fun testApplicationToken() {

        val token = UUID.randomUUID().toString()
        val applicationId = "testApplicationId"
        val tokenType = "FCM"

        val testToken = ApplicationToken(token, applicationId, tokenType)

        val reply: ApplicationToken = post {
            path = "/applicationtoken"
            body = testToken
        }
    }
}