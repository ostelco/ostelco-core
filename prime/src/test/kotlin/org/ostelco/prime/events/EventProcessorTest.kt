package org.ostelco.prime.events

import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventMessageType.GET_DATA_BUNDLE_BALANCE
import org.ostelco.prime.disruptor.PrimeEventMessageType.RELEASE_RESERVED_BUCKET
import org.ostelco.prime.storage.Products.DATA_TOPUP_3GB
import org.ostelco.prime.storage.PurchaseRequestListener
import org.ostelco.prime.storage.Storage
import org.ostelco.prime.storage.StorageException
import org.ostelco.prime.storage.entities.NotATopupProductException
import org.ostelco.prime.storage.entities.Product
import org.ostelco.prime.storage.entities.PurchaseRequest

class EventProcessorTest {

    @Rule
    @JvmField
    var mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    lateinit var storage: Storage

    @Mock
    lateinit var ocsBalanceUpdater: OcsBalanceUpdater

    private lateinit var processor: EventProcessor

    @Before
    fun setUp() {
        `when`(storage.isValidSKU(DATA_TOPUP_3GB.sku)).thenReturn(true)

        `when`<Product>(storage.getProductForSku(DATA_TOPUP_3GB.sku)).thenReturn(DATA_TOPUP_3GB)

        this.processor = EventProcessor(storage, ocsBalanceUpdater)
        this.processor.start()
    }

    private class DummyPurchaseRequest : PurchaseRequest {

        override val sku: String
            get() = DATA_TOPUP_3GB.sku

        override val paymentToken: String
            get() = PAYMENT_TOKEN

        override val msisdn: String
            get() = MSISDN

        override val millisSinceEpoch: Long
            get() = 0

        override val id: String
            get() = "Sir Tristram, violer d'amores"

        override fun asMap(): Map<String, Any> {
            return emptyMap()
        }
    }

    // FIXME
    @Ignore
    @Test
    @Throws(EventProcessorException::class, StorageException::class)
    fun handlePurchaseRequestTest() {

        val req = DummyPurchaseRequest()

        // Process a little
        processor.handlePurchaseRequest(req)

        // Then verify that the appropriate actions has been performed.
        val topupBytes: Long
        try {
            topupBytes = DATA_TOPUP_3GB.asTopupProduct()!!.noOfBytes
        } catch (ex: NotATopupProductException) {
            throw EventProcessorException("Programming error, this shouldn't happen", ex)
        }

        verify<Storage>(storage).addPurchaseRequestListener(any(PurchaseRequestListener::class.java))
        verify<Storage>(storage).addRecordOfPurchaseByMsisdn(eq(MSISDN), eq(req.sku), anyLong())
        verify<Storage>(storage).updateDisplayDatastructure(eq(MSISDN))
        verify<Storage>(storage).removePurchaseRequestById(eq(req.id))
        verify<OcsBalanceUpdater>(ocsBalanceUpdater).updateBalance(eq(MSISDN), eq(topupBytes))
    }

    @Test
    @Throws(Exception::class)
    fun testPrimeEventReleaseReservedDataBucket() {
        val noOfBytes = 4711L
        val primeEvent = PrimeEvent()
        primeEvent.messageType = RELEASE_RESERVED_BUCKET
        primeEvent.msisdn = MSISDN
        primeEvent.bundleBytes = noOfBytes

        processor.onEvent(primeEvent, 0L, false)

        verify<Storage>(storage).setRemainingByMsisdn(eq(
                PLUS_USED_TO_BEGIN_INTERNATIONAL_PREFIX_IN_MSISSDN + MSISDN), eq(noOfBytes))
    }

    @Test
    @Throws(StorageException::class)
    fun testPrimeEventGetDataBundleBalance() {
        val primeEvent = PrimeEvent()
        primeEvent.messageType = GET_DATA_BUNDLE_BALANCE
        primeEvent.msisdn = MSISDN
        primeEvent.bundleBytes = NO_OF_BYTES

        processor.onEvent(primeEvent, 0L, false)

        // Verify a little.
        val inernationalMsisdn = PLUS_USED_TO_BEGIN_INTERNATIONAL_PREFIX_IN_MSISSDN + MSISDN
        verify<Storage>(storage).setRemainingByMsisdn(eq(inernationalMsisdn),
                eq(NO_OF_BYTES))
    }

    companion object {

        const val PAYMENT_TOKEN = "a weird token"

        private const val MSISDN = "12345678"

        private const val PLUS_USED_TO_BEGIN_INTERNATIONAL_PREFIX_IN_MSISSDN = "+"

        private const val NO_OF_BYTES = 4711L
    }

    // XXX Are we missing an event type here?
}
