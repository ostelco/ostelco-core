package org.ostelco.at.okhttp

import org.junit.Test
import org.ostelco.at.common.StripePayment
import org.ostelco.at.common.createProfile
import org.ostelco.at.common.createSubscription
import org.ostelco.at.common.expectedProducts
import org.ostelco.at.common.logger
import org.ostelco.at.common.randomInt
import org.ostelco.at.okhttp.ClientFactory.clientForSubject
import org.ostelco.prime.client.model.Consent
import org.ostelco.prime.client.model.Price
import org.ostelco.prime.client.model.Product
import org.ostelco.prime.client.model.Profile
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProfileTest {

    @Test
    fun `okhttp test - GET and PUT profile`() {

        val email = "profile-${randomInt()}@test.com"

        val client = clientForSubject(subject = email)

        val createProfile = Profile()
                .email(email)
                .name("Test Profile User")
                .address("")
                .city("")
                .country("")
                .postCode("")
                .referralId("")

        client.createProfile(createProfile)

        val profile: Profile = client.profile

        assertEquals(email, profile.email, "Incorrect 'email' in fetched profile")
        assertEquals(createProfile.name, profile.name, "Incorrect 'name' in fetched profile")
        assertEquals(email, profile.referralId, "Incorrect 'referralId' in fetched profile")

        profile
                .address("Some place")
                .postCode("418")
                .city("Udacity")
                .country("Online")

        val updatedProfile: Profile = client.updateProfile(profile)

        assertEquals(email, updatedProfile.email, "Incorrect 'email' in response after updating profile")
        assertEquals(createProfile.name, updatedProfile.name, "Incorrect 'name' in response after updating profile")
        assertEquals("Some place", updatedProfile.address, "Incorrect 'address' in response after updating profile")
        assertEquals("418", updatedProfile.postCode, "Incorrect 'postcode' in response after updating profile")
        assertEquals("Udacity", updatedProfile.city, "Incorrect 'city' in response after updating profile")
        assertEquals("Online", updatedProfile.country, "Incorrect 'country' in response after updating profile")

        updatedProfile
                .address("")
                .postCode("")
                .city("")
                .country("")

        val clearedProfile: Profile = client.updateProfile(updatedProfile)

        assertEquals(email, clearedProfile.email, "Incorrect 'email' in response after clearing profile")
        assertEquals(createProfile.name, clearedProfile.name, "Incorrect 'name' in response after clearing profile")
        assertEquals("", clearedProfile.address, "Incorrect 'address' in response after clearing profile")
        assertEquals("", clearedProfile.postCode, "Incorrect 'postcode' in response after clearing profile")
        assertEquals("", clearedProfile.city, "Incorrect 'city' in response after clearing profile")
        assertEquals("", clearedProfile.country, "Incorrect 'country' in response after clearing profile")
    }
}

class GetSubscriptions {

    @Test
    fun `okhttp test - GET subscriptions`() {

        val email = "subs-${randomInt()}@test.com"
        createProfile(name = "Test Subscriptions User", email = email)
        val msisdn = createSubscription(email)

        val client = clientForSubject(subject = email)

        val subscriptions = client.subscriptions

        assertEquals(listOf(msisdn), subscriptions.map { it.msisdn })
    }
}

class GetSubscriptionStatusTest {

    private val logger by logger()

    @Test
    fun `okhttp test - GET subscription status`() {

        val email = "balance-${randomInt()}@test.com"
        createProfile(name = "Test Balance User", email = email)

        val client = clientForSubject(subject = email)

        val subscriptionStatus = client.subscriptionStatus

        logger.info("Balance: ${subscriptionStatus.remaining}")

        val freeProduct = Product()
                .sku("100MB_FREE_ON_JOINING")
                .price(Price().apply {
                    this.amount = 0
                    this.currency = "NOK"
                })
                .properties(mapOf("noOfBytes" to "100_000_000"))
                .presentation(emptyMap<String, String>())

        val purchaseRecords = subscriptionStatus.purchaseRecords
        purchaseRecords.sortBy { it.timestamp }

        assertEquals(freeProduct, purchaseRecords.first().product, "Incorrect first 'Product' in purchase record")
    }
}

class GetPseudonymsTest {

    private val logger by logger()

    @Test
    fun `okhttp test - GET active pseudonyms`() {

        val email = "pseu-${randomInt()}@test.com"
        createProfile(name = "Test Pseudonyms User", email = email)

        val client = clientForSubject(subject = email)

        createSubscription(email)

        val activePseudonyms = client.activePseudonyms

        logger.info("Current: ${activePseudonyms.current.pseudonym}")
        logger.info("Next: ${activePseudonyms.next.pseudonym}")
        assertNotNull(activePseudonyms.current.pseudonym, "Empty current pseudonym")
        assertNotNull(activePseudonyms.next.pseudonym, "Empty next pseudonym")
        assertEquals(activePseudonyms.current.end + 1, activePseudonyms.next.start, "The pseudonyms are not in order")
    }
}

class GetProductsTest {

    @Test
    fun `okhttp test - GET products`() {

        val email = "products-${randomInt()}@test.com"
        createProfile(name = "Test Products User", email = email)

        val client = clientForSubject(subject = email)

        val products = client.allProducts.toList()

        assertEquals(expectedProducts().toSet(), products.toSet(), "Incorrect 'Products' fetched")
    }
}

class PurchaseTest {

    @Test
    fun `okhttp test - POST products purchase`() {

        StripePayment.deleteAllCustomers()

        val email = "purchase-${randomInt()}@test.com"
        createProfile(name = "Test Purchase User", email = email)

        val client = clientForSubject(subject = email)

        val balanceBefore = client.subscriptionStatus.remaining

        val sourceId = StripePayment.createPaymentSourceId()

        client.purchaseProduct("1GB_249NOK", sourceId, false)

        Thread.sleep(200) // wait for 200 ms for balance to be updated in db

        val balanceAfter = client.subscriptionStatus.remaining

        assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

        val purchaseRecords = client.purchaseHistory

        purchaseRecords.sortBy { it.timestamp }

        assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
        assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
    }

    @Test
    fun `okhttp test - POST products purchase without payment`() {

        val email = "purchase-legacy-${randomInt()}@test.com"
        createProfile(name = "Test Legacy Purchase User", email = email)

        val client = clientForSubject(subject = email)

        val balanceBefore = client.subscriptionStatus.remaining

        client.buyProductDeprecated("1GB_249NOK")

        Thread.sleep(200) // wait for 200 ms for balance to be updated in db

        val balanceAfter = client.subscriptionStatus.remaining

        assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

        val purchaseRecords = client.purchaseHistory

        purchaseRecords.sortBy { it.timestamp }

        assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
        assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
    }
}

class ConsentTest {

    private val consentId = "privacy"

    @Test
    fun `okhttp test - GET and PUT consent`() {

        val email = "consent-${randomInt()}@test.com"
        createProfile(name = "Test Consent User", email = email)

        val client = clientForSubject(subject = email)

        val defaultConsent: List<Consent> = client.consents.toList()

        assertEquals(1, defaultConsent.size, "Incorrect number of consents fetched")
        assertEquals(consentId, defaultConsent[0].consentId, "Incorrect 'consent id' in fetched consent")

        // TODO vihang: Update consent operation is missing response entity
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