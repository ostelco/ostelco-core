package org.ostelco.at.okhttp

import org.junit.Test
import org.ostelco.at.common.StripePayment
import org.ostelco.at.common.createCustomer
import org.ostelco.at.common.createSubscription
import org.ostelco.at.common.enableRegion
import org.ostelco.at.common.expectedProducts
import org.ostelco.at.common.getLogger
import org.ostelco.at.common.randomInt
import org.ostelco.at.jersey.post
import org.ostelco.at.okhttp.ClientFactory.clientForSubject
import org.ostelco.prime.customer.api.DefaultApi
import org.ostelco.prime.customer.model.ApplicationToken
import org.ostelco.prime.customer.model.Customer
import org.ostelco.prime.customer.model.GraphQLRequest
import org.ostelco.prime.customer.model.PaymentSource
import org.ostelco.prime.customer.model.Person
import org.ostelco.prime.customer.model.PersonList
import org.ostelco.prime.customer.model.Price
import org.ostelco.prime.customer.model.Product
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CustomerTest {

    @Test
    fun `okhttp test - GET and PUT customer`() {

        val email = "customer-${randomInt()}@test.com"

        val client = clientForSubject(subject = email)

        val createCustomer = Customer()
                .contactEmail(email)
                .nickname("Test Customer")
                .analyticsId("")
                .referralId("")

        client.createCustomer(createCustomer.nickname, createCustomer.contactEmail, null)

        val customer: Customer = client.customer

        assertEquals(email, customer.contactEmail, "Incorrect 'contactEmail' in fetched customer")
        assertEquals(createCustomer.nickname, customer.nickname, "Incorrect 'name' in fetched customer")

        val newName = "New name: Test Customer"

        customer.nickname(newName)

        val updatedCustomer: Customer = client.updateCustomer(customer.nickname, null)

        assertEquals(email, updatedCustomer.contactEmail, "Incorrect 'contactEmail' in response after updating customer")
        assertEquals(newName, updatedCustomer.nickname, "Incorrect 'nickname' in response after updating customer")
    }

    @Test
    fun `okhttp test - GET application token`() {

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
        createCustomer(name = "Test Subscriptions User", email = email)
        val msisdn = createSubscription(email)

        val client = clientForSubject(subject = email)

        val subscriptions = client.getSubscriptions("no")

        assertEquals(listOf(msisdn), subscriptions.map { it.msisdn })
    }
}

class BundlesAndPurchasesTest {

    private val logger by getLogger()

    @Test
    fun `okhttp test - GET bundles`() {

        val email = "balance-${randomInt()}@test.com"
        createCustomer(name = "Test Balance User", email = email)

        val client = clientForSubject(subject = email)

        val bundles = client.bundles

        logger.info("Balance: ${bundles[0].balance}")

        val freeProduct = Product()
                .sku("100MB_FREE_ON_JOINING")
                .price(Price().apply {
                    this.amount = 0
                    this.currency = "NOK"
                })
                .properties(mapOf("noOfBytes" to "100_000_000"))
                .presentation(emptyMap<String, String>())

        val purchaseRecords = client.purchaseHistory
        purchaseRecords.sortBy { it.timestamp }

        assertEquals(freeProduct, purchaseRecords.first().product, "Incorrect first 'Product' in purchase record")
    }
}

class GetProductsTest {

    @Test
    fun `okhttp test - GET products`() {

        val email = "products-${randomInt()}@test.com"
        createCustomer(name = "Test Products User", email = email)
        enableRegion(email = email)

        val client = clientForSubject(subject = email)

        val products = client.allProducts.toList()

        assertEquals(expectedProducts().toSet(), products.toSet(), "Incorrect 'Products' fetched")
    }
}

class SourceTest {

    @Test
    fun `okhttp test - POST source create`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test create Payment Source", email = email).id

            val client = clientForSubject(subject = email)

            val tokenId = StripePayment.createPaymentTokenId()
            val cardId = StripePayment.getCardIdForTokenId(tokenId)

            // Ties source with user profile both local and with Stripe
            client.createSource(tokenId)

            Thread.sleep(200)

            val sources = client.listSources()

            assert(sources.isNotEmpty()) { "Expected at least one payment source for profile $email" }
            assertNotNull(sources.first { it.id == cardId },
                    "Expected card $cardId in list of payment sources for profile $email")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - GET list sources`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test list Payment Source", email = email).id

            val client = clientForSubject(subject = email)

            Thread.sleep(200)

            val createdIds = listOf(createTokenWithStripe(client),
                    createSourceWithStripe(client),
                    createTokenWithStripe(client),
                    createSourceWithStripe(client))

            val sources = client.listSources()

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
    fun `okhttp test - GET list sources no profile`() {

        val email = "purchase-${randomInt()}@test.com"

        var customerId = ""
        try {
            customerId = createCustomer(name = "Test get list Sources", email = email).id

            val client = clientForSubject(subject = email)

            Thread.sleep(200)

            val sources = client.listSources()

            assert(sources.isEmpty()) { "Expected no payment source for profile $email" }

            assertNotNull(StripePayment.getStripeCustomerId(customerId = customerId)) { "Customer Id should have been created" }

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - PUT source set default`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test update Payment Source", email = email).id

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
            val stripeCustomerId = StripePayment.getStripeCustomerId(customerId = customerId)

            // Verify that original 'sourceId/card' is default.
            assertEquals(cardId, StripePayment.getDefaultSourceForCustomer(stripeCustomerId),
                    "Expected $cardId to be default source for $stripeCustomerId")

