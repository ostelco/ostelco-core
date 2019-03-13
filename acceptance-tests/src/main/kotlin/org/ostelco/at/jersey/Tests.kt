package org.ostelco.at.jersey

import org.junit.Test
import org.ostelco.at.common.*
import org.ostelco.prime.customer.model.*
import org.ostelco.prime.customer.model.RegionDetails.StatusEnum.PENDING
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap
import kotlin.test.*


class CustomerTest {

    @Test
    fun `jersey test - GET and PUT customer`() {

        val email = "customer-${randomInt()}@test.com"

        val createCustomer = Customer()
                .id("")
                .email(email)
                .name("Test Customer")
                .analyticsId("")
                .referralId("")

        val createdCustomer: Customer = post {
            path = "/customer"
            body = createCustomer
            this.email = email
        }

        assertEquals(createCustomer.email, createdCustomer.email, "Incorrect 'email' in created customer")
        assertEquals(createCustomer.name, createdCustomer.name, "Incorrect 'name' in created customer")

        val customer: Customer = get {
            path = "/customer"
            this.email = email
        }

        assertEquals(createdCustomer.email, customer.email, "Incorrect 'email' in fetched customer")
        assertEquals(createdCustomer.name, customer.name, "Incorrect 'name' in fetched customer")
        assertEquals(createdCustomer.analyticsId, customer.analyticsId, "Incorrect 'analyticsId' in fetched customer")
        assertEquals(createdCustomer.referralId, customer.referralId, "Incorrect 'referralId' in fetched customer")

        val newName = "New name: Test Customer"

        customer.name(newName)

        val updatedCustomer: Customer = put {
            path = "/customer"
            body = customer
            this.email = email
        }

        assertEquals(email, updatedCustomer.email, "Incorrect 'email' in response after updating customer")
        assertEquals(newName, updatedCustomer.name, "Incorrect 'name' in response after updating customer")
    }

    @Test
    fun `jersey test - POST application token`() {

        val email = "token-${randomInt()}@test.com"
        createCustomer("Test Token User", email)

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
            this.email = email
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
        createCustomer(name = "Test Subscriptions User", email = email)
        val msisdn = createSubscription(email)

        val subscriptions: Collection<Subscription> = get {
            path = "/subscriptions"
            this.email = email
        }

        assertEquals(listOf(msisdn), subscriptions.map { it.msisdn })
    }
}

class BundlesAndPurchasesTest {

    private val logger by getLogger()

    @Test
    fun `jersey test - GET bundles`() {

        val email = "balance-${randomInt()}@test.com"
        createCustomer(name = "Test Balance User", email = email)

        val bundles: BundleList = get {
            path = "/bundles"
            this.email = email
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
            this.email = email
        }
        purchaseRecords.sortBy { it.timestamp }

        assertEquals(listOf(freeProduct), purchaseRecords.map { it.product }, "Incorrect first 'Product' in purchase record")
    }
}

class GetProductsTest {

    @Test
    fun `jersey test - GET products`() {

        val email = "products-${randomInt()}@test.com"
        createCustomer(name = "Test Products User", email = email)
        enableRegion(email = email)

        val products: List<Product> = get {
            path = "/products"
            this.email = email
        }

        assertEquals(expectedProducts().toSet(), products.toSet(), "Incorrect 'Products' fetched")
    }
}

class SourceTest {

    @Test
    fun `jersey test - POST source create`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test create Payment Source", email = email).id

            val tokenId = StripePayment.createPaymentTokenId()

