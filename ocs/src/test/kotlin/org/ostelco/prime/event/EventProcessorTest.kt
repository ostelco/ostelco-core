package org.ostelco.prime.event

import arrow.core.Either
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.ostelco.prime.disruptor.BundleBalanceStore
import org.ostelco.prime.disruptor.EventMessageType.RELEASE_RESERVED_BUCKET
import org.ostelco.prime.disruptor.OcsEvent
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Product
import org.ostelco.prime.storage.ClientDataSource
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.legacy.Products

class EventProcessorTest {

    private val EMAIL = "foo@bar.com"
    private val NO_OF_BYTES = 4711L

    @Rule
    @JvmField
    var mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    lateinit var storage: ClientDataSource

    private lateinit var processor: BundleBalanceStore

    @Before
    fun setUp() {

        Mockito.`when`<Either<StoreError, Product>>(storage
                .getProduct(
                        Identity(id = EMAIL, type = "EMAIL", provider = "email"),
                        Products.DATA_TOPUP_3GB.sku))
                .thenReturn(Either.right(Products.DATA_TOPUP_3GB))

        this.processor = BundleBalanceStore(storage)
    }

    @Test
    fun testPrimeEventReleaseReservedDataBucket() {
        val primeEvent = OcsEvent()
        primeEvent.messageType = RELEASE_RESERVED_BUCKET
        primeEvent.bundleId = EMAIL
        primeEvent.bundleBytes = NO_OF_BYTES

        processor.onEvent(primeEvent, 0L, false)

        Mockito.verify<ClientDataSource>(storage).updateBundle(safeEq(Bundle(EMAIL, NO_OF_BYTES)))
    }

    // https://github.com/mockito/mockito/issues/1255
    private fun <T : Any> safeEq(value: T): T = ArgumentMatchers.eq(value) ?: value
}
