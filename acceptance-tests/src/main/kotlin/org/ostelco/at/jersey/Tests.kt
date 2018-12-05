package org.ostelco.at.jersey

import org.junit.Test
import org.ostelco.at.common.*
import org.ostelco.prime.client.model.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import kotlin.collections.HashMap
import kotlin.test.*


class ProfileTest {

    @Test
    fun `jersey test - GET and PUT profile`() {

        val email = "profile-${randomInt()}@test.com"

        val createProfile = Profile()
                .email(email)
                .name("Test Profile User")
                .address("")
                .city("")
                .country("NO")
                .postCode("")
                .referralId("")

        val createdProfile: Profile = post {
            path = "/profile"
            body = createProfile
            subscriberId = email
        }

        assertEquals(createProfile.email, createdProfile.email, "Incorrect 'email' in created profile")
        assertEquals(createProfile.name, createdProfile.name, "Incorrect 'name' in created profile")
        assertEquals(createProfile.email, createdProfile.referralId, "Incorrect 'referralId' in created profile")

        val profile: Profile = get {
            path = "/profile"
            subscriberId = email
        }

        assertEquals(email, profile.email, "Incorrect 'email' in fetched profile")
        assertEquals(createProfile.name, profile.name, "Incorrect 'name' in fetched profile")
        assertEquals(email, profile.referralId, "Incorrect 'referralId' in fetched profile")

        profile
                .address("Some place")
                .postCode("418")
                .city("Udacity")
                .country("Online")

        val updatedProfile: Profile = put {
            path = "/profile"
            body = profile
            subscriberId = email
        }

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

        val clearedProfile: Profile = put {
            path = "/profile"
            body = updatedProfile
            subscriberId = email
        }

        assertEquals(email, clearedProfile.email, "Incorrect 'email' in response after clearing profile")
        assertEquals(createProfile.name, clearedProfile.name, "Incorrect 'name' in response after clearing profile")
        assertEquals("", clearedProfile.address, "Incorrect 'address' in response after clearing profile")
        assertEquals("", clearedProfile.postCode, "Incorrect 'postcode' in response after clearing profile")
        assertEquals("", clearedProfile.city, "Incorrect 'city' in response after clearing profile")

        updatedProfile.country("")

        // A test in 'HttpClientUtil' checks for status code 200 while the
        // expected status code is actually 400.
        assertFailsWith(AssertionError::class, "Incorrectly accepts that 'country' is cleared/not set") {
            put {
                path = "/profile"
                body = updatedProfile
                subscriberId = email
            }
        }
    }

    @Test
    fun `jersey test - POST application token`() {

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

        val reply: ApplicationToken = post {
            path = "/applicationtoken"
            body = testToken
            subscriberId = email
        }

        assertEquals(token, reply.token, "Incorrect token in reply after posting new token")
        assertEquals(applicationId, reply.applicationID, "Incorrect applicationId in reply after posting new token")
        assertEquals(tokenType, reply.tokenType, "Incorrect tokenType in reply after posting new token")
    }
}

class GetSubscriptions {

    @Test
    fun `jersey test - GET subscriptions`() {

        val email = "subs-${randomInt()}@test.com"
        createProfile(name = "Test Subscriptions User", email = email)
        val msisdn = createSubscription(email)

        val subscriptions: Collection<Subscription> = get {
            path = "/subscriptions"
            subscriberId = email
        }

        assertEquals(listOf(msisdn), subscriptions.map { it.msisdn })
    }
}

class BundlesAndPurchasesTest {

    private val logger by getLogger()

    @Test
    fun `jersey test - GET bundles`() {

        val email = "balance-${randomInt()}@test.com"
        createProfile(name = "Test Balance User", email = email)

        val bundles: BundleList = get {
            path = "/bundles"
            subscriberId = email
        }

        logger.info("Balance: ${bundles[0].balance}")

        val freeProduct = Product()
                .sku("100MB_FREE_ON_JOINING")
                .price(Price().apply {
                    this.amount = 0
                    this.currency = "NOK"
                })
                .properties(mapOf("noOfBytes" to "100_000_000"))
                .presentation(emptyMap<String, String>())

        val purchaseRecords: PurchaseRecordList = get {
            path = "/purchases"
            subscriberId = email
        }
        purchaseRecords.sortBy { it.timestamp }

        assertEquals(listOf(freeProduct), purchaseRecords.map { it.product }, "Incorrect first 'Product' in purchase record")
    }
}

