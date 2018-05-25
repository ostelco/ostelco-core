package org.ostelco.prime.handler

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.ostelco.prime.disruptor.PrimeEventProducer
import org.ostelco.prime.events.EventProcessorException
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.storage.legacy.Products.DATA_TOPUP_3GB
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.prime.storage.legacy.StorageException

class PurchaseRequestHandlerTest {

    @Rule
    @JvmField
    var mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    lateinit var storage: Storage

    @Mock
    lateinit var producer: PrimeEventProducer

    private lateinit var purchaseRequestHandler: PurchaseRequestHandler

    @Before
    fun setUp() {

        `when`<Product>(storage.getProduct(DATA_TOPUP_3GB.sku)).thenReturn(DATA_TOPUP_3GB)

        this.purchaseRequestHandler = PurchaseRequestHandler(producer, storage)
    }

    // FIXME
    @Ignore
    @Test
    @Throws(EventProcessorException::class, StorageException::class)
    fun handlePurchaseRequestTest() {

        val sku = DATA_TOPUP_3GB.sku

        // Process a little
        purchaseRequestHandler.handlePurchaseRequest(MSISDN, sku)

        // Then verify that the appropriate actions has been performed.
        val topupBytes = DATA_TOPUP_3GB.properties["noOfBytes"]?.toLong()
                ?: throw EventProcessorException("Missing property 'noOfBytes' in product sku: $sku")

        val capturedPurchaseRecord = ArgumentCaptor.forClass(PurchaseRecord::class.java)

        assertEquals(MSISDN, capturedPurchaseRecord.value.msisdn)
        assertEquals(sku, capturedPurchaseRecord.value.sku)

        verify<PrimeEventProducer>(producer).topupDataBundleBalanceEvent(MSISDN, topupBytes)
    }

    companion object {

        private const val MSISDN = "12345678"

    }

    // https://github.com/mockito/mockito/issues/1255
    fun <T : Any> safeEq(value: T): T = ArgumentMatchers.eq(value) ?: value
}
