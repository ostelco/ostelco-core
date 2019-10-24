package org.ostelco.at.okhttp

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Ignore
import org.junit.Test
import org.ostelco.at.common.StripePayment
import org.ostelco.at.common.createCustomer
import org.ostelco.at.common.createSubscription
import org.ostelco.at.common.enableRegion
import org.ostelco.at.common.expectedPlanProduct
import org.ostelco.at.common.expectedProducts
import org.ostelco.at.common.getLogger
import org.ostelco.at.common.graphqlGetQuery
import org.ostelco.at.common.graphqlPostQuery
import org.ostelco.at.common.randomInt
import org.ostelco.at.jersey.post
import org.ostelco.at.okhttp.ClientFactory.clientForSubject
import org.ostelco.prime.customer.api.DefaultApi
import org.ostelco.prime.customer.model.ApplicationToken
import org.ostelco.prime.customer.model.Customer
import org.ostelco.prime.customer.model.GraphQLRequest
import org.ostelco.prime.customer.model.KycStatus
import org.ostelco.prime.customer.model.KycType
import org.ostelco.prime.customer.model.PaymentSource
import org.ostelco.prime.customer.model.Person
import org.ostelco.prime.customer.model.PersonList
import org.ostelco.prime.customer.model.Price
import org.ostelco.prime.customer.model.Product
import org.ostelco.prime.customer.model.Region
import org.ostelco.prime.customer.model.RegionDetails
import org.ostelco.prime.customer.model.RegionDetails.StatusEnum.APPROVED
import org.ostelco.prime.customer.model.RegionDetails.StatusEnum.PENDING
import org.ostelco.prime.customer.model.RegionDetails.StatusEnum.AVAILABLE
import org.ostelco.prime.customer.model.RegionDetailsList
import org.ostelco.prime.customer.model.ScanInformation
import org.ostelco.prime.customer.model.SimProfile
import org.ostelco.prime.customer.model.SimProfileList
import java.time.Instant
import java.util.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap
import kotlin.collections.set
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomerTest {

    @Test
    fun `okhttp test - GET and PUT customer`() {

        val email = "customer-${randomInt()}@test.com"
        var customerId = ""
        try {
            val client = clientForSubject(subject = email)

            val nickname = "Test Customer"

            client.createCustomer(nickname, email, null)

            val customer: Customer = client.customer
            customerId = customer.id

            assertEquals(email, customer.contactEmail, "Incorrect 'contactEmail' in fetched customer")
            assertEquals(nickname, customer.nickname, "Incorrect 'name' in fetched customer")

            val newNickname = "New name: Test Customer"

            customer.nickname(newNickname)

            val updatedCustomer: Customer = client.updateCustomer(customer.nickname, null)

            assertEquals(email, updatedCustomer.contactEmail, "Incorrect 'contactEmail' in response after updating customer")
            assertEquals(newNickname, updatedCustomer.nickname, "Incorrect 'nickname' in response after updating customer")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - POST application token`() {

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

            val client = clientForSubject(subject = email)

            val reply = client.storeApplicationToken(testToken)

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
    fun `okhttp test - GET regions - No regions`() {

        val email = "regions-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test No Region User", email = email).id

            val client = clientForSubject(subject = email)

            val regionDetailsList: RegionDetailsList = client.allRegions

            regionDetailsList.forEach {
                assertTrue(it.status == AVAILABLE, "All regions should be in available state")
            }

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - GET regions - Single Region with no profiles`() {

        val email = "regions-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Single Region User", email = email).id
            enableRegion(email = email)

            val client = clientForSubject(subject = email)

            val regionDetailsList: RegionDetailsList = client.allRegions

            val noRegionIndex = regionDetailsList.indexOfFirst { it.region.id == "no" }
            assertTrue(noRegionIndex != -1, "regionDetailsList should contain 'no' region")

            val regionDetails = RegionDetails()
                    .region(Region().id("no").name("Norway"))
                    .status(APPROVED)
                    .kycStatusMap(mapOf(KycType.JUMIO.name to KycStatus.APPROVED))
                    .simProfiles(SimProfileList())

            assertEquals(regionDetails, regionDetailsList[noRegionIndex], "RegionDetails do not match")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Ignore
    @Test
    fun `okhttp test - GET regions - Single Region with one profile`() {

        val email = "regions-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Single Region User", email = email).id
            enableRegion(email = email)

            val client = clientForSubject(subject = email)

            client.provisionSimProfile("no", null)

            val regionDetailsList = client.allRegions

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
    fun `okhttp test - GET subscriptions`() {

        val email = "subs-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Subscriptions User", email = email).id
            val msisdn = createSubscription(email)

            val client = clientForSubject(subject = email)

            val subscriptions = client.subscriptions

            assertEquals(listOf(msisdn), subscriptions.map { it.msisdn })
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class BundlesAndPurchasesTest {

    private val logger by getLogger()

    @Test
    fun `okhttp test - GET bundles`() {

        val email = "balance-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Balance User", email = email).id

            val client = clientForSubject(subject = email)

            val bundles = client.bundles

            logger.info("Balance: ${bundles[0].balance}")

            val freeProduct = Product()
                    .sku("2GB_FREE_ON_JOINING")
                    .price(Price().amount(0).currency(""))
                    .payment(emptyMap<String, String>())
                    .properties(mapOf(
                            "noOfBytes" to "2_147_483_648",
                            "productClass" to "SIMPLE_DATA"))
                    .presentation(emptyMap<String, String>())

            val purchaseRecords = client.purchaseHistory
            purchaseRecords.sortBy { it.timestamp }

            assertEquals(freeProduct, purchaseRecords.first().product, "Incorrect first 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class GetProductsTest {

    @Test
    fun `okhttp test - GET products`() {

        val email = "products-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Products User", email = email).id
            enableRegion(email = email)

            val client = clientForSubject(subject = email)

            val products = client.allProducts.toList()

            assertEquals(expectedProducts().toSet(), products.toSet(), "Incorrect 'Products' fetched")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
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
                    createSourceWithStripeNoAddress(client))

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

    private fun createSourceWithStripeNoAddress(client: DefaultApi): String {
        val sourceId = StripePayment.createPaymentSourceIdNoAddress()

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

            assertEquals(1_073_741_824, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

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

            assertEquals(1_073_741_824, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

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

            assertEquals(1_073_741_824, balanceAfter - balanceBefore, "Balance did not increased by 1GB after Purchase")

            val purchaseRecords = client.purchaseHistory

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedProducts().first(), purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

// TODO Prasanth: add okhttp acceptance tests for Jumio

class SingaporeKycTest {

    @Test
    fun `okhttp test - GET myinfoConfig v2`() {

        val email = "myinfo-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test MyInfoConfig v2 Customer", email = email).id

            val client = clientForSubject(subject = email)

            val myInfoConfig = client.myInfoV2Config

            assertEquals(
                    "http://ext-myinfo-emulator:8080/v2/authorise" +
                            "?client_id=STG2-MYINFO-SELF-TEST" +
                            "&attributes=name,sex,dob,residentialstatus,nationality,mobileno,email,mailadd" +
                            "&redirect_uri=http://localhost:3001/callback",
                    myInfoConfig.url)

        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - GET myinfoConfig v3`() {

        val email = "myinfo-v3-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test MyInfoConfig v3 Customer", email = email).id

            val client = clientForSubject(subject = email)

            val myInfoConfig = client.myInfoV3Config

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
    fun `okhttp test - GET myinfo v2`() {

        val email = "myinfo-v2-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test MyInfo v2 Customer", email = email).id

            val client = clientForSubject(subject = email)

            run {
                val regionDetailsList = client.allRegions

                regionDetailsList.forEach {
                    assertTrue(it.status == AVAILABLE, "All regions should be in available state")
                }
            }

            val personData: String = jacksonObjectMapper().writeValueAsString(client.getCustomerMyInfoV2Data("authCode"))

            val expectedPersonData = """{"name":{"lastupdated":"2018-03-20","source":"1","classification":"C","value":"TAN XIAO HUI"},"sex":{"lastupdated":"2018-03-20","source":"1","classification":"C","value":"F"},"nationality":{"lastupdated":"2018-03-20","source":"1","classification":"C","value":"SG"},"dob":{"lastupdated":"2018-03-20","source":"1","classification":"C","value":"1970-05-17"},"email":{"lastupdated":"2018-08-23","source":"4","classification":"C","value":"myinfotesting@gmail.com"},"mobileno":{"lastupdated":"2018-08-23","code":"65","source":"4","classification":"C","prefix":"+","nbr":"97399245"},"mailadd":{"country":"SG","unit":"128","street":"BEDOK NORTH AVENUE 4","lastupdated":"2018-03-20","block":"102","postal":"460102","source":"1","classification":"C","floor":"09","building":"PEARL GARDEN"},"uinfin":"S9812381D"}"""
            assertEquals(expectedPersonData, personData, "MyInfo PersonData do not match")

            run {
                val regionDetailsList = client.allRegions

                val sgRegionIndex = regionDetailsList.indexOfFirst { it.region.id == "sg" }
                assertTrue(sgRegionIndex != -1, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.JUMIO.name to KycStatus.PENDING,
                                KycType.MY_INFO.name to KycStatus.APPROVED,
                                KycType.ADDRESS.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, regionDetailsList[sgRegionIndex], "RegionDetails do not match")
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - GET myinfo v3`() {

        val email = "myinfo-${randomInt()}@test.com"
        var customerId = ""
        try {

            customerId = createCustomer(name = "Test MyInfo Customer", email = email).id

            val client = clientForSubject(subject = email)

            run {
                val regionDetailsList = client.allRegions

                regionDetailsList.forEach {
                    assertTrue(it.status == AVAILABLE, "All regions should be in available state")
                }
            }

            val personData: String = jacksonObjectMapper().writeValueAsString(client.getCustomerMyInfoV3Data("authCode"))

            val expectedPersonData = """{"name":{"lastupdated":"2019-04-05","source":"1","classification":"C","value":"TAN XIAO HUI"},"sex":{"lastupdated":"2019-04-05","code":"F","source":"1","classification":"C","desc":"FEMALE"},"nationality":{"lastupdated":"2019-04-05","code":"SG","source":"1","classification":"C","desc":"SINGAPORE CITIZEN"},"dob":{"lastupdated":"2019-04-05","source":"1","classification":"C","value":"1998-06-06"},"email":{"lastupdated":"2019-04-05","source":"2","classification":"C","value":"myinfotesting@gmail.com"},"mobileno":{"lastupdated":"2019-04-05","source":"2","classification":"C","areacode":{"value":"65"},"prefix":{"value":"+"},"nbr":{"value":"97399245"}},"mailadd":{"country":{"code":"SG","desc":"SINGAPORE"},"unit":{"value":"128"},"street":{"value":"BEDOK NORTH AVENUE 4"},"lastupdated":"2019-04-05","block":{"value":"102"},"source":"1","postal":{"value":"460102"},"classification":"C","floor":{"value":"09"},"type":"SG","building":{"value":"PEARL GARDEN"}}}"""
            assertEquals(expectedPersonData, personData, "MyInfo PersonData do not match")

            run {
                val regionDetailsList = client.allRegions

                val sgRegionIndex = regionDetailsList.indexOfFirst { it.region.id == "sg" }
                assertTrue(sgRegionIndex != -1, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.JUMIO.name to KycStatus.PENDING,
                                KycType.MY_INFO.name to KycStatus.APPROVED,
                                KycType.ADDRESS.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, regionDetailsList[sgRegionIndex], "RegionDetails do not match")
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - NRIC, Jumio and address`() {

        val email = "myinfo-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test MyInfo Customer", email = email).id

            val client = clientForSubject(subject = email)

            run {
                val regionDetailsList = client.allRegions

                regionDetailsList.forEach {
                    assertTrue(it.status == AVAILABLE, "All regions should be in available state")
                }
            }

            client.checkNricFinId("S7808018C")

            run {
                val regionDetailsList = client.allRegions

                val sgRegionIndex = regionDetailsList.indexOfFirst { it.region.id == "sg" }
                assertTrue(sgRegionIndex != -1, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.APPROVED,
                                KycType.JUMIO.name to KycStatus.PENDING,
                                KycType.ADDRESS.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, regionDetailsList[sgRegionIndex], "RegionDetails do not match")
            }

            val scanInfo: ScanInformation = client.createNewJumioKycScanId("sg")

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
                val regionDetailsList = client.allRegions

                val sgRegionIndex = regionDetailsList.indexOfFirst { it.region.id == "sg" }
                assertTrue(sgRegionIndex != -1, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.APPROVED,
                                KycType.JUMIO.name to KycStatus.APPROVED,
                                KycType.ADDRESS.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, regionDetailsList[sgRegionIndex], "RegionDetails do not match")
            }

            client.updateDetails("Singapore")

            run {
                val regionDetailsList = client.allRegions

                val sgRegionIndex = regionDetailsList.indexOfFirst { it.region.id == "sg" }
                assertTrue(sgRegionIndex != -1, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(APPROVED)
                        .kycStatusMap(mutableMapOf(
                                KycType.JUMIO.name to KycStatus.APPROVED,
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.ADDRESS.name to KycStatus.APPROVED,
                                KycType.NRIC_FIN.name to KycStatus.APPROVED))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, regionDetailsList[sgRegionIndex], "RegionDetails do not match")
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - Jumio and address`() {

        val email = "myinfo-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test MyInfo Customer", email = email).id

            val client = clientForSubject(subject = email)

            run {
                val regionDetailsList = client.allRegions

                regionDetailsList.forEach {
                    assertTrue(it.status == AVAILABLE, "All regions should be in available state")
                }
            }

            val scanInfo: ScanInformation = client.createNewJumioKycScanId("sg")

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
                val regionDetailsList = client.allRegions

                val sgRegionIndex = regionDetailsList.indexOfFirst { it.region.id == "sg" }
                assertTrue(sgRegionIndex != -1, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(PENDING)
                        .kycStatusMap(mutableMapOf(
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.NRIC_FIN.name to KycStatus.PENDING,
                                KycType.JUMIO.name to KycStatus.APPROVED,
                                KycType.ADDRESS.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, regionDetailsList[sgRegionIndex], "RegionDetails do not match")
            }

            client.updateDetails("Singapore")

            run {
                val regionDetailsList = client.allRegions

                val sgRegionIndex = regionDetailsList.indexOfFirst { it.region.id == "sg" }
                assertTrue(sgRegionIndex != -1, "regionDetailsList should contain sg region")

                val regionDetails = RegionDetails()
                        .region(Region().id("sg").name("Singapore"))
                        .status(APPROVED)
                        .kycStatusMap(mutableMapOf(
                                KycType.JUMIO.name to KycStatus.APPROVED,
                                KycType.MY_INFO.name to KycStatus.PENDING,
                                KycType.ADDRESS.name to KycStatus.APPROVED,
                                KycType.NRIC_FIN.name to KycStatus.PENDING))
                        .simProfiles(SimProfileList())

                assertEquals(regionDetails, regionDetailsList[sgRegionIndex], "RegionDetails do not match")
            }
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
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
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Referral First User", email = firstEmail).id

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
                    .price(Price().amount(0).currency("NOK"))
                    .payment(emptyMap<String, String>())
                    .properties(mapOf(
                            "noOfBytes" to "1_000_000_000",
                            "productClass" to "SIMPLE_DATA"))
                    .presentation(emptyMap<String, String>())

            assertEquals(listOf(freeProductForReferred), secondEmailClient.purchaseHistory.map { it.product })
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class PlanTest {

    @Test
    fun `okhttp test - POST purchase plan`() {

        val email = "purchase-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer(name = "Test Purchase Plan User", email = email).id
            enableRegion(email = email, region = "sg")

            val client = clientForSubject(subject = email)
            val sourceId = StripePayment.createPaymentTokenId()

            client.purchaseProduct("PLAN_1000SGD_YEAR", sourceId, false)

            Thread.sleep(200) // wait for 200 ms for balance to be updated in db

            val purchaseRecords = client.purchaseHistory

            purchaseRecords.sortBy { it.timestamp }

            assert(Instant.now().toEpochMilli() - purchaseRecords.last().timestamp < 10_000) { "Missing Purchase Record" }
            assertEquals(expectedPlanProduct, purchaseRecords.last().product, "Incorrect 'Product' in purchase record")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}

class GraphQlTests {

    private val logger by getLogger()

    @Test
    fun `okhttp test - POST graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer("Test GraphQL POST Endpoint", email).id

            enableRegion(email)

            createSubscription(email)

            val client = clientForSubject(subject = email)

            val request = GraphQLRequest()
            request.query = graphqlPostQuery

            val map = client.graphqlPost(request) as Map<String, *>

            logger.info("GraphQL POST response {}", map)

            assertNotNull(actual = map["data"], message = "Data is null")
            assertNull(actual = map["error"], message = "Error is not null")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }

    @Test
    fun `okhttp test - GET graphql`() {

        val email = "graphql-${randomInt()}@test.com"
        var customerId = ""
        try {
            customerId = createCustomer("Test GraphQL GET Endpoint", email).id

            enableRegion(email)

            val msisdn = createSubscription(email)

            val client = clientForSubject(subject = email)

            val map = client.graphqlGet(graphqlGetQuery) as Map<String, *>

            logger.info("GraphQL GET response {}", map)

            assertNotNull(actual = map["data"], message = "Data is null")
            assertNull(actual = map["error"], message = "Error is not null")
        } finally {
            StripePayment.deleteCustomer(customerId = customerId)
        }
    }
}