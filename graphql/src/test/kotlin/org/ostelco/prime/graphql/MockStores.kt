package org.ostelco.prime.graphql

import arrow.core.Either
import arrow.core.right
import org.mockito.Mockito
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.storage.DocumentStore
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.StoreError

class MockGraphStore : GraphStore by Mockito.mock(GraphStore::class.java) {

    private val product = Product(sku = "SKU", price = Price(amount = 10000, currency = "NOK"))

    override fun getSubscriber(subscriberId: String): Either<StoreError, Subscriber> =
            Subscriber(email = subscriberId).right()

    override fun getBundles(subscriberId: String): Either<StoreError, Collection<Bundle>> =
            listOf(Bundle(id = subscriberId, balance = 1000000000L)).right()

    override fun getSubscriptions(subscriberId: String): Either<StoreError, Collection<Subscription>> =
            listOf(Subscription(msisdn = "4790300123")).right()

    override fun getProducts(subscriberId: String): Either<StoreError, Map<String, Product>> =
            mapOf("SKU" to product).right()

    override fun getPurchaseRecords(subscriberId: String): Either<StoreError, Collection<PurchaseRecord>> =
            listOf(PurchaseRecord(id = "PID", product = product, timestamp = 1234L, msisdn = "")).right()

}

class MockDocumentStore : DocumentStore by Mockito.mock(DocumentStore::class.java)