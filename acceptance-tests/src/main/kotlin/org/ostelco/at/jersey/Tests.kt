package org.ostelco.at.jersey

import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.ostelco.at.common.StripeEventListener
import org.ostelco.at.common.StripePayment
import org.ostelco.at.common.createCustomer
import org.ostelco.at.common.createSubscription
import org.ostelco.at.common.enableRegion
import org.ostelco.at.common.expectedPlanProductSG
import org.ostelco.at.common.expectedPlanProductUS
import org.ostelco.at.common.expectedProducts
import org.ostelco.at.common.getLogger
import org.ostelco.at.common.graphqlGetQuery
import org.ostelco.at.common.graphqlPostQuery
import org.ostelco.at.common.randomInt
import org.ostelco.prime.customer.model.ApplicationToken
import org.ostelco.prime.customer.model.Bundle
import org.ostelco.prime.customer.model.BundleList
import org.ostelco.prime.customer.model.Customer
import org.ostelco.prime.customer.model.KycStatus
import org.ostelco.prime.customer.model.KycType
import org.ostelco.prime.customer.model.MyInfoConfig
import org.ostelco.prime.customer.model.PaymentSource
import org.ostelco.prime.customer.model.PaymentSourceList
import org.ostelco.prime.customer.model.Person
import org.ostelco.prime.customer.model.Price
import org.ostelco.prime.customer.model.Product
import org.ostelco.prime.customer.model.ProductInfo
import org.ostelco.prime.customer.model.PurchaseRecord
import org.ostelco.prime.customer.model.PurchaseRecordList
import org.ostelco.prime.customer.model.Region
import org.ostelco.prime.customer.model.RegionDetails
import org.ostelco.prime.customer.model.RegionDetails.StatusEnum.APPROVED
import org.ostelco.prime.customer.model.RegionDetails.StatusEnum.PENDING
import org.ostelco.prime.customer.model.RegionDetails.StatusEnum.AVAILABLE
import org.ostelco.prime.customer.model.RegionDetailsList
import org.ostelco.prime.customer.model.ScanInformation
import org.ostelco.prime.customer.model.SimProfile
import org.ostelco.prime.customer.model.SimProfileList
import org.ostelco.prime.customer.model.Subscription
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


class CustomerTest {

