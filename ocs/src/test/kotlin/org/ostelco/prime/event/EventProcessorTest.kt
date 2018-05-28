package org.ostelco.prime.event

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventMessageType.GET_DATA_BUNDLE_BALANCE
import org.ostelco.prime.disruptor.PrimeEventMessageType.RELEASE_RESERVED_BUCKET
import org.ostelco.prime.events.EventProcessor
import org.ostelco.prime.model.Product
import org.ostelco.prime.storage.legacy.Products
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.prime.storage.legacy.StorageException

class EventProcessorTest  {

    private val MSISDN = "12345678"
    private val NO_OF_BYTES = 4711L

    @Rule
    @JvmField
    var mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    lateinit var storage: Storage

    private lateinit var processor: EventProcessor

    @Before
    fun setUp() {

        Mockito.`when`<Product>(storage.getProduct(Products.DATA_TOPUP_3GB.sku)).thenReturn(Products.DATA_TOPUP_3GB)

        this.processor = EventProcessor(storage)
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

        Mockito.verify<Storage>(storage).setBalance(safeEq(MSISDN), safeEq(noOfBytes))
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
        Mockito.verify<Storage>(storage).setBalance(safeEq(MSISDN), safeEq(NO_OF_BYTES))
    }

    // https://github.com/mockito/mockito/issues/1255
    fun <T : Any> safeEq(value: T): T = ArgumentMatchers.eq(value) ?: value
}
