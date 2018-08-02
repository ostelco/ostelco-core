package org.ostelco.at.okhttp

import org.junit.Test
import org.ostelco.at.common.expectedProducts
import org.ostelco.at.okhttp.OkHttpClient.client
import org.ostelco.prime.client.model.Consent
import org.ostelco.prime.client.model.Profile
import org.ostelco.prime.logger
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetBalanceTest {

    private val logger by logger()

    @Test
    fun testGetBalance() {
        val subscriptions = client.subscriptions

        subscriptions.forEach { logger.info("Balance: ${it.balance}") }
    }

    @Test
    fun testGetBalanceUsingStatus() {
        val subscriptionStatus = client.subscriptionStatus

        logger.info("Balance: ${subscriptionStatus.remaining}")
        subscriptionStatus.purchaseRecords.forEach {
            assertEquals("4747900184", it.msisdn, "Incorrect 'MSISDN' in purchase record")
            assertEquals(expectedProducts().first(), it.product, "Incorrect 'Product' in purchase record")
        }
    }
}
class GetPseudonymsTest {

    private val logger by logger()

    @Test
    fun testGetActivePseudonyms() {
        val activePseudonyms = client.activePseudonyms

        logger.info("Current: ${activePseudonyms.current.pseudonym}")
        logger.info("Next: ${activePseudonyms.next.pseudonym}")
        assertNotNull(activePseudonyms.current.pseudonym,"Empty current pseudonym")
        assertNotNull(activePseudonyms.next.pseudonym,"Empty next pseudonym")
        assertEquals(activePseudonyms.current.end+1, activePseudonyms.next.start, "The pseudonyms are not in order")
    }
}

class GetProductsTest {

    @Test
    fun testGetProducts() {
        val products = client.allProducts.toList()
        assertEquals(expectedProducts().toSet(), products.toSet(), "Incorrect 'Products' fetched")
    }
}

class PurchaseTest {

    @Test
    fun testPurchase() {
//        val balanceBefore = client.subscription.remaining

        client.buyProduct("1GB_249NOK")

        val subscriptionStatusAfter = client.subscriptionStatus
        // TODO Test to check updated balance after purchase is not working
//        val balanceAfter = subscriptionStatusAfter.remaining
//        assertEquals(1L*1024*124*1024, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")
        assert(Instant.now().toEpochMilli() - subscriptionStatusAfter.purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }

        val purchaseRecordList = client.purchaseHistory
        assert(Instant.now().toEpochMilli() - purchaseRecordList.last().timestamp < 10_000) { "Missing Purchase Record" }
    }
}

class ConsentTest {

    private val consentId = "privacy"

    @Test
    fun testConsent() {

        val defaultConsent: List<Consent> = client.consents.toList()
        assertEquals(1, defaultConsent.size, "Incorrect number of consents fetched")
        assertEquals(consentId, defaultConsent[0].consentId, "Incorrect 'consent id' in fetched consent")

        // TODO Update consent operation is missing response entity
        // val acceptedConsent: Consent =
        client.updateConsent(consentId, true)

//        assertEquals(consentId, acceptedConsent.consentId, "Incorrect 'consent id' in response after accepting consent")
//        assertTrue(acceptedConsent.isAccepted ?: false, "Accepted consent not reflected in response after accepting consent")

//        val rejectedConsent: Consent =
        client.updateConsent(consentId, false)

//        assertEquals(consentId, rejectedConsent.consentId, "Incorrect 'consent id' in response after rejecting consent")
//        assertFalse(rejectedConsent.isAccepted ?: true, "Accepted consent not reflected in response after rejecting consent")
    }
}

class ProfileTest {

    @Test
    fun testProfile() {

        val profile: Profile = client.profile

        assertEquals("foo@bar.com", profile.email, "Incorrect 'email' in fetched profile")
        assertEquals("Test User", profile.name, "Incorrect 'name' in fetched profile")

        profile.address = "Some place"
        profile.postCode = "418"
        profile.city = "Udacity"
        profile.country = "Online"

        val updatedProfile: Profile = client.updateProfile(profile)

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

        val clearedProfile: Profile = client.updateProfile(updatedProfile)

        assertEquals("foo@bar.com", clearedProfile.email, "Incorrect 'email' in response after clearing profile")
        assertEquals("Test User", clearedProfile.name, "Incorrect 'name' in response after clearing profile")
        assertEquals("", clearedProfile.address, "Incorrect 'address' in response after clearing profile")
        assertEquals("", clearedProfile.postCode, "Incorrect 'postcode' in response after clearing profile")
        assertEquals("", clearedProfile.city, "Incorrect 'city' in response after clearing profile")
        assertEquals("", clearedProfile.country, "Incorrect 'country' in response after clearing profile")
    }
}