    @Test
    fun `jersey test - encoded email GET and PUT customer`() {

        val email = "customer-${randomInt()}+test@test.com"
        val nickname = "Test Customer"
        var customerId = ""
        try {
            val createdCustomer: Customer = post {
                path = "/customer"
                queryParams = mapOf(
                        "contactEmail" to URLEncoder.encode(email, StandardCharsets.UTF_8),
                        "nickname" to nickname)
                this.email = email
            }

            customerId = createdCustomer.id

            assertEquals(email, createdCustomer.contactEmail, "Incorrect 'contactEmail' in created customer")
            assertEquals(nickname, createdCustomer.nickname, "Incorrect 'nickname' in created customer")

            val customer: Customer = get {
                path = "/customer"
                this.email = email
            }

            assertEquals(createdCustomer.contactEmail, customer.contactEmail, "Incorrect 'contactEmail' in fetched customer")
            assertEquals(createdCustomer.nickname, customer.nickname, "Incorrect 'nickname' in fetched customer")
            assertEquals(createdCustomer.analyticsId, customer.analyticsId, "Incorrect 'analyticsId' in fetched customer")
            assertEquals(createdCustomer.referralId, customer.referralId, "Incorrect 'referralId' in fetched customer")

            val newName = "New name: Test Customer"
            val email2 = "customer-${randomInt()}.abc+test@test.com"

            val updatedCustomer: Customer = put {
                path = "/customer"
                queryParams = mapOf(
                        "contactEmail" to URLEncoder.encode(email2, StandardCharsets.UTF_8),
                        "nickname" to newName)
                this.email = email
            }

            assertEquals(email2, updatedCustomer.contactEmail, "Incorrect 'email' in response after updating customer")
            assertEquals(newName, updatedCustomer.nickname, "Incorrect 'name' in response after updating customer")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - GET and PUT customer`() {

        val email = "customer-${randomInt()}+test@test.com"
        val nickname = "Test Customer"
        var customerId = ""
        try {
            val createdCustomer: Customer = post {
                path = "/customer"
                queryParams = mapOf(
                        "contactEmail" to email,
                        "nickname" to nickname)
                this.email = email
            }

            customerId = createdCustomer.id

            assertEquals(email, createdCustomer.contactEmail, "Incorrect 'contactEmail' in created customer")
            assertEquals(nickname, createdCustomer.nickname, "Incorrect 'nickname' in created customer")

            val customer: Customer = get {
                path = "/customer"
                this.email = email
            }

            assertEquals(createdCustomer.contactEmail, customer.contactEmail, "Incorrect 'contactEmail' in fetched customer")
            assertEquals(createdCustomer.nickname, customer.nickname, "Incorrect 'nickname' in fetched customer")
            assertEquals(createdCustomer.analyticsId, customer.analyticsId, "Incorrect 'analyticsId' in fetched customer")
            assertEquals(createdCustomer.referralId, customer.referralId, "Incorrect 'referralId' in fetched customer")

            val newName = "New name: Test Customer"

            val updatedCustomer: Customer = put {
                path = "/customer"
                queryParams = mapOf("nickname" to newName)
                this.email = email
            }

            assertEquals(email, updatedCustomer.contactEmail, "Incorrect 'email' in response after updating customer")
            assertEquals(newName, updatedCustomer.nickname, "Incorrect 'name' in response after updating customer")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - POST application token`() {

        val email = "token-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer("Test Token User", email).id

            createSubscription(email)

            val token = UUID.randomUUID().toString()
            val applicationId = "testApplicationId"
            val tokenType = "FCM"

            val testToken = ApplicationToken()
                    .token(token)
                    .applicationID(applicationId)
                    .tokenType(tokenType)

            val reply: ApplicationToken = post {
                path = "/applicationToken"
                body = testToken
                this.email = email
            }

            assertEquals(token, reply.token, "Incorrect token in reply after posting new token")
            assertEquals(applicationId, reply.applicationID, "Incorrect applicationId in reply after posting new token")
            assertEquals(tokenType, reply.tokenType, "Incorrect tokenType in reply after posting new token")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class RegionsTest {

    @Test
    fun `jersey test - GET regions - No regions`() {

        val email = "regions-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test No Region User", email = email).id

            val regionDetailsList: Collection<RegionDetails> = get {
                path = "/regions"
                this.email = email
            }
            regionDetailsList.forEach {
                assertTrue(it.status == AVAILABLE, "All regions should be in available state")
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - GET regions - Single Region with no profiles`() {

        val email = "regions-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Single Region User", email = email).id
            enableRegion(email = email)

            val regionDetailsList: RegionDetailsList = get {
                path = "/regions"
                this.email = email
            }

            val naRegionDetails = regionDetailsList.singleOrNull { it.status != AVAILABLE }
            assertTrue(naRegionDetails != null, "List should contain only one region in a state other than available")

            val noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "List should contain contain 'no' region")

            assertEquals(naRegionDetails, noRegionDetails, "RegionDetails do not match")


            val regionDetails = RegionDetails()
                    .region(Region().id("no").name("Norway"))
                    .status(APPROVED)
                    .kycStatusMap(mapOf(KycType.JUMIO.name to KycStatus.APPROVED))
                    .simProfiles(SimProfileList())

            assertEquals(regionDetails, noRegionDetails, "RegionDetails do not match")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Ignore
    @Test
    fun `jersey test - GET regions - Single Region with one profile`() {

        val email = "regions-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Single Region User", email = email).id
            enableRegion(email = email)

            post<SimProfile> {
                path = "/regions/no/simProfiles"
                this.email = email
            }

            val regionDetailsList: Collection<RegionDetails> = get {
                path = "/regions"
                this.email = email
            }

            assertEquals(1, regionDetailsList.size, "Customer should have one region")

            val receivedRegion = regionDetailsList.first()

            assertEquals(Region().id("no").name("Norway"), receivedRegion.region, "Region do not match")

            assertEquals(APPROVED, receivedRegion.status, "Region status do not match")

            assertEquals(
                    mapOf(KycType.JUMIO.name to KycStatus.APPROVED),
                    receivedRegion.kycStatusMap,
                    "Kyc status map do not match")

            assertEquals(
                    1,
                    receivedRegion.simProfiles.size,
                    "Should have only one sim profile")

            assertNotNull(receivedRegion.simProfiles.single().iccId)
            assertEquals("", receivedRegion.simProfiles.single().alias)
            assertNotNull(receivedRegion.simProfiles.single().eSimActivationCode)
            assertEquals(SimProfile.StatusEnum.AVAILABLE_FOR_DOWNLOAD, receivedRegion.simProfiles.single().status)
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class SubscriptionsTest {

    @Test
    fun `jersey test - GET subscriptions`() {

        val email = "subs-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Subscriptions User", email = email).id
            enableRegion(email = email)
            val msisdn = createSubscription(email)

            val subscriptions: Collection<Subscription> = get {
                path = "regions/no/subscriptions"
                this.email = email
            }

            assertEquals(listOf(msisdn), subscriptions.map { it.msisdn })
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class BundlesAndPurchasesTest {

    private val logger by getLogger()

    @Test
    fun `jersey test - GET bundles`() {

        val email = "balance-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Balance User", email = email).id

            val bundles: BundleList = get {
                path = "/bundles"
                this.email = email
            }

            logger.info("Balance: ${bundles[0].balance}")

            val freeProduct = Product()
                    .sku("2GB_FREE_ON_JOINING")
                    .price(Price().amount(0).currency(""))
                    .payment(emptyMap<String, String>())
                    .properties(mapOf(
                            "noOfBytes" to "2_147_483_648",
                            "productClass" to "SIMPLE_DATA"))
                    .presentation(emptyMap<String, String>())

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }
            purchaseRecords.sortBy { it.timestamp }

            assertEquals(listOf(freeProduct), purchaseRecords.map { it.product }, "Incorrect first 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class GetProductsTest {

    @Test
    fun `jersey test - GET products`() {

        val email = "products-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Products User", email = email).id
            enableRegion(email = email)

            val products: List<Product> = get {
                path = "/products"
                this.email = email
            }

            assertEquals(expectedProducts().toSet(), products.toSet(), "Incorrect 'Products' fetched")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
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

            assertEquals(1_073_741_824, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

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

            assertEquals(1_073_741_824, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

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

            assertEquals(1_073_741_824, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")

            val refundedProduct = put<ProductInfo> {
                path = "/support/refund/$customerId"
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

            assertEquals(1_073_741_824, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

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

class JumioKycTest {

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

            val regionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            val noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(PENDING, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(
                            KycType.JUMIO.name to KycStatus.PENDING),
                    actual = noRegionDetails.kycStatusMap)

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

            val regionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }


            val noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(PENDING, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(
                            KycType.JUMIO.name to KycStatus.REJECTED),
                    actual = noRegionDetails.kycStatusMap)

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

            val regionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            val noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(APPROVED, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(
                            KycType.JUMIO.name to KycStatus.APPROVED),
                    actual = noRegionDetails.kycStatusMap)

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

            val regionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            val noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(PENDING, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.REJECTED),
                    actual = noRegionDetails.kycStatusMap)

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

            val regionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            val noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(PENDING, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.PENDING),
                    actual = noRegionDetails.kycStatusMap)

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

            val regionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            val noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(PENDING, noRegionDetails.status, message = "Wrong State")
            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.REJECTED),
                    actual = noRegionDetails.kycStatusMap)

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

            val regionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            var noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(PENDING, noRegionDetails.status, message = "Wrong State")
            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.REJECTED),
                    actual = noRegionDetails.kycStatusMap)

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

            val newRegionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            noRegionDetails = newRegionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(APPROVED, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.APPROVED),
                    actual = noRegionDetails.kycStatusMap)

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
                path = "/regions/no/kyc/jumio/scans/${scanInfo.scanId}"
                this.email = email
            }
            assertEquals("APPROVED", scanInformation.status, message = "Wrong status")

            val scanInformationList = get<Collection<ScanInformation>> {
                path = "/support/profiles/$customerId/scans"
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

            val regionDetailsList1 = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            var noRegionDetails = regionDetailsList1.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(PENDING, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.REJECTED),
                    actual = noRegionDetails.kycStatusMap)

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

            val regionDetailsList2 = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }

            noRegionDetails = regionDetailsList2.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")

            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(APPROVED, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.APPROVED),
                    actual = noRegionDetails.kycStatusMap)

            val scanInformationList = get<Collection<ScanInformation>> {
                path = "/support/profiles/$customerId/scans"
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

            val regionDetailsList = get<RegionDetailsList> {
                path = "/regions"
                this.email = email
            }
            val noRegionDetails = regionDetailsList.singleOrNull { it.region.id == "no" }
            assertTrue(noRegionDetails != null, "Did not find Norway region")


            assertEquals(Region().id("no").name("Norway"), noRegionDetails.region)
            assertEquals(PENDING, noRegionDetails.status, message = "Wrong State")

            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.REJECTED),
                    actual = noRegionDetails.kycStatusMap)

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class SingaporeKycTest {

    @Test
    fun `jersey test - GET myinfoConfig v3`() {

        val email = "myinfo-v3-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test MyInfoConfig v3 Customer", email = email).id

            val myInfoConfig = get<MyInfoConfig> {
                path = "/regions/sg/kyc/myInfo/v3/config"
                this.email = email
            }

            assertEquals(
                    "http://ext-myinfo-emulator:8080/v3/authorise" +
                            "?client_id=STG2-MYINFO-SELF-TEST" +
                            "&attributes=name,dob,mailadd,regadd,passexpirydate,uinfin" +
                            "&redirect_uri=http://localhost:3001/callback",
                    myInfoConfig.url)

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - GET myinfo v3`() {

        val email = "myinfo-v3-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test MyInfo v3 Customer", email = email).id

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }

                regionDetailsList.forEach {
                    assertTrue(it.status == AVAILABLE, "All regions should be in available state")
                }
            }

            val personData: String = get {
                path = "/regions/sg/kyc/myInfo/v3/personData/authCode"
                this.email = email
            }

            val expectedPersonData = """{"name":{"lastupdated":"2019-04-05","source":"1","classification":"C","value":"TAN XIAO HUI"},"sex":{"lastupdated":"2019-04-05","code":"F","source":"1","classification":"C","desc":"FEMALE"},"nationality":{"lastupdated":"2019-04-05","code":"SG","source":"1","classification":"C","desc":"SINGAPORE CITIZEN"},"dob":{"lastupdated":"2019-04-05","source":"1","classification":"C","value":"1998-06-06"},"email":{"lastupdated":"2019-04-05","source":"2","classification":"C","value":"myinfotesting@gmail.com"},"mobileno":{"lastupdated":"2019-04-05","source":"2","classification":"C","areacode":{"value":"65"},"prefix":{"value":"+"},"nbr":{"value":"97399245"}},"mailadd":{"country":{"code":"SG","desc":"SINGAPORE"},"unit":{"value":"128"},"street":{"value":"BEDOK NORTH AVENUE 4"},"lastupdated":"2019-04-05","block":{"value":"102"},"source":"1","postal":{"value":"460102"},"classification":"C","floor":{"value":"09"},"type":"SG","building":{"value":"PEARL GARDEN"}}}"""
            assertEquals(expectedPersonData, personData, "MyInfo PersonData do not match")

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }

                val newRegionDetailsList = regionDetailsList.singleOrNull { it.region.id == "sg" }
                assertTrue(newRegionDetailsList != null, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.JUMIO.name to KycStatus.PENDING,
                                KycType.MY_INFO.name to KycStatus.APPROVED,
                                KycType.ADDRESS.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, newRegionDetailsList, "RegionDetails do not match")
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - NRIC, Jumio and address`() {

        val email = "myinfo-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test MyInfo Customer", email = email).id

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }

                regionDetailsList.forEach {
                    assertTrue(it.status == AVAILABLE, "All regions should be in available state")
                }
            }

            get<String> {
                path = "/regions/sg/kyc/dave/S7808018C"
                this.email = email
            }

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }
                val sgRegionDetails = regionDetailsList.singleOrNull { it.region.id == "sg" }
                assertTrue(sgRegionDetails != null, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.APPROVED,
                                KycType.JUMIO.name to KycStatus.PENDING,
                                KycType.ADDRESS.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, sgRegionDetails, "RegionDetails do not match")
            }

            val scanInfo: ScanInformation = post {
                path = "/regions/sg/kyc/jumio/scans"
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
            val imgUrl = "https://www.gstatic.com/webp/gallery3/1.png"
            val imgUrl2 = "https://www.gstatic.com/webp/gallery3/2.png"
            dataMap["livenessImages"] = listOf(imgUrl, imgUrl2)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }

                val sgRegionDetails = regionDetailsList.singleOrNull { it.region.id == "sg" }
                assertTrue(sgRegionDetails != null, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.APPROVED,
                                KycType.JUMIO.name to KycStatus.APPROVED,
                                KycType.ADDRESS.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, sgRegionDetails, "RegionDetails do not match")
            }

            put<String>(expectedResultCode = 204) {
                path = "/regions/sg/kyc/profile"
                this.email = email
                queryParams = mapOf("address" to "Singapore")
            }

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }

                val sgRegionDetails = regionDetailsList.singleOrNull { it.region.id == "sg" }
                assertTrue(sgRegionDetails != null, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(APPROVED)
                        .kycStatusMap(mutableMapOf(
                                KycType.JUMIO.name to KycStatus.APPROVED,
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.ADDRESS.name to KycStatus.APPROVED,
                                KycType.NRIC_FIN.name to KycStatus.APPROVED))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, sgRegionDetails, "RegionDetails do not match")
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - Jumio and address`() {

        val email = "myinfo-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test MyInfo Customer", email = email).id

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }

                regionDetailsList.forEach {
                    assertTrue(it.status == AVAILABLE, "All regions should be in available state")
                }
            }

