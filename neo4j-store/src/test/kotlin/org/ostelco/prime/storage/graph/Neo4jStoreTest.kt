package org.ostelco.prime.storage.graph

import arrow.core.right
import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import kotlinx.coroutines.runBlocking
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.neo4j.driver.v1.AccessMode.WRITE
import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.dsl.DSL.job
import org.ostelco.prime.kts.engine.KtsServiceFactory
import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.CustomerRegionStatus.APPROVED
import org.ostelco.prime.model.CustomerRegionStatus.AVAILABLE
import org.ostelco.prime.model.CustomerRegionStatus.PENDING
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.JumioScanData
import org.ostelco.prime.model.KycStatus
import org.ostelco.prime.model.KycType.ADDRESS
import org.ostelco.prime.model.KycType.JUMIO
import org.ostelco.prime.model.KycType.MY_INFO
import org.ostelco.prime.model.KycType.NRIC_FIN
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Region
import org.ostelco.prime.model.RegionDetails
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.ScanResult
import org.ostelco.prime.model.ScanStatus
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.model.SimProfile
import org.ostelco.prime.model.SimProfileStatus.AVAILABLE_FOR_DOWNLOAD
import org.ostelco.prime.notifications.EmailNotifier
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.InvoiceInfo
import org.ostelco.prime.paymentprocessor.core.InvoicePaymentInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.sim.SimManager
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.ScanInformationStore
import org.ostelco.prime.storage.graph.model.Segment
import org.ostelco.prime.tracing.Trace
import java.time.Instant
import java.util.*
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

private val mockPaymentProcessor = Mockito.mock(PaymentProcessor::class.java)
class MockPaymentProcessor : PaymentProcessor by mockPaymentProcessor

class MockAnalyticsService : AnalyticsService by Mockito.mock(AnalyticsService::class.java)

private val mockScanInformationStore = Mockito.mock(ScanInformationStore::class.java)
class MockScanInformationStore : ScanInformationStore by mockScanInformationStore

private val mockSimManager = Mockito.mock(SimManager::class.java)
class MockSimManager : SimManager by mockSimManager

private val mockEmailNotifier = Mockito.mock(EmailNotifier::class.java)
class MockEmailNotifier : EmailNotifier by mockEmailNotifier

private val mockAppNotifier = Mockito.mock(AppNotifier::class.java)
class MockAppNotifier : AppNotifier by mockAppNotifier

private val trace = object : Trace {
    override fun <T> childSpan(name: String, work: () -> T): T = work()
}

class MockTrace : Trace by trace

class Neo4jStoreTest {

    @BeforeTest
    fun clear() {

        Neo4jClient.driver.session(WRITE).use { session ->
            session.writeTransaction {
                it.run("MATCH (n) DETACH DELETE n")
            }
        }

        job {
            create {
                Product(sku = "2GB_FREE_ON_JOINING",
                        price = Price(0, ""),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "2_147_483_648"
                        )
                )
            }
            create {
                Product(sku = "1GB_FREE_ON_REFERRED",
                        price = Price(0, ""),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        )
                )
            }
            create {
                Segment(id = "country-${REGION.toLowerCase()}")
            }
        }
    }

    @Test
    fun `test - add customer`() {

        Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER,
                referredBy = null)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getCustomer(IDENTITY).bimap(
                { fail(it.message) },
                { assertEquals(CUSTOMER, it) })

        // TODO vihang: fix argument captor for neo4j-store tests