            // Ties source with user profile both local and with Stripe
            post<PaymentSource> {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to tokenId)
            }

            Thread.sleep(200)

            val sources: PaymentSourceList = get {
                path = "/paymentSources"
                this.email = email
            }
            assert(sources.isNotEmpty()) { "Expected at least one payment source for profile $email" }

            val cardId = StripePayment.getCardIdForTokenId(tokenId)
            assertNotNull(sources.first { it.id == cardId }, "Expected card $cardId in list of payment sources for profile $email")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - GET list sources`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test list Payment Source", email = email).id

            Thread.sleep(200)

            val createdIds = listOf(createTokenWithStripe(email),
                    createSourceWithStripe(email),
                    createTokenWithStripe(email),
                    createSourceWithStripe(email))

            val sources: PaymentSourceList = get {
                path = "/paymentSources"
                this.email = email
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
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - GET list sources no profile `() {

        val email = "purchase-${randomInt()}@test.com"

        var customerId = ""
        try {
            customerId = createCustomer(name = "Test List Payment Sources", email = email).id

            val sources: PaymentSourceList = get {
                path = "/paymentSources"
                this.email = email
            }

            assert(sources.isEmpty()) { "Expected no payment source for profile $email" }

            assertNotNull(StripePayment.getStripeCustomerId(customerId = customerId)) { "Customer Id should have been created" }

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - PUT source set default`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test update Payment Source", email = email).id

            val tokenId = StripePayment.createPaymentTokenId()
            val cardId = StripePayment.getCardIdForTokenId(tokenId)

            // Ties source with user profile both local and with Stripe
            post<PaymentSource> {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to tokenId)
            }

            Thread.sleep(200)

            val newTokenId = StripePayment.createPaymentTokenId()
            val newCardId = StripePayment.getCardIdForTokenId(newTokenId)

            post<PaymentSource> {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to newTokenId)
            }

            // TODO: Update to fetch the Stripe customerId from 'admin' API when ready.
            val stripeCustomerId = StripePayment.getStripeCustomerId(customerId = customerId)

            // Verify that original 'sourceId/card' is default.
            assertEquals(cardId, StripePayment.getDefaultSourceForCustomer(stripeCustomerId),
                    "Expected $cardId to be default source for $stripeCustomerId")

            // Set new default card.
            put<PaymentSource> {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to newCardId)
            }

            assertEquals(newCardId, StripePayment.getDefaultSourceForCustomer(stripeCustomerId),
                    "Expected $newCardId to be default source for $stripeCustomerId")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - DELETE source`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test delete Payment Source", email = email).id

            Thread.sleep(200)

            val createdIds = listOf(getCardIdForTokenFromStripe(createTokenWithStripe(email)),
                    createSourceWithStripe(email))

            val deletedIds = createdIds.map { removeSourceWithStripe(email, it) }

            assert(createdIds.containsAll(deletedIds.toSet())) {
                "Failed to delete one or more sources: ${createdIds.toSet() - deletedIds.toSet()}"
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
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
            this.email = email
            queryParams = mapOf("sourceId" to tokenId)
        }

        return tokenId
    }

    private fun createSourceWithStripe(email: String): String {
        val sourceId = StripePayment.createPaymentSourceId()

        post<PaymentSource> {
            path = "/paymentSources"
            this.email = email
            queryParams = mapOf("sourceId" to sourceId)
        }

        return sourceId
    }

    private fun removeSourceWithStripe(email: String, sourceId: String): String {
        val removedSource = delete<PaymentSource> {
            path = "/paymentSources"
            this.email = email
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
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Purchase User", email = email).id
            enableRegion(email = email)

            val balanceBefore = get<List<Bundle>> {
                path = "/bundles"
                this.email = email
            }.first().balance

            val productSku = "1GB_249NOK"
            val sourceId = StripePayment.createPaymentTokenId()

            post<String> {
                path = "/products/$productSku/purchase"
                this.email = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            Thread.sleep(100) // wait for 100 ms for balance to be updated in db

            val balanceAfter = get<List<Bundle>> {
                path = "/bundles"
                this.email = email
            }.first().balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - POST products purchase using default source`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Purchase with Default Payment Source", email = email).id
            enableRegion(email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val paymentSource: PaymentSource = post {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            val balanceBefore = get<List<Bundle>> {
                path = "/bundles"
                this.email = email
            }.first().balance

            val productSku = "1GB_249NOK"

            post<String> {
                path = "/products/$productSku/purchase"
                this.email = email
            }

            Thread.sleep(100) // wait for 100 ms for balance to be updated in db

            val balanceAfter = get<List<Bundle>> {
                path = "/bundles"
                this.email = email
            }.first().balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - Refund purchase using default source`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test refund Purchase User with Default Payment Source", email = email).id
            enableRegion(email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val paymentSource: PaymentSource = post {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            val balanceBefore = get<List<Bundle>> {
                path = "/bundles"
                this.email = email
            }.first().balance

            val productSku = "1GB_249NOK"

            post<String> {
                path = "/products/$productSku/purchase"
                this.email = email
            }

            Thread.sleep(100) // wait for 100 ms for balance to be updated in db

            val balanceAfter = get<List<Bundle>> {
                path = "/bundles"
                this.email = email
            }.first().balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")

            val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val refundedProduct = put<ProductInfo> {
                path = "/refund/$encodedEmail"
                this.email = email
                queryParams = mapOf(
                        "purchaseRecordId" to purchaseRecords.last().id,
                        "reason" to "requested_by_customer")
            }
            logger.info("Refunded product: $refundedProduct with purchase id:${purchaseRecords.last().id}")
            assertEquals(productSku, refundedProduct.id, "Refund returned a different product")

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - POST products purchase add source then pay with it`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Purchase with adding Payment Source", email = email).id
            enableRegion(email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val paymentSource: PaymentSource = post {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            val bundlesBefore: BundleList = get {
                path = "/bundles"
                this.email = email
            }
            val balanceBefore = bundlesBefore[0].balance

            val productSku = "1GB_249NOK"

            post<String> {
                path = "/products/$productSku/purchase"
                this.email = email
                queryParams = mapOf("sourceId" to paymentSource.id)
            }

            Thread.sleep(100) // wait for 100 ms for balance to be updated in db

            val bundlesAfter: BundleList = get {
                path = "/bundles"
                this.email = email
            }
            val balanceAfter = bundlesAfter[0].balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

}

class eKYCTest {

    private val imgUrl = "https://www.gstatic.com/webp/gallery3/1.png"
    private val imgUrl2 = "https://www.gstatic.com/webp/gallery3/2.png"

    @Test
    fun `jersey test - GET new-ekyc-scanId - generate new scanId for eKYC`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "regions/no/kyc/jumio/scans"
                this.email = email
            }
            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val regionDetails = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(PENDING, regionDetails.status, message = "Wrong State")

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc callback - test the call back processing`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("ERROR")
            dataMap["verificationStatus"] = listOf("DENIED_FRAUD")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            dataMap["merchantIdScanReference"] = listOf(scanInfo.scanId)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val regionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(RegionDetails.StatusEnum.REJECTED, regionDetails.status, message = "Wrong State")

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc callback - process success`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("SUCCESS")
            dataMap["verificationStatus"] = listOf("APPROVED_VERIFIED")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            dataMap["merchantIdScanReference"] = listOf(scanInfo.scanId)
            val identityVerification = """{ "similarity":"MATCH", "validity":"TRUE"}"""
            dataMap["identityVerification"] = listOf(identityVerification)
            dataMap["livenessImages"] = listOf(imgUrl, imgUrl2)
            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val regionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(RegionDetails.StatusEnum.APPROVED, regionDetails.status, message = "Wrong State")

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc callback - process failure of face id`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("SUCCESS")
            dataMap["verificationStatus"] = listOf("APPROVED_VERIFIED")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            dataMap["merchantIdScanReference"] = listOf(scanInfo.scanId)
            val identityVerification = """{ "similarity":"MATCH", "validity":"FALSE", "reason": "ENTIRE_ID_USED_AS_SELFIE" }"""
            dataMap["identityVerification"] = listOf(identityVerification)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val regionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(RegionDetails.StatusEnum.REJECTED, regionDetails.status, message = "Wrong State")

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc callback - process incomplete form data`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("SUCCESS")
            dataMap["verificationStatus"] = listOf("APPROVED_VERIFIED")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            //dataMap.put("merchantIdScanReference", listOf(scanInfo.scanId))

            post<String>(expectedResultCode = 400, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val regionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(RegionDetails.StatusEnum.PENDING, regionDetails.status, message = "Wrong State")

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc callback - incomplete face id verification data`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("SUCCESS")
            dataMap["verificationStatus"] = listOf("APPROVED_VERIFIED")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            dataMap["merchantIdScanReference"] = listOf(scanInfo.scanId)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val regionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(RegionDetails.StatusEnum.REJECTED, regionDetails.status, message = "Wrong State")

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc callback - reject & approve`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("ERROR")
            dataMap["verificationStatus"] = listOf("DENIED_FRAUD")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            dataMap["merchantIdScanReference"] = listOf(scanInfo.scanId)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val regionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(RegionDetails.StatusEnum.REJECTED, regionDetails.status, message = "Wrong State")

            val newScanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(newScanInfo.scanId, message = "Failed to get new scanId")


            val dataMap2 = MultivaluedHashMap<String, String>()
            dataMap2["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap2["idScanStatus"] = listOf("SUCCESS")
            dataMap2["verificationStatus"] = listOf("APPROVED_VERIFIED")
            dataMap2["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap2["idType"] = listOf("LICENSE")
            dataMap2["idCountry"] = listOf("NOR")
            dataMap2["idFirstName"] = listOf("Test User")
            dataMap2["idLastName"] = listOf("Test Family")
            dataMap2["idDob"] = listOf("1990-12-09")
            dataMap2["merchantIdScanReference"] = listOf(newScanInfo.scanId)
            dataMap2["idScanImage"] = listOf(imgUrl)
            dataMap2["idScanImageBackside"] = listOf(imgUrl2)
            dataMap2["livenessImages"] = listOf(imgUrl, imgUrl2)
            val identityVerification = """{ "similarity":"MATCH", "validity":"TRUE"}"""
            dataMap2["identityVerification"] = listOf(identityVerification)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap2
            }

            val newRegionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), newRegionDetails.region)
            assertEquals(RegionDetails.StatusEnum.APPROVED, newRegionDetails.status, message = "Wrong State")


        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc verify scan information`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("SUCCESS")
            dataMap["verificationStatus"] = listOf("APPROVED_VERIFIED")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            dataMap["merchantIdScanReference"] = listOf(scanInfo.scanId)
            dataMap["idScanImage"] = listOf(imgUrl)
            dataMap["idScanImageBackside"] = listOf(imgUrl2)
            dataMap["livenessImages"] = listOf(imgUrl, imgUrl2)
            val identityVerification = """{ "similarity":"MATCH", "validity":"TRUE"}"""
            dataMap["identityVerification"] = listOf(identityVerification)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val scanInformation: ScanInformation = get {
                path = "/regions/no/kyc/jumio/scans/${scanInfo.scanId}/status"
                this.email = email
            }
            assertEquals("APPROVED", scanInformation.status, message = "Wrong status")

            val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val scanInformationList = get<Collection<ScanInformation>> {
                path = "/profiles/$encodedEmail/scans"
                this.email = email
            }
            assertEquals(1, scanInformationList.size, message = "More scans than expected")
            assertEquals("APPROVED", scanInformationList.elementAt(0).status, message = "Wrong status")

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc verify 2 scans`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("ERROR")
            dataMap["verificationStatus"] = listOf("DENIED_FRAUD")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            dataMap["merchantIdScanReference"] = listOf(scanInfo.scanId)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val newRegionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), newRegionDetails.region)
            assertEquals(RegionDetails.StatusEnum.REJECTED, newRegionDetails.status, message = "Wrong State")

            val newScanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(newScanInfo.scanId, message = "Failed to get new scanId")

            val dataMap2 = MultivaluedHashMap<String, String>()
            dataMap2["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap2["idScanStatus"] = listOf("SUCCESS")
            dataMap2["verificationStatus"] = listOf("APPROVED_VERIFIED")
            dataMap2["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap2["idType"] = listOf("LICENSE")
            dataMap2["idCountry"] = listOf("NOR")
            dataMap2["idFirstName"] = listOf("Test User")
            dataMap2["idLastName"] = listOf("Test Family")
            dataMap2["idDob"] = listOf("1990-12-09")
            dataMap2["merchantIdScanReference"] = listOf(newScanInfo.scanId)
            dataMap2["idScanImage"] = listOf(imgUrl)
            dataMap2["idScanImageBackside"] = listOf(imgUrl2)
            // JUMIO POST data for livenesss images are interpreted like this by HTTP client in prime.
            val stringList = "[ \"$imgUrl\", \"$imgUrl2\" ]"
            dataMap2["livenessImages"] = listOf(stringList)
            val identityVerification = """{ "similarity":"MATCH", "validity":"TRUE"}"""
            dataMap2["identityVerification"] = listOf(identityVerification)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap2
            }

            val regionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(RegionDetails.StatusEnum.APPROVED, regionDetails.status, message = "Wrong State")

            val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val scanInformationList = get<Collection<ScanInformation>> {
                path = "/profiles/$encodedEmail/scans"
                this.email = email
            }
            assertEquals(2, scanInformationList.size, message = "More scans than expected")
            var verifiedItemIndex = 0
            if (newScanInfo.scanId == scanInformationList.elementAt(1).scanId) {
                verifiedItemIndex = 1
            }
            assertEquals("APPROVED", scanInformationList.elementAt(verifiedItemIndex).status, message = "Wrong status")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - ekyc rejected with detailed reject reason`() {

        val email = "ekyc-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test User for eKYC", email = email).id

            val scanInfo: ScanInformation = post {
                path = "/regions/no/kyc/jumio/scans"
                this.email = email
            }

            assertNotNull(scanInfo.scanId, message = "Failed to get new scanId")
            val rejectReason = """{ "rejectReasonCode":"100", "rejectReasonDescription":"MANIPULATED_DOCUMENT", "rejectReasonDetails": [{ "detailsCode": "1001", "detailsDescription": "PHOTO" },{ "detailsCode": "1004", "detailsDescription": "DOB" }]}"""

            val dataMap = MultivaluedHashMap<String, String>()
            dataMap["jumioIdScanReference"] = listOf(UUID.randomUUID().toString())
            dataMap["idScanStatus"] = listOf("ERROR")
            dataMap["verificationStatus"] = listOf("DENIED_FRAUD")
            dataMap["callbackDate"] = listOf("2018-12-07T09:19:07.036Z")
            dataMap["idType"] = listOf("LICENSE")
            dataMap["idCountry"] = listOf("NOR")
            dataMap["idFirstName"] = listOf("Test User")
            dataMap["idLastName"] = listOf("Test Family")
            dataMap["idDob"] = listOf("1990-12-09")
            dataMap["merchantIdScanReference"] = listOf(scanInfo.scanId)
            dataMap["rejectReason"] = listOf(rejectReason)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            val regionDetails = get<Collection<RegionDetails>> {
                path = "/regions"
                this.email = email
            }.single()

            assertEquals(Region().id("no").name("Norway"), regionDetails.region)
            assertEquals(RegionDetails.StatusEnum.REJECTED, regionDetails.status, message = "Wrong State")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

}

class AnalyticsTest {

    @Test
    fun testReportEvent() {

        val email = "analytics-${randomInt()}@test.com"
        createCustomer(name = "Test Analytics User", email = email)

        post<String> {
            path = "/analytics"
            body = "event"
            this.email = email
        }
    }
}

class ReferralTest {

    @Test
    fun `jersey test - POST customer with invalid referred by`() {

        val email = "referred_by_invalid-${randomInt()}@test.com"

        val invalid = "invalid_referrer@test.com"

        val customer = Customer()
                .email(email)
                .name("Test Referral Second User")
                .referralId("")

        val failedToCreate = assertFails {
            post<Customer> {
                path = "/customer"
                body = customer
                this.email = email
                queryParams = mapOf("referred_by" to invalid)
            }
        }

        assertEquals("""
{"description":"Incomplete customer description. Subscriber - $invalid not found."} expected:<201> but was:<403>
        """.trimIndent(), failedToCreate.message)

        val failedToGet = assertFails {
            get<Customer> {
                path = "/customer"
                this.email = email
            }
        }

        assertEquals("""
{"description":"Incomplete customer description. Subscriber - $email not found."} expected:<200> but was:<404>
        """.trimIndent(), failedToGet.message)
    }

    @Test
    fun `jersey test - POST customer`() {

        val firstEmail = "referral_first-${randomInt()}@test.com"
        createCustomer(name = "Test Referral First User", email = firstEmail)

        val secondEmail = "referral_second-${randomInt()}@test.com"

        val customer = Customer()
                .email(secondEmail)
                .name("Test Referral Second User")
                .referralId("")

        post<Customer> {
            path = "/customer"
            body = customer
            email = secondEmail
            queryParams = mapOf("referred_by" to firstEmail)
        }

        // for first
        val referralsForFirst: List<Person> = get {
            path = "/referred"
            email = firstEmail
        }
        assertEquals(listOf("Test Referral Second User"), referralsForFirst.map { it.name })

        val referredByForFirst: Person = get {
            path = "/referred/by"
            email = firstEmail
        }
        assertNull(referredByForFirst.name)

        // No need to test SubscriptionStatus for first, since it is already tested in GetSubscriptionStatusTest.

        // for referred_by_foo
        val referralsForSecond: List<Person> = get {
            path = "/referred"
            email = secondEmail
        }
        assertEquals(emptyList(), referralsForSecond.map { it.name })

        val referredByForSecond: Person = get {
            path = "/referred/by"
            email = secondEmail
        }
        assertEquals("Test Referral First User", referredByForSecond.name)

        val secondSubscriberBundles: BundleList = get {
            path = "/bundles"
            email = secondEmail
        }

        assertEquals(1_000_000_000, secondSubscriberBundles[0].balance)

        val secondSubscriberPurchases: PurchaseRecordList = get {
            path = "/purchases"
            email = secondEmail
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
                .name("PLAN_1_NOK_PER_DAY-${randomInt()}")
                .price(price)
                .interval(Plan.IntervalEnum.DAY)
                .intervalCount(1)
                .properties(emptyMap<String, Any>())
                .presentation(emptyMap<String, Any>())

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
    fun `jersey test - POST profiles plans`() {

        val email = "purchase-${randomInt()}@test.com"

        val price = Price()
                .amount(100)
                .currency("nok")
        val plan = Plan()
                .name("plan-${randomInt()}")
                .price(price)
                .interval(Plan.IntervalEnum.DAY)
                .intervalCount(1)
                .properties(emptyMap<String, Any>())
                .presentation(emptyMap<String, Any>())

        var customerId = ""

        try {
            // Create subscriber with payment source.

            customerId = createCustomer(name = "Test create Profile Plans", email = email).id

            val sourceId = StripePayment.createPaymentTokenId()

            val paymentSource: PaymentSource = post {
                path = "/paymentSources"
                this.email = email
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
                path = "/profiles/$email/plans/${plan.name}"
            }

            val plans: List<Plan> = get {
                path = "/profiles/$email/plans"
            }

            assert(plans.isNotEmpty())
            assert(plans.lastIndex == 0)
            assertEquals(plan.name, plans[0].name)
            assertEquals(plan.price, plans[0].price)
            assertEquals(plan.interval, plans[0].interval)
            assertEquals(plan.intervalCount, plans[0].intervalCount)

            delete<Unit> {
                path = "/profiles/$email/plans/${plan.name}"
            }

            // Cleanup - remove plan.
            val deletedPLan: Plan = delete {
                path = "/plans/${plan.name}"
            }
            assertEquals(plan.name, deletedPLan.name)

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class GraphQlTests {

    data class Context(
            val customer: Customer? = null,
            val bundles: Collection<Bundle>? = null,
            val subscriptions: Collection<Subscription>? = null,
            val products: Collection<Product>? = null,
            val purchases: Collection<PurchaseRecord>? = null)

    data class Data(var context: Context? = null)

    data class GraphQlResponse(var data: Data? = null, var errors: List<String>? = null)

    @Test
    fun `jersey test - POST graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        createCustomer("Test GraphQL Endpoint", email)

        val msisdn = createSubscription(email)

        val context = post<GraphQlResponse>(expectedResultCode = 200) {
            path = "/graphql"
            this.email = email
            body = mapOf("query" to """{ context { customer { email } subscriptions { msisdn } } }""")
        }.data?.context

        assertEquals(expected = email, actual = context?.customer?.email)
        assertEquals(expected = msisdn, actual = context?.subscriptions?.first()?.msisdn)
    }

    @Test
    fun `jersey test - GET graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        createCustomer("Test GraphQL Endpoint", email)

        val msisdn = createSubscription(email)

        val context = get<GraphQlResponse> {
            path = "/graphql"
            this.email = email
            queryParams = mapOf("query" to URLEncoder.encode("""{context{customer{email}subscriptions{msisdn}}}""", StandardCharsets.UTF_8.name()))
        }.data?.context

        assertEquals(expected = email, actual = context?.customer?.email)
        assertEquals(expected = msisdn, actual = context?.subscriptions?.first()?.msisdn)
    }
}