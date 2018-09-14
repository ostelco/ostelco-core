package org.ostelco.at.okhttp

import org.junit.Test
import org.ostelco.at.common.Firebase
import org.ostelco.at.common.StripePayment
import org.ostelco.at.common.createProfile
import org.ostelco.at.common.createSubscription
import org.ostelco.at.common.expectedProducts
import org.ostelco.at.common.logger
import org.ostelco.at.common.randomInt
import org.ostelco.at.okhttp.ClientFactory.clientForSubject
import org.ostelco.prime.client.model.ApplicationToken
import org.ostelco.prime.client.model.Consent
import org.ostelco.prime.client.model.PaymentSource
import org.ostelco.prime.client.model.Person
import org.ostelco.prime.client.model.PersonList
import org.ostelco.prime.client.model.Price
import org.ostelco.prime.client.model.Product
import org.ostelco.prime.client.model.Profile
import org.ostelco.prime.client.model.SubscriptionStatus
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

        client.createProfile(createProfile, null)

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

    @Test
    fun `okhttp test - GET application token`() {

        val email = "token-${randomInt()}@test.com"
        createProfile("Test Token User", email)

        createSubscription(email)

        val token = UUID.randomUUID().toString()
        val applicationId = "testApplicationId"
        val tokenType = "FCM"

        val testToken = ApplicationToken()
                .token(token)
                .applicationID(applicationId)
                .tokenType(tokenType)

        val client = clientForSubject(subject = email)

        val reply = client.storeApplicationToken(testToken)

        assertEquals(token, reply.token, "Incorrect token in reply after posting new token")
        assertEquals(applicationId, reply.applicationID, "Incorrect applicationId in reply after posting new token")
        assertEquals(tokenType, reply.tokenType, "Incorrect tokenType in reply after posting new token")
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

class SourceTest {

    @Test
    fun `okhttp test - POST source create`() {

        StripePayment.deleteAllCustomers()
        Firebase.deleteAllPaymentCustomers()

        val email = "purchase-${randomInt()}@test.com"
        createProfile(name = "Test Payment Source", email = email)

        val client = clientForSubject(subject = email)

        val sourceId = StripePayment.createPaymentTokenId()

        // Ties source with user profile both local and with Stripe
        client.createSource(sourceId)

        Thread.sleep(200)

        val sources = client.listSources()
        assert(sources.isNotEmpty()) { "Expected at least one payment source for profile $email" }
        assertNotNull(sources.first { it.id == cardId },
                "Expected card $cardId in list of payment sources for profile $email")
    }

    @Test
    fun `okhttp test - GET list sources`() {

        StripePayment.deleteAllCustomers()
        Firebase.deleteAllPaymentCustomers()

        val email = "purchase-${randomInt()}@test.com"
        createProfile(name = "Test Payment Source", email = email)

        val client = clientForSubject(subject = email)

        val tokenId = StripePayment.createPaymentTokenId()
        val cardId = StripePayment.getCardIdForTokenId(tokenId)

        // Ties source with user profile both local and with Stripe
        client.createSource(tokenId)

        Thread.sleep(200)

        val newTokenId = StripePayment.createPaymentTokenId()
        val newCardId = StripePayment.getCardIdForTokenId(newTokenId)

        client.createSource(newTokenId)

        val sources = client.listSources()

        assert(sources.isNotEmpty()) { "Expected at least one payment source for profile $email" }
        assert(sources.map{ it.id }.containsAll(listOf(cardId, newCardId)))
        { "Expected to find both $cardId and $newCardId in list of sources for profile $email" }

        sources.forEach {
            assert(it.details.id.isNotEmpty()) { "Expected 'id' to be set in source account details for profile $email" }
            assertEquals("card", it.details.accountType,
                    "Unexpected source account type ${it.details.accountType} for profile $email")
        }
    }

    @Test
    fun `okhttp test - PUT source set default`() {

        StripePayment.deleteAllCustomers()
        Firebase.deleteAllPaymentCustomers()

        val email = "purchase-${randomInt()}@test.com"
        createProfile(name = "Test Payment Source", email = email)

        val client = clientForSubject(subject = email)

        val tokenId = StripePayment.createPaymentTokenId()
        val cardId = StripePayment.getCardIdForTokenId(tokenId)

        // Ties source with user profile both local and with Stripe
        client.createSource(tokenId)

        Thread.sleep(200)

        val newTokenId = StripePayment.createPaymentTokenId()
        val newCardId = StripePayment.getCardIdForTokenId(newTokenId)

        client.createSource(newTokenId)

        // TODO: Update to fetch the Stripe customerId from 'admin' API when ready.
        val customerId = StripePayment.getCustomerIdForEmail(email)

        // Verify that original 'sourceId/card' is default.
        assertEquals(cardId, StripePayment.getDefaultSourceForCustomer(customerId),
                "Expected $cardId to be default source for $customerId")

        // Set new default card.
        client.setDefaultSource(newCardId)

        val cardId = StripePayment.getCardIdForTokenId(sourceId)
        assertNotNull(sources.first { it.id == cardId }, "Expected card $cardId in list of payment sources for profile $email")
    }
}

class PurchaseTest {

    @Test
    fun `okhttp test - POST products purchase`() {

        StripePayment.deleteAllCustomers()
        Firebase.deleteAllPaymentCustomers()

        val email = "purchase-${randomInt()}@test.com"
        createProfile(name = "Test Purchase User", email = email)

        val client = clientForSubject(subject = email)

        val balanceBefore = client.subscriptionStatus.remaining

        val sourceId = StripePayment.createPaymentTokenId()

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
    fun `okhttp test - POST products purchase using default source`() {

        StripePayment.deleteAllCustomers()
        Firebase.deleteAllPaymentCustomers()

        val email = "purchase-${randomInt()}@test.com"
        createProfile(name = "Test Purchase User with Default Payment Source", email = email)

        val sourceId = StripePayment.createPaymentTokenId()

        val client = clientForSubject(subject = email)

        val paymentSource: PaymentSource = client.createSource(sourceId)

        assertNotNull(paymentSource.id, message = "Failed to create payment source")

        val balanceBefore = client.subscriptionStatus.remaining

        val productSku = "1GB_249NOK"

        client.purchaseProduct(productSku, null, null)

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
//         val acceptedConsent: Consent =
        client.updateConsent(consentId, true)

//        assertEquals(consentId, acceptedConsent.consentId, "Incorrect 'consent id' in response after accepting consent")
//        assertTrue(acceptedConsent.isAccepted ?: false, "Accepted consent not reflected in response after accepting consent")

//        val rejectedConsent: Consent =
        client.updateConsent(consentId, false)

//        assertEquals(consentId, rejectedConsent.consentId, "Incorrect 'consent id' in response after rejecting consent")
//        assertFalse(rejectedConsent.isAccepted ?: true, "Accepted consent not reflected in response after rejecting consent")
    }
}

class ReferralTest {

    @Test
    fun `okhttp test - POST profile with invalid referred by`() {

        val email = "referred_by_invalid-${randomInt()}@test.com"

        val client = clientForSubject(subject = email)

        val invalid = "invalid_referrer@test.com"

        val profile = Profile()
                .email(email)
                .name("Test Referral Second User")
                .address("")
                .city("")
                .country("")
                .postCode("")
                .referralId("")

        val failedToCreate = assertFails {
            client.createProfile(profile, invalid)
        }

        assertEquals("""
{"description":"Incomplete profile description. Subscriber - $invalid not found."} expected:<201> but was:<403>
        """.trimIndent(), failedToCreate.message)

        val failedToGet = assertFails {
            client.profile
        }

        assertEquals("""
{"description":"Incomplete profile description. Subscriber - $email not found."} expected:<200> but was:<404>
        """.trimIndent(), failedToGet.message)
    }

    @Test
    fun `okhttp test - POST profile`() {

        val firstEmail = "referral_first-${randomInt()}@test.com"
        createProfile(name = "Test Referral First User", email = firstEmail)

        val secondEmail = "referral_second-${randomInt()}@test.com"

        val profile = Profile()
                .email(secondEmail)
                .name("Test Referral Second User")
                .address("")
                .city("")
                .country("")
                .postCode("")
                .referralId("")

        val firstEmailClient = clientForSubject(subject = firstEmail)
        val secondEmailClient = clientForSubject(subject = secondEmail)

        secondEmailClient.createProfile(profile, firstEmail)

        // for first
        val referralsForFirst: PersonList = firstEmailClient.referred

        assertEquals(listOf("Test Referral Second User"), referralsForFirst.map { it.name })

        val referredByForFirst: Person = firstEmailClient.referredBy
        assertNull(referredByForFirst.name)

        // No need to test SubscriptionStatus for first, since it is already tested in GetSubscriptionStatusTest.

        // for referred_by_foo
        val referralsForSecond: List<Person> = secondEmailClient.referred

        assertEquals(emptyList(), referralsForSecond.map { it.name })

        val referredByForSecond: Person = secondEmailClient.referredBy

        assertEquals("Test Referral First User", referredByForSecond.name)

        val secondSubscriptionStatus: SubscriptionStatus = secondEmailClient.subscriptionStatus

        assertEquals(1_000_000_000, secondSubscriptionStatus.remaining)

        val freeProductForReferred = Product()
                .sku("1GB_FREE_ON_REFERRED")
                .price(Price().apply {
                    this.amount = 0
                    this.currency = "NOK"
                })
                .properties(mapOf("noOfBytes" to "1_000_000_000"))
                .presentation(emptyMap<String, String>())

        assertEquals(listOf(freeProductForReferred), secondSubscriptionStatus.purchaseRecords.map { it.product })
    }
}