//        val bundleArgCaptor: ArgumentCaptor<Bundle> = ArgumentCaptor.forClass(Bundle::class.java)
//        verify(OCS_MOCK, times(1)).addBundle(bundleArgCaptor.capture())
//        assertEquals(Bundle(id = EMAIL, balance = 100_000_000), bundleArgCaptor.value)
    }

    @Test
    fun `test - get customer identity from contactEmail`() {

        Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER,
                referredBy = null)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getIdentityForContactEmail(contactEmail = EMAIL).bimap(
                { fail(it.message) },
                { identity: Identity ->
                    Neo4jStoreSingleton.getCustomer(identity).bimap(
                            { fail(it.message) },
                            { assertEquals(CUSTOMER, it) })})
    }

    @Test
    fun `test - customer identity from contactEmail`() {

        Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER,
                referredBy = null)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getIdentityForContactEmail(contactEmail = EMAIL).bimap(
                { fail(it.message) },
                { identity: Identity ->
                    assertEquals("EMAIL", identity.type)
                    assertEquals(EMAIL, identity.id)
                    assertEquals(IDENTITY.provider, identity.provider)
                })
    }

    @Test
    fun `test - fail to add customer with invalid referred by`() {

        Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER, referredBy = "blah")
                .fold({
                    assertEquals(
                            expected = "Failed to create REFERRED - blah -> ${CUSTOMER.id}",
                            actual = it.message)
                },
                        { fail("Created customer in spite of invalid 'referred by'") })
    }

    @Test
    fun `test - add subscription`() {

        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER)
                .mapLeft { fail(it.message) }

        // test
        Neo4jStoreSingleton.addSubscription(
                identity = IDENTITY,
                msisdn = MSISDN,
                iccId = UUID.randomUUID().toString(),
                regionCode = REGION_CODE,
                alias = "")
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getSubscriptions(IDENTITY).bimap(
                { fail(it.message) },
                { assertEquals(MSISDN, it.single().msisdn) })

        // TODO vihang: fix argument captor for neo4j-store tests