            val scanInfo: ScanInformation = post {
                path = "/regions/sg/kyc/jumio/scans"
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
            val imgUrl = "https://www.gstatic.com/webp/gallery3/1.png"
            val imgUrl2 = "https://www.gstatic.com/webp/gallery3/2.png"
            dataMap["livenessImages"] = listOf(imgUrl, imgUrl2)

            post<ScanInformation>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
                path = "/ekyc/callback"
                body = dataMap
            }

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }

                val sgRegionDetails = regionDetailsList.singleOrNull { it.region.id == "sg" }
                assertTrue(sgRegionDetails != null, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.PENDING,
                                KycType.JUMIO.name to KycStatus.APPROVED,
                                KycType.ADDRESS.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, sgRegionDetails, "RegionDetails do not match")
            }

            put<String>(expectedResultCode = 204) {
                path = "/regions/sg/kyc/profile"
                this.email = email
                queryParams = mapOf("address" to "Singapore")
            }

            run {
                val regionDetailsList = get<RegionDetailsList> {
                    path = "/regions"
                    this.email = email
                }

                val sgRegionDetails = regionDetailsList.singleOrNull { it.region.id == "sg" }
                assertTrue(sgRegionDetails != null, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(APPROVED)
                        .kycStatusMap(mutableMapOf(
                                KycType.JUMIO.name to KycStatus.APPROVED,
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.ADDRESS.name to KycStatus.APPROVED,
                                KycType.NRIC_FIN.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, sgRegionDetails, "RegionDetails do not match")
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class ReferralTest {

    @Test
    fun `jersey test - POST customer with invalid referred by`() {

        val email = "referred_by_invalid-${randomInt()}@test.com"

        val invalid = "invalid_referrer@test.com"

        val customer = Customer()
                .contactEmail(email)
                .nickname("Test Referral Second User")
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
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Referral First User", email = firstEmail).id

            val secondEmail = "referral_second-${randomInt()}@test.com"

            val customer = Customer()
                    .contactEmail(secondEmail)
                    .nickname("Test Referral Second User")
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
                    .price(Price().amount(0).currency("NOK"))
                    .payment(emptyMap<String, String>())
                    .properties(mapOf(
                            "noOfBytes" to "1_000_000_000",
                            "productClass" to "SIMPLE_DATA"))
                    .presentation(emptyMap<String, String>())

            assertEquals(listOf(freeProductForReferred), secondSubscriberPurchases.map { it.product })
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class PlanTest {

    @Test
    fun `jersey test - POST purchase plan`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Purchase Plan User", email = email).id
            enableRegion(email = email, region = "sg")

            val sourceId = StripePayment.createPaymentTokenId()

            post<String> {
                path = "/products/PLAN_1000SGD_YEAR/purchase"
                this.email = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            Thread.sleep(200) // wait for 200 ms for balance to be updated in db

            val purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedPlanProductSG, purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class RenewPlanTest {

    @Test
    fun `jersey test - POST purchase plan with trial time`() {

        val email = "purchase-${randomInt()}@test.com"
        val sku = "PLAN_10USD_DAY"
        var customerId = ""

        try {
            customerId = createCustomer(name = "Test Purchase Plan User", email = email).id
            enableRegion(email = email, region = "us")

            val sourceId = StripePayment.createPaymentTokenId()

            post<String> {
                path = "/products/$sku/purchase"
                this.email = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            Thread.sleep(200) // wait for 200 ms for balance to be updated in db

            var purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }
            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing purchase record" }

            // First record - free product
            // Second record - product subscribed to
            assert(2 == purchaseRecords.size) { "Got ${purchaseRecords.size} purchase records, expected 2" }
            assertEquals(expectedPlanProductUS, purchaseRecords.last().product, "Incorrect 'Product' in purchase record")

            // Actual charge for renewal will first be done after trial time
            // expires, which will happen after 4 sec. Waiting a bit longer
            // before checking the outcome.
            StripeEventListener.waitForSubscriptionPaymentToSucceed(
                    subscription = "stripe-event-jersey-purchase-ok-sub",
                    customerId = customerId,
                    timeout = 30000L)

            Thread.sleep(200)

            purchaseRecords = get {
                path = "/purchases"
                this.email = email
            }
            assert(3 == purchaseRecords.size) { "Got ${purchaseRecords.size} purchase records, expected 3 after renewal" }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - POST purchase plan with trial time and with insuffient funds payment source`() {

        val email = "purchase-${randomInt()}@test.com"
        val sku = "PLAN_10USD_DAY"
        var customerId = ""

        try {
            customerId = createCustomer(name = "Test Purchase Plan User", email = email).id
            enableRegion(email = email, region = "us")

            val sourceId = StripePayment.createPaymentSourceId()
            val insuffientFundsSourceId = StripePayment.createInsuffientFundsPaymentSourceId()
            val newSourceId = StripePayment.createPaymentSourceId()

            post<String> {
                path = "/products/$sku/purchase"
                this.email = email
                queryParams = mapOf("sourceId" to sourceId)
            }

            // Wait for DB to be updated.
            Thread.sleep(200)

            var purchaseRecords: PurchaseRecordList = get {
                path = "/purchases"
                this.email = email
            }
            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing purchase record" }

            // First record - created when trial time starts (cost is 0).
            // Second record - actual charge record.
            assert(2 == purchaseRecords.size) { "Got ${purchaseRecords.size} purchase records, expected 2" }
            assertEquals(expectedPlanProductUS, purchaseRecords.last().product, "Incorrect 'Product' in purchase record")

            // Switch to a "no funds" card.
            // This should cause subscription renewal to fail.
            post<PaymentSource> {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to insuffientFundsSourceId)
            }
            put<PaymentSource> {
                path = "/paymentSources"
                this.email = email
                queryParams = mapOf("sourceId" to insuffientFundsSourceId)
            }

            // Actual charge for renewal will first be done after trial time
            // expires, which will happen after 4 sec. Waiting a bit longer
            // before checking the outcome.
            StripeEventListener.waitForFailedSubscriptionRenewal(
                    subscription = "stripe-event-jersey-purchase-fail-sub",
                    customerId = customerId,
                    timeout = 30000L)

            // Wait for DB to be updated (creation of the 'pending payment' relation.
            // (200 ms is too low...)
            Thread.sleep(2000)

            purchaseRecords = get {
                path = "/purchases"
                this.email = email
            }
            assert(2 == purchaseRecords.size) { "Got ${purchaseRecords.size} purchase records, expected 2 due to renewal failure" }

            // Explicitly renew the subscription with a new card.
            post<String> {
                path = "/products/$sku/renew"
                this.email = email
                queryParams = mapOf("sourceId" to newSourceId)
            }

            StripeEventListener.waitForSubscriptionPaymentToSucceed(
                    subscription = "stripe-event-jersey-purchase-fail-sub",
                    customerId = customerId,
                    timeout = 30000L)

            // Wait for DB to be updated.
            Thread.sleep(200)

            purchaseRecords = get {
                path = "/purchases"
                this.email = email
            }
            assert(3 == purchaseRecords.size) { "Got ${purchaseRecords.size} purchase records, expected 3 after renewal completed" }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class GraphQlTests {

    data class Context(
            val customer: Customer? = null,
            val bundles: Collection<Bundle>? = null,
            val regions: Collection<RegionDetails>? = null,
            val subscriptions: Collection<Subscription>? = null,
            val products: Collection<Product>? = null,
            val purchases: Collection<PurchaseRecord>? = null)

    data class Data(var context: Context? = null)

    data class GraphQlResponse(var data: Data? = null, var errors: List<String>? = null)

    @Test
    fun `jersey test - POST graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer("Test GraphQL POST Endpoint", email).id

            enableRegion(email)

            val msisdn = createSubscription(email)

            val response = post<GraphQlResponse>(expectedResultCode = 200) {
                path = "/graphql"
                this.email = email
                body = mapOf("query" to graphqlPostQuery)
            }

            val context = response.data?.context

            assertEquals(expected = "Test GraphQL POST Endpoint", actual = context?.customer?.nickname)
            assertEquals(expected = email, actual = context?.customer?.contactEmail)
            assertEquals(expected = 2_147_483_648L, actual = context?.bundles?.first()?.balance)
            assertEquals(expected = "no", actual = context?.regions?.first()?.region?.id)
            assertEquals(expected = "Norway", actual = context?.regions?.first()?.region?.name)
            assertEquals(expected = APPROVED, actual = context?.regions?.first()?.status)
            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.APPROVED),
                    actual = context?.regions?.first()?.kycStatusMap?.filterValues { it != null }
            )
            assertEquals(expected = "TEST-unknown", actual = context?.regions?.first()?.simProfiles?.first()?.iccId)
            assertEquals(expected = "Dummy eSIM", actual = context?.regions?.first()?.simProfiles?.first()?.eSimActivationCode)
            assertEquals(expected = "default", actual = context?.regions?.first()?.simProfiles?.first()?.alias)
            assertEquals(expected = SimProfile.StatusEnum.INSTALLED, actual = context?.regions?.first()?.simProfiles?.first()?.status)
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `jersey test - GET graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer("Test GraphQL GET Endpoint", email).id

            enableRegion(email)

            val msisdn = createSubscription(email)

            val response = get<GraphQlResponse> {
                path = "/graphql"
                this.email = email
                queryParams = mapOf("query" to URLEncoder.encode(graphqlGetQuery, StandardCharsets.UTF_8))
            }

            val context = response.data?.context

            assertEquals(expected = "Test GraphQL GET Endpoint", actual = context?.customer?.nickname)
            assertEquals(expected = email, actual = context?.customer?.contactEmail)
            assertEquals(expected = 2_147_483_648L, actual = context?.bundles?.first()?.balance)
            assertEquals(expected = "no", actual = context?.regions?.first()?.region?.id)
            assertEquals(expected = "Norway", actual = context?.regions?.first()?.region?.name)
            assertEquals(expected = APPROVED, actual = context?.regions?.first()?.status)
            assertEquals(
                    expected = mapOf(KycType.JUMIO.name to KycStatus.APPROVED),
                    actual = context?.regions?.first()?.kycStatusMap?.filterValues { it != null }
            )
            assertEquals(expected = "TEST-unknown", actual = context?.regions?.first()?.simProfiles?.first()?.iccId)
            assertEquals(expected = "Dummy eSIM", actual = context?.regions?.first()?.simProfiles?.first()?.eSimActivationCode)
            assertEquals(expected = "default", actual = context?.regions?.first()?.simProfiles?.first()?.alias)
            assertEquals(expected = SimProfile.StatusEnum.INSTALLED, actual = context?.regions?.first()?.simProfiles?.first()?.status)
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}