class GetPseudonymsTest {

    private val logger by getLogger()

    @Test
    fun `jersey test - GET active pseudonyms`() {

        val email = "pseu-${randomInt()}@test.com"
        createProfile(name = "Test Pseudonyms User", email = email)

        createSubscription(email)

        val activePseudonyms: ActivePseudonyms = get {
            path = "/subscription/activePseudonyms"
            subscriberId = email
        }

        logger.info("Current: ${activePseudonyms.current.pseudonym}")
        logger.info("Next: ${activePseudonyms.next.pseudonym}")
        assertNotNull(activePseudonyms.current.pseudonym, "Empty current pseudonym")
        assertNotNull(activePseudonyms.next.pseudonym, "Empty next pseudonym")
        assertEquals(activePseudonyms.current.end + 1, activePseudonyms.next.start, "The pseudonyms are not in order")
    }
}

class GetProductsTest {

    @Test
    fun `jersey test - GET products`() {

        val email = "products-${randomInt()}@test.com"
        createProfile(name = "Test Products User", email = email)

        val products: List<Product> = get {
            path = "/products"
            subscriberId = email
        }

        assertEquals(expectedProducts().toSet(), products.toSet(), "Incorrect 'Products' fetched")
    }
}

class SourceTest {

    @Test
    fun `jersey test - POST source create`() {

        val email = "purchase-${randomInt()}@test.com"
        try {

            createProfile(name = "Test Payment Source", email = email)

            val tokenId = StripePayment.createPaymentTokenId()

            // Ties source with user profile both local and with Stripe
            post<PaymentSource> {
                path = "/paymentSources"
                subscriberId = email
                queryParams = mapOf("sourceId" to tokenId)
            }

            Thread.sleep(200)

            val sources: PaymentSourceList = get {
                path = "/paymentSources"
                subscriberId = email
            }
            assert(sources.isNotEmpty()) { "Expected at least one payment source for profile $email" }

            val cardId = StripePayment.getCardIdForTokenId(tokenId)
            assertNotNull(sources.first { it.id == cardId }, "Expected card $cardId in list of payment sources for profile $email")
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - GET list sources`() {

        val email = "purchase-${randomInt()}@test.com"

        try {
            createProfile(name = "Test Payment Source", email = email)

            Thread.sleep(200)

            val createdIds = listOf(createTokenWithStripe(email),
                    createSourceWithStripe(email),
                    createTokenWithStripe(email),
                    createSourceWithStripe(email))

            val sources: PaymentSourceList = get {
                path = "/paymentSources"
                subscriberId = email
            }

            val ids = createdIds.map { getCardIdForTokenFromStripe(it) }

            assert(sources.isNotEmpty()) { "Expected at least one payment source for profile $email" }
            assert(sources.map { it.id }.containsAll(ids))
            { "Expected to find all of $ids in list of sources for profile $email" }

            sources.forEach {
                assert(it.id.isNotEmpty()) { "Expected 'id' to be set in source account details for profile $email" }
                assert(arrayOf("card", "source").contains(it.type)) {
                    "Unexpected source account type ${it.type} for profile $email"
                }
            }
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - GET list sources no profile `() {

        val email = "purchase-${randomInt()}@test.com"

        try {

            val sources: PaymentSourceList = get {
                path = "/paymentSources"
                subscriberId = email
            }

            assert(sources.isEmpty()) { "Expected no payment source for profile $email" }

            assertNotNull(StripePayment.getCustomerIdForEmail(email)) { "Customer Id should have been created" }

        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - PUT source set default`() {

        val email = "purchase-${randomInt()}@test.com"
        try {
            createProfile(name = "Test Payment Source", email = email)

            val tokenId = StripePayment.createPaymentTokenId()
            val cardId = StripePayment.getCardIdForTokenId(tokenId)

            // Ties source with user profile both local and with Stripe
            post<PaymentSource> {
                path = "/paymentSources"
                subscriberId = email
                queryParams = mapOf("sourceId" to tokenId)
            }

            Thread.sleep(200)

            val newTokenId = StripePayment.createPaymentTokenId()
            val newCardId = StripePayment.getCardIdForTokenId(newTokenId)

            post<PaymentSource> {
                path = "/paymentSources"
                subscriberId = email
                queryParams = mapOf("sourceId" to newTokenId)
            }

            // TODO: Update to fetch the Stripe customerId from 'admin' API when ready.
            val customerId = StripePayment.getCustomerIdForEmail(email)

            // Verify that original 'sourceId/card' is default.
            assertEquals(cardId, StripePayment.getDefaultSourceForCustomer(customerId),
                    "Expected $cardId to be default source for $customerId")

            // Set new default card.
            put<PaymentSource> {
                path = "/paymentSources"
                subscriberId = email
                queryParams = mapOf("sourceId" to newCardId)
            }

            assertEquals(newCardId, StripePayment.getDefaultSourceForCustomer(customerId),
                    "Expected $newCardId to be default source for $customerId")
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - DELETE source`() {

        val email = "purchase-${randomInt()}@test.com"

        try {

            createProfile(name = "Test Payment Source", email = email)

            Thread.sleep(200)

            val createdIds = listOf(getCardIdForTokenFromStripe(createTokenWithStripe(email)),
                    createSourceWithStripe(email))

            val deletedIds = createdIds.map { it -> removeSourceWithStripe(email, it) }

            assert(createdIds.containsAll(deletedIds.toSet())) {
                "Failed to delete one or more sources: ${createdIds.toSet() - deletedIds.toSet()}"
            }
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    // Helpers for source handling with Stripe.

    private fun getCardIdForTokenFromStripe(id: String): String {
        if (id.startsWith("tok_")) {
            return StripePayment.getCardIdForTokenId(id)
        }
        return id
    }

    private fun createTokenWithStripe(email: String): String {
        val tokenId = StripePayment.createPaymentTokenId()

        post<PaymentSource> {
            path = "/paymentSources"
            subscriberId = email
            queryParams = mapOf("sourceId" to tokenId)
        }

        return tokenId
    }

    private fun createSourceWithStripe(email: String): String {
        val sourceId = StripePayment.createPaymentSourceId()

        post<PaymentSource> {
            path = "/paymentSources"
            subscriberId = email
            queryParams = mapOf("sourceId" to sourceId)
        }

        return sourceId
    }

    private fun removeSourceWithStripe(email: String, sourceId: String): String {
        val removedSource = delete<PaymentSource> {
            path = "/paymentSources"
            subscriberId = email
            queryParams = mapOf("sourceId" to sourceId)
        }

        return removedSource.id
    }
}

class PurchaseTest {

    private val logger by getLogger()

    @Test
    fun `jersey test - POST products purchase`() {

        val email = "purchase-${randomInt()}@test.com"
        try {
            createProfile(name = "Test Purchase User", email = email)

            val balanceBefore = get<List<Bundle>> {
                path = "/bundles"
                subscriberId = email
            }.first().balance

            val productSku = "1GB_249NOK"
            val sourceId = StripePayment.createPaymentTokenId()

            post<String> {
                path = "/products/$productSku/purchase"
                subscriberId = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            Thread.sleep(100) // wait for 100 ms for balance to be updated in db

            val balanceAfter = get<List<Bundle>> {
                path = "/bundles"
                subscriberId = email
            }.first().balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                subscriberId = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - POST products purchase using default source`() {

        val email = "purchase-${randomInt()}@test.com"
        try {
            createProfile(name = "Test Purchase User with Default Payment Source", email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val paymentSource: PaymentSource = post {
                path = "/paymentSources"
                subscriberId = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            val balanceBefore = get<List<Bundle>> {
                path = "/bundles"
                subscriberId = email
            }.first().balance

            val productSku = "1GB_249NOK"

            post<String> {
                path = "/products/$productSku/purchase"
                subscriberId = email
            }

            Thread.sleep(100) // wait for 100 ms for balance to be updated in db

            val balanceAfter = get<List<Bundle>> {
                path = "/bundles"
                subscriberId = email
            }.first().balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                subscriberId = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - Refund purchase using default source`() {

        val email = "purchase-${randomInt()}@test.com"
        try {
            createProfile(name = "Test Purchase User with Default Payment Source", email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val paymentSource: PaymentSource = post {
                path = "/paymentSources"
                subscriberId = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            val balanceBefore = get<List<Bundle>> {
                path = "/bundles"
                subscriberId = email
            }.first().balance

            val productSku = "1GB_249NOK"

            post<String> {
                path = "/products/$productSku/purchase"
                subscriberId = email
            }

            Thread.sleep(100) // wait for 100 ms for balance to be updated in db

            val balanceAfter = get<List<Bundle>> {
                path = "/bundles"
                subscriberId = email
            }.first().balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                subscriberId = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")

            val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val refundedProduct: ProductInfo = put<ProductInfo> {
                path = "/refund/$encodedEmail"
                subscriberId = email
                queryParams = mapOf(
                        "purchaseRecordId" to purchaseRecords.last().id,
                        "reason" to "requested_by_customer")
            }
            logger.info("Refunded product: $refundedProduct with purchase id:${purchaseRecords.last().id}")
            assertEquals(productSku, refundedProduct.id, "Refund returned a different product")

        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - POST products purchase add source then pay with it`() {

        val email = "purchase-${randomInt()}@test.com"
        try {
            createProfile(name = "Test Purchase User with Default Payment Source", email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val paymentSource: PaymentSource = post {
                path = "/paymentSources"
                subscriberId = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            val bundlesBefore: BundleList = get {
                path = "/bundles"
                subscriberId = email
            }
            val balanceBefore = bundlesBefore[0].balance

            val productSku = "1GB_249NOK"

            post<String> {
                path = "/products/$productSku/purchase"
                subscriberId = email
                queryParams = mapOf("sourceId" to paymentSource.id)
            }

            Thread.sleep(100) // wait for 100 ms for balance to be updated in db

            val bundlesAfter: BundleList = get {
                path = "/bundles"
                subscriberId = email
            }
            val balanceAfter = bundlesAfter[0].balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                subscriberId = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

}

class eKYCTest {
    @Test
    fun `jersey test - GET new-ekyc-scanId - generate new scanId for eKYC`() {

        val email = "ekyc-${randomInt()}@test.com"
        try {
            createProfile(name = "Test User for eKYC", email = email)

            val scanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }
            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val subscriberState: SubscriberState = get {
                path = "/customer/subscriberState"
                subscriberId = email
            }
            assertEquals("REGISTERED", subscriberState.status, message = "Incorrect State")
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }
    @Test
    fun `jersey test - ekyc callback - test the call back processing`() {

        val email = "ekyc-${randomInt()}@test.com"
        try {
            createProfile(name = "Test User for eKYC", email = email)

            val scanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            var dataMap = MultivaluedHashMap<String,String>()
            dataMap.put("jumioIdScanReference", listOf(UUID.randomUUID().toString()));
            dataMap.put("idScanStatus", listOf("ERROR"))
            dataMap.put("verificationStatus", listOf("FRAUD"))
            dataMap.put("callbackDate", listOf("2018-12-07T09:19:07.036Z"))
            dataMap.put("idType", listOf("LICENSE"))
            dataMap.put("idCountry", listOf("NOR"))
            dataMap.put("idFirstName", listOf("Test User"))
            dataMap.put("idLastName", listOf("Test Family"))
            dataMap.put("idDob", listOf("1990-12-09"))
            dataMap.put("merchantIdScanReference", listOf(scanInfo.scanId))

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val subscriberState: SubscriberState = get {
                path = "/customer/subscriberState"
                subscriberId = email
            }
            assertEquals("EKYC_REJECTED", subscriberState.status, message = "Wrong state")

        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }
    @Test
    fun `jersey test - ekyc callback - process success`() {

        val email = "ekyc-${randomInt()}@test.com"
        try {
            createProfile(name = "Test User for eKYC", email = email)

            val scanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            var dataMap = MultivaluedHashMap<String,String>()
            dataMap.put("jumioIdScanReference", listOf(UUID.randomUUID().toString()));
            dataMap.put("idScanStatus", listOf("SUCCESS"))
            dataMap.put("verificationStatus", listOf("APPROVED_VERIFIED"))
            dataMap.put("callbackDate", listOf("2018-12-07T09:19:07.036Z"))
            dataMap.put("idType", listOf("LICENSE"))
            dataMap.put("idCountry", listOf("NOR"))
            dataMap.put("idFirstName", listOf("Test User"))
            dataMap.put("idLastName", listOf("Test Family"))
            dataMap.put("idDob", listOf("1990-12-09"))
            dataMap.put("merchantIdScanReference", listOf(scanInfo.scanId))

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val subscriberState: SubscriberState = get {
                path = "/customer/subscriberState"
                subscriberId = email
            }
            assertEquals("EKYC_APPROVED", subscriberState.status, message = "Wrong state")

        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }
    @Test
    fun `jersey test - ekyc callback - process incomplete form data`() {

        val email = "ekyc-${randomInt()}@test.com"
        try {
            createProfile(name = "Test User for eKYC", email = email)

            val scanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            var dataMap = MultivaluedHashMap<String,String>()
            dataMap.put("jumioIdScanReference", listOf(UUID.randomUUID().toString()));
            dataMap.put("idScanStatus", listOf("SUCCESS"))
            dataMap.put("verificationStatus", listOf("APPROVED_VERIFIED"))
            dataMap.put("callbackDate", listOf("2018-12-07T09:19:07.036Z"))
            dataMap.put("idType", listOf("LICENSE"))
            dataMap.put("idCountry", listOf("NOR"))
            dataMap.put("idFirstName", listOf("Test User"))
            dataMap.put("idLastName", listOf("Test Family"))
            dataMap.put("idDob", listOf("1990-12-09"))
            //dataMap.put("merchantIdScanReference", listOf(scanInfo.scanId))

            post<String>(expectedResultCode = 400, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val subscriberState: SubscriberState = get {
                path = "/customer/subscriberState"
                subscriberId = email
            }
            assertEquals("REGISTERED", subscriberState.status, message = "Wrong state")

        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }
    @Test
    fun `jersey test - ekyc callback - reject & approve`() {

        val email = "ekyc-${randomInt()}@test.com"
        try {
            createProfile(name = "Test User for eKYC", email = email)

            val scanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            var dataMap = MultivaluedHashMap<String, String>()
            dataMap.put("jumioIdScanReference", listOf(UUID.randomUUID().toString()));
            dataMap.put("idScanStatus", listOf("ERROR"))
            dataMap.put("verificationStatus", listOf("FRAUD"))
            dataMap.put("callbackDate", listOf("2018-12-07T09:19:07.036Z"))
            dataMap.put("idType", listOf("LICENSE"))
            dataMap.put("idCountry", listOf("NOR"))
            dataMap.put("idFirstName", listOf("Test User"))
            dataMap.put("idLastName", listOf("Test Family"))
            dataMap.put("idDob", listOf("1990-12-09"))
            dataMap.put("merchantIdScanReference", listOf(scanInfo.scanId))

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val subscriberState: SubscriberState = get {
                path = "/customer/subscriberState"
                subscriberId = email
            }
            assertEquals("EKYC_REJECTED", subscriberState.status, message = "Wrong state")

            val newScanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }

            assertNotNull(newScanInfo.scanId, message = "Failed to get new scanId")

            dataMap.clear()
            dataMap.put("jumioIdScanReference", listOf(UUID.randomUUID().toString()));
            dataMap.put("idScanStatus", listOf("SUCCESS"))
            dataMap.put("verificationStatus", listOf("VERIFIED"))
            dataMap.put("callbackDate", listOf("2018-12-07T09:19:07.036Z"))
            dataMap.put("idType", listOf("LICENSE"))
            dataMap.put("idCountry", listOf("NOR"))
            dataMap.put("idFirstName", listOf("Test User"))
            dataMap.put("idLastName", listOf("Test Family"))
            dataMap.put("idDob", listOf("1990-12-09"))
            dataMap.put("merchantIdScanReference", listOf(newScanInfo.scanId))

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val newSsubscriberState: SubscriberState = get {
                path = "/customer/subscriberState"
                subscriberId = email
            }
            assertEquals("EKYC_APPROVED", newSsubscriberState.status, message = "Wrong state")


        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - ekyc verify scan information`() {

        val email = "ekyc-${randomInt()}@test.com"
        try {
            createProfile(name = "Test User for eKYC", email = email)

            val scanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            var dataMap = MultivaluedHashMap<String, String>()
            dataMap.put("jumioIdScanReference", listOf(UUID.randomUUID().toString()));
            dataMap.put("idScanStatus", listOf("SUCCESS"))
            dataMap.put("verificationStatus", listOf("APPROVED_VERIFIED"))
            dataMap.put("callbackDate", listOf("2018-12-07T09:19:07.036Z"))
            dataMap.put("idType", listOf("LICENSE"))
            dataMap.put("idCountry", listOf("NOR"))
            dataMap.put("idFirstName", listOf("Test User"))
            dataMap.put("idLastName", listOf("Test Family"))
            dataMap.put("idDob", listOf("1990-12-09"))
            dataMap.put("merchantIdScanReference", listOf(scanInfo.scanId))

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val scanInformation: ScanInformation = get {
                path = "/customer/scanStatus/${scanInfo.scanId}"
                subscriberId = email
            }
            assertEquals("APPROVED", scanInformation.status, message = "Wrong status")

            val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val scanInformationList = get<Collection<ScanInformation>> {
                path = "/profiles/$encodedEmail/scans"
                subscriberId = email
            }
            assertEquals(1, scanInformationList.size, message = "More scans than expected")
            assertEquals("APPROVED", scanInformationList.elementAt(0).status, message = "Wrong status")

        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

    @Test
    fun `jersey test - ekyc verify 2 scans`() {

        val email = "ekyc-${randomInt()}@test.com"
        try {
            createProfile(name = "Test User for eKYC", email = email)

            val scanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            var dataMap = MultivaluedHashMap<String, String>()
            dataMap.put("jumioIdScanReference", listOf(UUID.randomUUID().toString()));
            dataMap.put("idScanStatus", listOf("ERROR"))
            dataMap.put("verificationStatus", listOf("FRAUD"))
            dataMap.put("callbackDate", listOf("2018-12-07T09:19:07.036Z"))
            dataMap.put("idType", listOf("LICENSE"))
            dataMap.put("idCountry", listOf("NOR"))
            dataMap.put("idFirstName", listOf("Test User"))
            dataMap.put("idLastName", listOf("Test Family"))
            dataMap.put("idDob", listOf("1990-12-09"))
            dataMap.put("merchantIdScanReference", listOf(scanInfo.scanId))

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val subscriberState: SubscriberState = get {
                path = "/customer/subscriberState"
                subscriberId = email
            }
            assertEquals("EKYC_REJECTED", subscriberState.status, message = "Wrong state")

            val newScanInfo: ScanInformation = get {
                path = "/customer/new-ekyc-scanId"
                subscriberId = email
            }

            assertNotNull(newScanInfo.scanId, message = "Failed to get new scanId")

            dataMap.clear()
            dataMap.put("jumioIdScanReference", listOf(UUID.randomUUID().toString()));
            dataMap.put("idScanStatus", listOf("SUCCESS"))
            dataMap.put("verificationStatus", listOf("VERIFIED"))
            dataMap.put("callbackDate", listOf("2018-12-07T09:19:07.036Z"))
            dataMap.put("idType", listOf("LICENSE"))
            dataMap.put("idCountry", listOf("NOR"))
            dataMap.put("idFirstName", listOf("Test User"))
            dataMap.put("idLastName", listOf("Test Family"))
            dataMap.put("idDob", listOf("1990-12-09"))
            dataMap.put("merchantIdScanReference", listOf(newScanInfo.scanId))

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val newSsubscriberState: SubscriberState = get {
                path = "/customer/subscriberState"
                subscriberId = email
            }
            assertEquals("EKYC_APPROVED", newSsubscriberState.status, message = "Wrong state")

            val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val scanInformationList = get<Collection<ScanInformation>> {
                path = "/profiles/$encodedEmail/scans"
                subscriberId = email
            }
            assertEquals(2, scanInformationList.size, message = "More scans than expected")
            var verifiedItemIndex = 0
            if (newScanInfo.scanId == scanInformationList.elementAt(1).scanId) {
                verifiedItemIndex = 1
            }
            assertEquals("APPROVED", scanInformationList.elementAt(verifiedItemIndex).status, message = "Wrong status")
        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }

}

class AnalyticsTest {

    @Test
    fun testReportEvent() {

        val email = "analytics-${randomInt()}@test.com"
        createProfile(name = "Test Analytics User", email = email)

        post<String> {
            path = "/analytics"
            body = "event"
            subscriberId = email
        }
    }
}

class ConsentTest {

    private val consentId = "privacy"

    @Test
    fun `jersey test - GET and PUT consent`() {

        val email = "consent-${randomInt()}@test.com"
        createProfile(name = "Test Consent User", email = email)

        val defaultConsent: List<Consent> = get {
            path = "/consents"
            subscriberId = email
        }

        assertEquals(1, defaultConsent.size, "Incorrect number of consents fetched")
        assertEquals(consentId, defaultConsent[0].consentId, "Incorrect 'consent id' in fetched consent")

        val acceptedConsent: Consent = put {
            path = "/consents/$consentId"
            subscriberId = email
        }

        assertEquals(consentId, acceptedConsent.consentId, "Incorrect 'consent id' in response after accepting consent")
        assertTrue(acceptedConsent.isAccepted
                ?: false, "Accepted consent not reflected in response after accepting consent")

        val rejectedConsent: Consent = put {
            path = "/consents/$consentId"
            queryParams = mapOf("accepted" to "false")
            subscriberId = email
        }

        assertEquals(consentId, rejectedConsent.consentId, "Incorrect 'consent id' in response after rejecting consent")
        assertFalse(rejectedConsent.isAccepted
                ?: true, "Accepted consent not reflected in response after rejecting consent")
    }
}

class ReferralTest {

    @Test
    fun `jersey test - POST profile with invalid referred by`() {

        val email = "referred_by_invalid-${randomInt()}@test.com"

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
            post<Profile> {
                path = "/profile"
                body = profile
                subscriberId = email
                queryParams = mapOf("referred_by" to invalid)
            }
        }

        assertEquals("""
{"description":"Incomplete profile description. Subscriber - $invalid not found."} expected:<201> but was:<403>
        """.trimIndent(), failedToCreate.message)

        val failedToGet = assertFails {
            get<Profile> {
                path = "/profile"
                subscriberId = email
            }
        }

        assertEquals("""
{"description":"Incomplete profile description. Subscriber - $email not found."} expected:<200> but was:<404>
        """.trimIndent(), failedToGet.message)
    }

    @Test
    fun `jersey test - POST profile`() {

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

        post<Profile> {
            path = "/profile"
            body = profile
            subscriberId = secondEmail
            queryParams = mapOf("referred_by" to firstEmail)
        }

        // for first
        val referralsForFirst: List<Person> = get {
            path = "/referred"
            subscriberId = firstEmail
        }
        assertEquals(listOf("Test Referral Second User"), referralsForFirst.map { it.name })

        val referredByForFirst: Person = get {
            path = "/referred/by"
            subscriberId = firstEmail
        }
        assertNull(referredByForFirst.name)

        // No need to test SubscriptionStatus for first, since it is already tested in GetSubscriptionStatusTest.

        // for referred_by_foo
        val referralsForSecond: List<Person> = get {
            path = "/referred"
            subscriberId = secondEmail
        }
        assertEquals(emptyList(), referralsForSecond.map { it.name })

        val referredByForSecond: Person = get {
            path = "/referred/by"
            subscriberId = secondEmail
        }
        assertEquals("Test Referral First User", referredByForSecond.name)

        val secondSubscriberBundles: BundleList = get {
            path = "/bundles"
            subscriberId = secondEmail
        }

        assertEquals(1_000_000_000, secondSubscriberBundles[0].balance)

        val secondSubscriberPurchases: PurchaseRecordList = get {
            path = "/purchases"
            subscriberId = secondEmail
        }

        val freeProductForReferred = Product()
                .sku("1GB_FREE_ON_REFERRED")
                .price(Price().apply {
                    this.amount = 0
                    this.currency = "NOK"
                })
                .properties(mapOf("noOfBytes" to "1_000_000_000"))
                .presentation(emptyMap<String, String>())

        assertEquals(listOf(freeProductForReferred), secondSubscriberPurchases.map { it.product })
    }
}

class PlanTest {

    @Test
    fun `jersey test - POST plan`() {

        val price = Price()
                .amount(100)
                .currency("nok")
        val plan = Plan()
                .name("PLAN_1_NOK_PER_DAY")
                .price(price)
                .interval(Plan.IntervalEnum.DAY)
                .intervalCount(1)

        post<Plan> {
            path = "/plans"
            body = plan
        }

        val stored: Plan = get {
            path = "/plans/${plan.name}"
        }

        assertEquals(plan.name, stored.name)
        assertEquals(plan.price, stored.price)
        assertEquals(plan.interval, stored.interval)
        assertEquals(plan.intervalCount, stored.intervalCount)

        val deletedPLan: Plan = delete {
            path = "/plans/${plan.name}"
        }

        assertEquals(plan.name, deletedPLan.name)
        assertEquals(plan.price, deletedPLan.price)
        assertEquals(plan.interval, deletedPLan.interval)
        assertEquals(plan.intervalCount, deletedPLan.intervalCount)

        assertFailsWith(AssertionError::class, "Plan ${plan.name} not removed") {
            get<Plan> {
                path = "/plans/${plan.name}"
            }
        }
    }

    @Test
    fun `jersey test - POST subscription`() {

        val email = "purchase-${randomInt()}@test.com"

        val price = Price()
                .amount(100)
                .currency("nok")
        val plan = Plan()
                .name("test")
                .price(price)
                .interval(Plan.IntervalEnum.DAY)
                .intervalCount(1)

        try {
            // Create subscriber with payment source.

            createProfile(name = "Test Purchase User with Default Payment Source", email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val paymentSource: PaymentSource = post {
                path = "/paymentSources"
                subscriberId = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            // Create a plan.

            post<Plan> {
                path = "/plans"
                body = plan
            }

            val stored: Plan = get {
                path = "/plans/${plan.name}"
            }

            assertEquals(plan.name, stored.name)

            // Now create and verify the subscription.

            post<Unit> {
                path = "/profiles/${email}/plans/${plan.name}"
            }

            val plans: List<Plan> = get {
                path = "/profiles/${email}/plans"
            }

            assert(plans.isNotEmpty())
            assert(plans.lastIndex == 0)
            assertEquals(plan.name, plans[0].name)
            assertEquals(plan.price, plans[0].price)
            assertEquals(plan.interval, plans[0].interval)
            assertEquals(plan.intervalCount, plans[0].intervalCount)

            delete<Unit> {
                path = "/profiles/${email}/plans/${plan.name}"
            }

            // Cleanup - remove plan.
            val deletedPLan: Plan = delete {
                path = "/plans/${plan.name}"
            }
            assertEquals(plan.name, deletedPLan.name)

        } finally {
            StripePayment.deleteCustomer(email = email)
        }
    }
}

class GraphQlTests {

    data class Subscriber(
            val profile: Profile? = null,
            val bundles: Collection<Bundle>? = null,
            val subscriptions: Collection<Subscription>? = null,
            val products: Collection<Product>? = null,
            val purchases: Collection<PurchaseRecord>? = null)

    data class Data(var subscriber: Subscriber? = null)

    data class GraphQlResponse(var data: Data? = null)

    @Test
    fun `jersey test - POST graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        createProfile("Test GraphQL Endpoint", email)

        val msisdn = createSubscription(email)

        val subscriber = post<GraphQlResponse>(expectedResultCode = 200) {
            path = "/graphql"
            subscriberId = email
            body = mapOf("query" to """{ subscriber { profile { email } subscriptions { msisdn } } }""")
        }.data?.subscriber

        assertEquals(expected = email, actual = subscriber?.profile?.email)
        assertEquals(expected = msisdn, actual = subscriber?.subscriptions?.first()?.msisdn)
    }

    @Test
    fun `jersey test - GET graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        createProfile("Test GraphQL Endpoint", email)

        val msisdn = createSubscription(email)

        val subscriber = get<GraphQlResponse> {
            path = "/graphql"
            subscriberId = email
            queryParams = mapOf("query" to URLEncoder.encode("""{subscriber{profile{email}subscriptions{msisdn}}}""", StandardCharsets.UTF_8.name()))
        }.data?.subscriber

        assertEquals(expected = email, actual = subscriber?.profile?.email)
        assertEquals(expected = msisdn, actual = subscriber?.subscriptions?.first()?.msisdn)
    }
}