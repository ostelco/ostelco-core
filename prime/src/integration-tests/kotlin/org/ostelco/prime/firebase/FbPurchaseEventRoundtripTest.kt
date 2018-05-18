package org.ostelco.prime.firebase

import org.junit.After
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.ostelco.prime.events.EventProcessor
import org.ostelco.prime.events.EventProcessorException
import org.ostelco.prime.events.EventProcessorTest
import org.ostelco.prime.events.OcsBalanceUpdater
import org.ostelco.prime.events.asTopupProduct
import org.ostelco.prime.model.PurchaseRequest
import org.ostelco.prime.storage.firebase.FbStorage
import org.ostelco.prime.storage.firebase.ProductDescriptionCacheImpl
import org.ostelco.prime.storage.legacy.Products.DATA_TOPUP_3GB
import org.ostelco.prime.storage.legacy.PurchaseRequestHandler
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.prime.storage.legacy.StorageException
import org.ostelco.prime.storage.legacy.entities.NotATopupProductException
import java.lang.Thread.sleep
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FbPurchaseEventRoundtripTest {

    @get:Rule
    var mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    var ocsBalanceUpdater: OcsBalanceUpdater? = null

    private var prids: MutableCollection<String>? = null

    private var fbStorage: FbStorage? = null

    private var storage: Storage? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        this.fbStorage = FbStorage(
                "pantel-tests",
                "src/integration-tests/resources/pantel-tests.json")
        this.storage = fbStorage
        val millisToSleepDuringStartup = 3000
        sleep(millisToSleepDuringStartup.toLong())
        storage!!.removeSubscriberByMsisdn(EPHERMERAL_MSISDN)
        storage!!.insertNewSubscriber(EPHERMERAL_MSISDN)

        val processor = EventProcessor(storage!!, ocsBalanceUpdater!!)
        processor.start()
        this.prids = ArrayList()
    }

    @After
    @Throws(StorageException::class)
    fun cleanUp() {
        if (storage != null) {
            storage!!.removeSubscriberByMsisdn(EPHERMERAL_MSISDN)
        }

        if (this.prids != null) {
            for (prid in this.prids!!) {
                fbStorage!!.removePurchaseRequestById(prid)
            }
        }
    }

    @Test
    @Throws(StorageException::class)
    fun insertNewSubscriberTest() {
        Assert.assertNotEquals(null, storage!!.getSubscriberFromMsisdn(EPHERMERAL_MSISDN))
    }

    @Test
    @Throws(EventProcessorException::class, StorageException::class, InterruptedException::class, NotATopupProductException::class)
    fun purchaseRequestRoundtripTest() {

        Assert.assertNotEquals(null, storage!!.getSubscriberFromMsisdn(EPHERMERAL_MSISDN))

        val latch = CountDownLatch(1)

        storage!!.addPurchaseRequestHandler(object : PurchaseRequestHandler {
            override fun onPurchaseRequest(request: PurchaseRequest) {
                latch.countDown()
            }
        })

        val req = PurchaseRequest(
                sku = DATA_TOPUP_3GB.sku,
                paymentToken = EventProcessorTest.PAYMENT_TOKEN,
                msisdn = EPHERMERAL_MSISDN,
                id = EPHERMERAL_MSISDN,
                millisSinceEpoch = Instant.now().toEpochMilli())

        Assert.assertNotEquals(null, storage!!.getSubscriberFromMsisdn(EPHERMERAL_MSISDN))

        val prid = storage!!.injectPurchaseRequest(req)
        prids!!.add(prid)
        sleep(MINIMUM_MILLIS_TO_SLEEP_AFTER_MAKING_PURCHASE_REQUEST.toLong())

        Assert.assertNotEquals(null, storage!!.getSubscriberFromMsisdn(EPHERMERAL_MSISDN))

        if (!latch.await(SECONDS_TO_WAIT_FOR_SUBSCRIPTION_PROCESSING_TO_FINISH.toLong(), TimeUnit.SECONDS)) {
            fail("Read/react failed")
        }

        val topupBytes = ProductDescriptionCacheImpl.DATA_TOPUP_3GB.asTopupProduct()!!.noOfBytes

        // Then verify
        verify<OcsBalanceUpdater>(ocsBalanceUpdater, times(2)).updateBalance(safeEq(EPHERMERAL_MSISDN), safeEq(topupBytes))

        // XXX Verification of data stored in firebase not verified.
    }

    // https://github.com/mockito/mockito/issues/1255
    fun <T : Any> safeEq(value: T): T = eq(value) ?: value

    companion object {

        private const val EPHERMERAL_MSISDN = "+4747116996"

        private const val MINIMUM_MILLIS_TO_SLEEP_AFTER_MAKING_PURCHASE_REQUEST = 3000

        private const val SECONDS_TO_WAIT_FOR_SUBSCRIPTION_PROCESSING_TO_FINISH = 10
    }
}