//        val msisdnArgCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
//        val bundleIdArgCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
//        verify(OCS_MOCK).addMsisdnToBundleMapping(msisdnArgCaptor.capture(), bundleIdArgCaptor.capture())
//        assertEquals(MSISDN, msisdnArgCaptor.value)
//        assertEquals(EMAIL, bundleIdArgCaptor.value)
    }

    @Test
    fun `test - purchase`() {

        val sku = "1GB_249NOK"
        val invoiceId = "in_01234"
        val chargeId = UUID.randomUUID().toString()

        // mock
        Mockito.`when`(mockPaymentProcessor.getPaymentProfile(customerId = CUSTOMER.id))
                .thenReturn(ProfileInfo(EMAIL).right())

        Mockito.`when`(mockPaymentProcessor.createInvoice(
                customerId = CUSTOMER.id,
                amount = 24900,
                currency = "NOK",
                description = sku,
                taxRegionId = "no",
                sourceId = null)
        ).thenReturn(InvoiceInfo(invoiceId).right())

        Mockito.`when`(mockPaymentProcessor.payInvoice(
                invoiceId = invoiceId)
        ).thenReturn(InvoicePaymentInfo(invoiceId, chargeId).right())

        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
            create { createProduct(sku = sku, taxRegionId = "no") }
        }.mapLeft { fail(it.message) }

        val offer = Offer(
                id = "NEW_OFFER",
                segments = listOf("country-${REGION.toLowerCase()}"),
                products = listOf(sku))

        Neo4jStoreSingleton.createOffer(offer)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.addSubscription(IDENTITY, REGION_CODE, ICC_ID, ALIAS, MSISDN)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = APPROVED,
                regionCode = REGION_CODE)

        // test
        Neo4jStoreSingleton.purchaseProduct(identity = IDENTITY, sku = sku, sourceId = null, saveCard = false)
                .mapLeft { fail(it.description) }

        // assert
        Neo4jStoreSingleton.getBundles(IDENTITY).bimap(
                { fail(it.message) },
                { bundles ->
                    bundles.forEach { bundle ->
                        assertEquals(3_221_225_472L, bundle.balance)
                    }
                })
    }

    @Test
    fun `test - consume`() = runBlocking {
        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.addSubscription(IDENTITY, REGION_CODE, ICC_ID, ALIAS, MSISDN)
                .mapLeft { fail(it.message) }

        // test

        // balance = 100_000_000
        // reserved = 0

        // requested = 40_000_000
        val dataBucketSize = 40_000_000L
        Neo4jStoreSingleton.consume(msisdn = MSISDN, usedBytes = 0, requestedBytes = dataBucketSize) { storeResult ->
            storeResult.fold(
                    { fail(it.message) },
                    {
                        assertEquals(dataBucketSize, it.granted) // reserved = 40_000_000
                        assertEquals(60_000_000L, it.balance) // balance = 60_000_000
                    })
        }
        // used = 50_000_000
        // requested = 40_000_000
        Neo4jStoreSingleton.consume(msisdn = MSISDN, usedBytes = 50_000_000L, requestedBytes = dataBucketSize) { storeResult ->
            storeResult.fold(
                    { fail(it.message) },
                    {
                        assertEquals(dataBucketSize, it.granted) // reserved = 40_000_000
                        assertEquals(10_000_000L, it.balance) // balance = 10_000_000
                    })
        }

        // used = 30_000_000
        // requested = 40_000_000
        Neo4jStoreSingleton.consume(msisdn = MSISDN, usedBytes = 30_000_000L, requestedBytes = dataBucketSize) { storeResult ->
            storeResult.fold(
                    { fail(it.message) },
                    {
                        assertEquals(20_000_000L, it.granted) // reserved = 20_000_000
                        assertEquals(0L, it.balance) // balance = 0
                    })
        }
    }

    @Test
    fun `set and get Purchase record`() {
        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        val product = createProduct("1GB_249NOK")
        val now = Instant.now().toEpochMilli()

        // prep
        job {
            create { product }
        }.mapLeft { fail(it.message) }

        // test
        val purchaseRecord = PurchaseRecord(product = product, timestamp = now, id = UUID.randomUUID().toString())
        Neo4jStoreSingleton.addPurchaseRecord(customerId = CUSTOMER.id, purchase = purchaseRecord).bimap(
                { fail(it.message) },
                { assertNotNull(it) }
        )

        Neo4jStoreSingleton.getPurchaseRecords(IDENTITY).bimap(
                { fail(it.message) },
                { assertTrue(it.contains(purchaseRecord)) }
        )
    }

    @Test
    fun `create products, offer, segment and then get products for a customer`() {
        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        // prep
        job {
            create { createProduct("1GB_249NOK") }
            create { createProduct("2GB_299NOK") }
            create { createProduct("3GB_349NOK") }
            create { createProduct("5GB_399NOK") }
        }.mapLeft { fail(it.message) }

        val segment = org.ostelco.prime.model.Segment(
                id = "NEW_SEGMENT",
                subscribers = listOf(CUSTOMER.id))
        Neo4jStoreSingleton.createSegment(segment)
                .mapLeft { fail(it.message) }

        val offer = Offer(
                id = "NEW_OFFER",
                segments = listOf("NEW_SEGMENT"),
                products = listOf("3GB_349NOK"))
        Neo4jStoreSingleton.createOffer(offer)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getProducts(IDENTITY).bimap(
                { fail(it.message) },
                { products ->
                    assertEquals(1, products.size)
                    assertEquals(createProduct("3GB_349NOK"), products.values.first())
                })

        Neo4jStoreSingleton.getProduct(IDENTITY, "2GB_299NOK").bimap(
                { assertEquals("Product - 2GB_299NOK not found.", it.message) },
                { fail("Expected get product to fail since it is not linked to any subscriber --> segment --> offer") })
    }

    @Test
    fun `import offer + product + segment`() {

        // existing products
        job {
            create { createProduct("1GB_249NOK") }
            create { createProduct("2GB_299NOK") }
        }.mapLeft { fail(it.message) }

        val products = listOf(
                createProduct("3GB_349NOK"),
                createProduct("5GB_399NOK"))

        val segments = listOf(
                org.ostelco.prime.model.Segment(id = "segment_1"),
                org.ostelco.prime.model.Segment(id = "segment_2")
        )

        val offer = Offer(id = "some_offer", products = listOf("1GB_249NOK", "2GB_299NOK"))

        Neo4jStoreSingleton.atomicCreateOffer(offer = offer, products = products, segments = segments)
                .mapLeft { fail(it.message) }
    }

    @Test
    fun `failed on import duplicate offer`() {

        // existing products
        job {
            create { createProduct("1GB_249NOK") }
            create { createProduct("2GB_299NOK") }
        }.mapLeft { fail(it.message) }

        // new products in the offer
        val products = listOf(
                createProduct("3GB_349NOK"),
                createProduct("5GB_399NOK"))

        // new segment in the offer
        val segments = listOf(
                org.ostelco.prime.model.Segment(id = "segment_1"),
                org.ostelco.prime.model.Segment(id = "segment_2")
        )

        val offer = Offer(id = "some_offer", products = listOf("1GB_249NOK", "2GB_299NOK"))

        Neo4jStoreSingleton.atomicCreateOffer(offer = offer, products = products, segments = segments)
                .mapLeft { fail(it.message) }

        val duplicateOffer = Offer(
                id = offer.id,
                products = (products.map { it.sku } + offer.products).toSet(),
                segments = segments.map { it.id })

        Neo4jStoreSingleton.atomicCreateOffer(offer = duplicateOffer).bimap(
                { assertEquals("Offer - some_offer already exists.", it.message) },
                { fail("Expected import to fail since offer already exists.") })
    }

    @Test
    fun `eKYCScan - generate new scanId`() {

        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        // test
        Neo4jStoreSingleton.createNewJumioKycScanId(identity = IDENTITY, regionCode = REGION_CODE).map {
            Neo4jStoreSingleton.getScanInformation(identity = IDENTITY, scanId = it.scanId).mapLeft {
                fail(it.message)
            }
        }.mapLeft {
            fail(it.message)
        }
    }

    @Test
    fun `eKYCScan - get all scans`() {

        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        // test
        Neo4jStoreSingleton.createNewJumioKycScanId(identity = IDENTITY, regionCode = REGION_CODE).map { newScan ->
            Neo4jStoreSingleton.getAllScanInformation(identity = IDENTITY).map { infoList ->
                assertEquals(1, infoList.size, "More scans than expected.")
                assertEquals(newScan.scanId, infoList.elementAt(0).scanId, "Wrong scan returned.")
            }.mapLeft {
                fail(it.message)
            }
        }.mapLeft {
            fail(it.message)
        }
    }

    @Test
    fun `eKYCScan - update scan information`() {

        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        Neo4jStoreSingleton.createNewJumioKycScanId(identity = IDENTITY, regionCode = REGION_CODE).map {
            val newScanInformation = ScanInformation(
                    scanId = it.scanId,
                    countryCode = REGION,
                    status = ScanStatus.APPROVED,
                    scanResult = ScanResult(
                            vendorScanReference = UUID.randomUUID().toString(),
                            time = 100,
                            verificationStatus = "APPROVED",
                            type = "ID",
                            country = "NOR",
                            firstName = "Test User",
                            lastName = "Family",
                            dob = "1980/10/10",
                            rejectReason = null
                    )
            )
            val vendorData: MultivaluedMap<String, String> = MultivaluedHashMap<String, String>()
            val scanId = "id1"
            val imgUrl = "https://www.gstatic.com/webp/gallery3/1.png"
            val imgUrl2 = "https://www.gstatic.com/webp/gallery3/2.png"
            vendorData.add(JumioScanData.SCAN_ID.s, scanId)
            vendorData.add(JumioScanData.SCAN_IMAGE.s, imgUrl)
            vendorData.add(JumioScanData.SCAN_IMAGE_BACKSIDE.s, imgUrl2)

            Mockito.`when`(mockScanInformationStore.upsertVendorScanInformation(customerId = CUSTOMER.id, countryCode = REGION, vendorData = vendorData))
                    .thenReturn(Unit.right())

            Neo4jStoreSingleton.updateScanInformation(newScanInformation, vendorData).mapLeft {
                fail(it.message)
            }
        }.mapLeft {
            fail(it.message)
        }
    }

    @Test
    fun `eKYCScan - update with unknown scanId`() {

        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        // test
        Neo4jStoreSingleton.createNewJumioKycScanId(identity = IDENTITY, regionCode = REGION_CODE).map {
            val newScanInformation = ScanInformation(
                    scanId = "fakeId",
                    countryCode = REGION,
                    status = ScanStatus.APPROVED,
                    scanResult = ScanResult(
                            vendorScanReference = UUID.randomUUID().toString(),
                            time = 100,
                            verificationStatus = "APPROVED",
                            type = "ID",
                            country = "NOR",
                            firstName = "Test User",
                            lastName = "Family",
                            dob = "1980/10/10",
                            rejectReason = null
                    )
            )
            val vendorData: MultivaluedMap<String, String> = MultivaluedHashMap<String, String>()
            val scanId = "id1"
            val imgUrl = "https://www.gstatic.com/webp/gallery3/1.png"
            val imgUrl2 = "https://www.gstatic.com/webp/gallery3/2.png"
            vendorData.add(JumioScanData.SCAN_ID.s, scanId)
            vendorData.add(JumioScanData.SCAN_IMAGE.s, imgUrl)
            vendorData.add(JumioScanData.SCAN_IMAGE_BACKSIDE.s, imgUrl2)
            Neo4jStoreSingleton.updateScanInformation(newScanInformation, vendorData).bimap(
                    { assertEquals("ScanInformation - fakeId not found.", it.message) },
                    { fail("Expected to fail since scanId is fake.") })
        }.mapLeft {
            fail(it.message)
        }
    }

    @Test
    fun `eKYCScan - illegal access`() {

        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        val fakeEmail = "fake-$EMAIL"
        val fakeIdentity = Identity(id = fakeEmail, type = "EMAIL", provider = "email")
        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())
        assert(Neo4jStoreSingleton.addCustomer(
                identity = fakeIdentity,
                customer = Customer(contactEmail = fakeEmail, nickname = NAME)).isRight())

        // test
        Neo4jStoreSingleton.createNewJumioKycScanId(fakeIdentity, REGION_CODE).mapLeft {
            fail(it.message)
        }
        Neo4jStoreSingleton.createNewJumioKycScanId(identity = IDENTITY, regionCode = REGION_CODE).map {
            Neo4jStoreSingleton.getScanInformation(fakeIdentity, scanId = it.scanId).bimap(
                    { assertEquals("Not allowed", it.message) },
                    { fail("Expected to fail since the requested subscriber is wrong.") })
        }.mapLeft {
            fail(it.message)
        }
    }

    @Test
    fun `test provision and get SIM profile`() {

        // prep
        `when`(mockEmailNotifier.sendESimQrCodeEmail(email = CUSTOMER.contactEmail, name = CUSTOMER.nickname, qrCode = "eSimActivationCode"))
                .thenReturn(Unit.right())

        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = APPROVED,
                regionCode = REGION_CODE).isRight())

        Mockito.`when`(mockSimManager.allocateNextEsimProfile("Loltel", "default"))
                .thenReturn(SimEntry(iccId = "iccId", eSimActivationCode = "eSimActivationCode", msisdnList = emptyList(), status = AVAILABLE_FOR_DOWNLOAD).right())

        Mockito.`when`(mockSimManager.getSimProfile("Loltel", "iccId"))
                .thenReturn(SimEntry(iccId = "iccId", eSimActivationCode = "eSimActivationCode", msisdnList = emptyList(), status = AVAILABLE_FOR_DOWNLOAD).right())

        // test
        Neo4jStoreSingleton.provisionSimProfile(
                identity = IDENTITY,
                regionCode = REGION_CODE,
                profileType = "default")
                .bimap(
                        { fail(it.message) },
                        {
                            assertEquals(
                                    expected = SimProfile(
                                            iccId = "iccId",
                                            eSimActivationCode = "eSimActivationCode",
                                            status = AVAILABLE_FOR_DOWNLOAD),
                                    actual = it)
                        })

        Neo4jStoreSingleton.getSimProfiles(
                identity = IDENTITY,
                regionCode = REGION_CODE)
                .bimap(
                        { fail(it.message) },
                        {
                            assertEquals(
                                    expected = listOf(SimProfile(
                                            iccId = "iccId",
                                            eSimActivationCode = "eSimActivationCode",
                                            status = AVAILABLE_FOR_DOWNLOAD)),
                                    actual = it)
                        })
    }

    @Test
    fun `test getAllRegionDetails with no region`() {
        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        // test
        Neo4jStoreSingleton.getAllRegionDetails(identity = IDENTITY)
                .bimap(
                        { fail("Failed to fetch regions list") },
                        {
                            for (region in it) {
                                assert(region.status == AVAILABLE) { "All regions should be marked available" }
                            }
                        })
    }

    @Test
    fun `test getRegionDetails with no region`() {
        // prep
        job {
            create { Region(REGION_CODE, "Norway") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        // test
        Neo4jStoreSingleton.getRegionDetails(identity = IDENTITY, regionCode = REGION_CODE)
                .bimap(
                        {
                            assert(it is NotFoundError)
                            assertEquals(expected = "BELONG_TO_REGION", actual = it.type)
                            assertTrue { it.id.endsWith(" -> no") }
                        },
                        { fail("Should fail with not found error") })
    }

    @Test
    fun `test getAllRegionDetails with region without sim profile`() {
        // prep
        job {
            create { Region("no", "Norway") }
            create { Region("sg", "Singapore") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = APPROVED,
                regionCode = "no").isRight())
        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = PENDING,
                regionCode = "sg").isRight())

        // test
        Neo4jStoreSingleton.getAllRegionDetails(identity = IDENTITY)
                .bimap(
                        { fail("Failed to fetch regions list") },
                        {
                            assertEquals(
                                    expected = setOf(
                                            RegionDetails(
                                                    region = Region("no", "Norway"),
                                                    kycStatusMap = mapOf(JUMIO to KycStatus.PENDING),
                                                    status = APPROVED),
                                            RegionDetails(
                                                    region = Region("sg", "Singapore"),
                                                    kycStatusMap = mapOf(
                                                            JUMIO to KycStatus.PENDING,
                                                            MY_INFO to KycStatus.PENDING,
                                                            ADDRESS to KycStatus.PENDING,
                                                            NRIC_FIN to KycStatus.PENDING),
                                                    status = PENDING)),
                                    actual = it.toSet())
                        })
    }

    @Test
    fun `test getRegionDetails with region without sim profile`() {
        // prep
        job {
            create { Region("no", "Norway") }
            create { Region("sg", "Singapore") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = APPROVED,
                regionCode = "no").isRight())
        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = PENDING,
                regionCode = "sg").isRight())

        // test
        Neo4jStoreSingleton.getRegionDetails(identity = IDENTITY, regionCode = REGION_CODE)
                .bimap(
                        { fail("Failed to fetch regions list") },
                        {
                            assertEquals(
                                    expected = RegionDetails(
                                            region = Region(REGION_CODE, "Norway"),
                                            status = APPROVED,
                                            kycStatusMap = mapOf(JUMIO to KycStatus.PENDING)),
                                    actual = it)
                        })
    }

    @Test
    fun `test getAllRegionDetails with region with sim profiles`() {

        // prep
        `when`(mockEmailNotifier.sendESimQrCodeEmail(email = CUSTOMER.contactEmail, name = CUSTOMER.nickname, qrCode = "eSimActivationCode"))
                .thenReturn(Unit.right())

        job {
            create { Region("no", "Norway") }
            create { Region("sg", "Singapore") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = APPROVED,
                regionCode = "no").isRight())
        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = PENDING,
                regionCode = "sg").isRight())

        Mockito.`when`(mockSimManager.allocateNextEsimProfile("Loltel", "default"))
                .thenReturn(SimEntry(iccId = "iccId", eSimActivationCode = "eSimActivationCode", msisdnList = emptyList(), status = AVAILABLE_FOR_DOWNLOAD).right())

        Mockito.`when`(mockSimManager.getSimProfile("Loltel", "iccId"))
                .thenReturn(SimEntry(iccId = "iccId", eSimActivationCode = "eSimActivationCode", msisdnList = emptyList(), status = AVAILABLE_FOR_DOWNLOAD).right())

        assert(Neo4jStoreSingleton.provisionSimProfile(
                identity = IDENTITY,
                regionCode = REGION_CODE,
                profileType = "default").isRight())

        // test
        Neo4jStoreSingleton.getAllRegionDetails(identity = IDENTITY)
                .bimap(
                        { fail("Failed to fetch regions list") },
                        {
                            assertEquals(
                                    expected = setOf(
                                            RegionDetails(
                                                    region = Region("no", "Norway"),
                                                    status = APPROVED,
                                                    kycStatusMap = mapOf(JUMIO to KycStatus.PENDING),
                                                    simProfiles = listOf(
                                                            SimProfile(
                                                                    iccId = "iccId",
                                                                    eSimActivationCode = "eSimActivationCode",
                                                                    status = AVAILABLE_FOR_DOWNLOAD))),
                                            RegionDetails(
                                                    region = Region("sg", "Singapore"),
                                                    kycStatusMap = mapOf(
                                                            JUMIO to KycStatus.PENDING,
                                                            MY_INFO to KycStatus.PENDING,
                                                            ADDRESS to KycStatus.PENDING,
                                                            NRIC_FIN to KycStatus.PENDING),
                                                    status = PENDING)),
                                    actual = it.toSet())
                        })
    }

    @Test
    fun `test getRegionDetails with region with sim profiles`() {

        // prep
        `when`(mockEmailNotifier.sendESimQrCodeEmail(email = CUSTOMER.contactEmail, name = CUSTOMER.nickname, qrCode = "eSimActivationCode"))
                .thenReturn(Unit.right())

        job {
            create { Region("no", "Norway") }
            create { Region("sg", "Singapore") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = APPROVED,
                regionCode = "no").isRight())
        assert(Neo4jStoreSingleton.createCustomerRegionSetting(
                customer = CUSTOMER,
                status = PENDING,
                regionCode = "sg").isRight())

        Mockito.`when`(mockSimManager.allocateNextEsimProfile("Loltel", "default"))
                .thenReturn(SimEntry(iccId = "iccId", eSimActivationCode = "eSimActivationCode", msisdnList = emptyList(), status = AVAILABLE_FOR_DOWNLOAD).right())

        Mockito.`when`(mockSimManager.getSimProfile("Loltel", "iccId"))
                .thenReturn(SimEntry(iccId = "iccId", eSimActivationCode = "eSimActivationCode", msisdnList = emptyList(), status = AVAILABLE_FOR_DOWNLOAD).right())

        assert(Neo4jStoreSingleton.provisionSimProfile(
                identity = IDENTITY,
                regionCode = REGION_CODE,
                profileType = "default").isRight())

        // test
        Neo4jStoreSingleton.getRegionDetails(identity = IDENTITY, regionCode = REGION_CODE)
                .bimap(
                        { fail("Failed to fetch regions list") },
                        {
                            assertEquals(
                                    expected = RegionDetails(
                                            region = Region(REGION_CODE, "Norway"),
                                            status = APPROVED,
                                            kycStatusMap = mapOf(JUMIO to KycStatus.PENDING),
                                            simProfiles = listOf(
                                                    SimProfile(
                                                            iccId = "iccId",
                                                            eSimActivationCode = "eSimActivationCode",
                                                            status = AVAILABLE_FOR_DOWNLOAD))),
                                    actual = it)
                        })
    }

    @Test
    fun `test MY_INFO status`() {

        job {
            create { Region("sg", "Singapore") }
        }.mapLeft { fail(it.message) }

        Neo4jStoreSingleton.createSegment(org.ostelco.prime.model.Segment(id = "country-sg"))
                .mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        Neo4jStoreSingleton.getRegionDetails(
                identity = IDENTITY,
                regionCode = "sg")
                .map {
                    fail("Should not have region details")
                }

        Neo4jStoreSingleton.setKycStatus(
                customer = CUSTOMER,
                regionCode = "sg",
                kycType = MY_INFO)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getRegionDetails(
                identity = IDENTITY,
                regionCode = "sg")
                .fold({
                    fail("Failed to get Region Details - ${it.message}")
                }, {
                    assertEquals(PENDING, it.status)
                })

        Neo4jStoreSingleton.setKycStatus(
                customer = CUSTOMER,
                regionCode = "sg",
                kycType = ADDRESS)
                .mapLeft { fail(it.message) }

        Neo4jStoreSingleton.getRegionDetails(
                identity = IDENTITY,
                regionCode = "sg")
                .fold({
                    fail("Failed to get Region Details - ${it.message}")
                }, {
                    assertEquals(APPROVED, it.status)
                })
    }

    @Test
    fun `test NRIC_FIN JUMIO and ADDRESS_PHONE status`() {

        Neo4jStoreSingleton.createSegment(org.ostelco.prime.model.Segment(id = "country-sg"))

        job {
            create { Region("sg", "Singapore") }
        }.mapLeft { fail(it.message) }

        assert(Neo4jStoreSingleton.addCustomer(
                identity = IDENTITY,
                customer = CUSTOMER).isRight())

        Neo4jStoreSingleton.getRegionDetails(
                identity = IDENTITY,
                regionCode = "sg")
                .map {
                    fail("Should not have region details")
                }

        Neo4jStoreSingleton.setKycStatus(
                customer = CUSTOMER,
                regionCode = "sg",
                kycType = NRIC_FIN)
                .mapLeft {
                    fail(it.message)
                }

        Neo4jStoreSingleton.getRegionDetails(
                identity = IDENTITY,
                regionCode = "sg")
                .fold({
                    fail("Failed to get Region Details")
                }, {
                    assertEquals(PENDING, it.status)
                })

        Neo4jStoreSingleton.setKycStatus(
                customer = CUSTOMER,
                regionCode = "sg",
                kycType = JUMIO)
                .mapLeft {
                    fail(it.message)
                }

        Neo4jStoreSingleton.getRegionDetails(
                identity = IDENTITY,
                regionCode = "sg")
                .fold({
                    fail("Failed to get Region Details")
                }, {
                    assertEquals(PENDING, it.status)
                })

        Neo4jStoreSingleton.setKycStatus(
                customer = CUSTOMER,
                regionCode = "sg",
                kycType = ADDRESS)
                .mapLeft {
                    fail(it.message)
                }

        Neo4jStoreSingleton.getRegionDetails(
                identity = IDENTITY,
                regionCode = "sg")
                .fold({
                    fail("Failed to get Region Details")
                }, {
                    assertEquals(APPROVED, it.status)
                })
    }

    companion object {
        const val EMAIL = "foo@bar.com"
        const val NAME = "Test User"
        const val REGION = "NO"
        const val REGION_CODE = "no"
        const val MSISDN = "4712345678"
        const val ICC_ID = "ICC_ID"
        const val ALIAS = "default"
        val IDENTITY = Identity(id = EMAIL, type = "EMAIL", provider = "email")
        val CUSTOMER = Customer(contactEmail = EMAIL, nickname = NAME)

        @ClassRule
        @JvmField
        var docker: DockerComposeRule = DockerComposeRule.builder()
                .file("src/test/resources/docker-compose.yaml")
                .waitingForService("neo4j", HealthChecks.toHaveAllPortsOpen())
                .waitingForService("neo4j",
                        HealthChecks.toRespond2xxOverHttp(7474) { port ->
                            port.inFormat("http://\$HOST:\$EXTERNAL_PORT/browser")
                        },
                        Duration.standardSeconds(40L))
                .build()

        @BeforeClass
        @JvmStatic
        fun start() {
            ConfigRegistry.config = Config(
                    host = "0.0.0.0",
                    protocol = "bolt",
                    onNewCustomerAction = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.OnNewCustomerAction",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/OnNewCustomerAction.kts"
                            )
                    ),
                    allowedRegionsService = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.AllowedRegionsService",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/AllowedRegionsService.kts"
                            )
                    ),
                    onRegionApprovedAction = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.OnRegionApprovedAction",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/OnRegionApprovedAction.kts"
                            )
                    ),
                    hssNameLookupService = KtsServiceFactory(
                            serviceInterface = "org.ostelco.prime.storage.graph.HssNameLookupService",
                            textReader = ClasspathResourceTextReader(
                                    filename = "/HssNameLookupService.kts"
                            )
                    )
            )
            Neo4jClient.start()
        }

        @AfterClass
        @JvmStatic
        fun stop() {
            Neo4jClient.stop()
        }
    }
}