            // Set new default card.
            client.setDefaultSource(newCardId)

            assertEquals(newCardId, StripePayment.getDefaultSourceForCustomer(stripeCustomerId),
                    "Expected $newCardId to be default source for $stripeCustomerId")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - DELETE source`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test delete Payment Source", email = email).id

            val client = clientForSubject(subject = email)

            Thread.sleep(200)

            val createdIds = listOf(getCardIdForTokenFromStripe(createTokenWithStripe(client)),
                    createSourceWithStripe(client))

            val deletedIds = createdIds.map { deleteSourceWithStripe(client, it) }

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

    private fun createTokenWithStripe(client: DefaultApi): String {
        val tokenId = StripePayment.createPaymentTokenId()

        client.createSource(tokenId)

        return tokenId
    }

    private fun createSourceWithStripe(client: DefaultApi): String {
        val sourceId = StripePayment.createPaymentSourceId()

        client.createSource(sourceId)

        return sourceId
    }

    private fun deleteSourceWithStripe(client: DefaultApi, sourceId: String): String {

        val removedSource = client.removeSource(sourceId)

        return removedSource.id
    }
}

class PurchaseTest {

    @Test
    fun `okhttp test - POST products purchase`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Purchase User", email = email).id
            enableRegion(email = email)

            val client = clientForSubject(subject = email)

            val balanceBefore = client.bundles.first().balance

            val sourceId = StripePayment.createPaymentTokenId()

            client.purchaseProduct("1GB_249NOK", sourceId, false)

            Thread.sleep(200) // wait for 200 ms for balance to be updated in db

            val balanceAfter = client.bundles.first().balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords = client.purchaseHistory

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - POST products purchase using default source`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Purchase with Default Payment Source", email = email).id
            enableRegion(email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val client = clientForSubject(subject = email)

            val paymentSource: PaymentSource = client.createSource(sourceId)

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            val balanceBefore = client.bundles.first().balance

            val productSku = "1GB_249NOK"

            client.purchaseProduct(productSku, null, null)

            Thread.sleep(200) // wait for 200 ms for balance to be updated in db

            val balanceAfter = client.bundles.first().balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords = client.purchaseHistory

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - POST products purchase add source then pay with it`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Purchase with adding Payment Source", email = email).id
            enableRegion(email = email)

            val sourceId = StripePayment.createPaymentTokenId()

            val client = clientForSubject(subject = email)

            val paymentSource: PaymentSource = client.createSource(sourceId)

            assertNotNull(paymentSource.id, message = "Failed to create payment source")

            val balanceBefore = client.bundles[0].balance

            val productSku = "1GB_249NOK"

            client.purchaseProduct(productSku, paymentSource.id, null)

            Thread.sleep(200) // wait for 200 ms for balance to be updated in db

            val balanceAfter = client.bundles[0].balance

            assertEquals(1_000_000_000, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords = client.purchaseHistory

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
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
    fun `okhttp test - POST customer with invalid referred by`() {

        val email = "referred_by_invalid-${randomInt()}@test.com"

        val client = clientForSubject(subject = email)

        val invalid = "invalid_referrer@test.com"

        val customer = Customer()
                .contactEmail(email)
                .nickname("Test Referral Second User")

        val failedToCreate = assertFails {
            client.createCustomer(customer.nickname, customer.contactEmail, invalid)
        }

        assertEquals("""
{"description":"Incomplete customer description. Subscriber - $invalid not found."} expected:<201> but was:<403>
        """.trimIndent(), failedToCreate.message)

        val failedToGet = assertFails {
            client.customer
        }

        assertEquals("""
{"description":"Incomplete customer description. Subscriber - $email not found."} expected:<200> but was:<404>
        """.trimIndent(), failedToGet.message)
    }

    @Test
    fun `okhttp test - POST customer`() {

        val firstEmail = "referral_first-${randomInt()}@test.com"
        createCustomer(name = "Test Referral First User", email = firstEmail)

        val secondEmail = "referral_second-${randomInt()}@test.com"

        val customer = Customer()
                .contactEmail(secondEmail)
                .nickname("Test Referral Second User")
                .referralId("")

        val firstEmailClient = clientForSubject(subject = firstEmail)
        val secondEmailClient = clientForSubject(subject = secondEmail)

        secondEmailClient.createCustomer(customer.nickname, firstEmail, null)

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

        assertEquals(1_000_000_000, secondEmailClient.bundles[0].balance)

        val freeProductForReferred = Product()
                .sku("1GB_FREE_ON_REFERRED")
                .price(Price().apply {
                    this.amount = 0
                    this.currency = "NOK"
                })
                .properties(mapOf("noOfBytes" to "1_000_000_000"))
                .presentation(emptyMap<String, String>())

        assertEquals(listOf(freeProductForReferred), secondEmailClient.purchaseHistory.map { it.product })
    }
}

class GraphQlTests {

    @Test
    fun `okhttp test - POST graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        createCustomer("Test GraphQL Endpoint", email)

        createSubscription(email)

        val client = clientForSubject(subject = email)

        val request = GraphQLRequest()
        request.query = """{ context(id: "$email") { customer { email } } }"""

        val map = client.graphql(request) as Map<String, *>

        println(map)

        assertNotNull(actual = map["data"], message = "Data is null")
        assertNull(actual = map["error"], message = "Error is not null")

    }
}