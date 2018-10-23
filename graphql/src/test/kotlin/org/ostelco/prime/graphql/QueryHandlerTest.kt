package org.ostelco.prime.graphql

import arrow.core.Either
import arrow.core.right
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.storage.DocumentStore
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.StoreError
import java.io.File

class QueryHandlerTest {

    private val queryHandler = QueryHandler(File("src/test/resources/subscriber.graphqls"))

    private fun execute(query: String): Map<String, Any> = queryHandler
            .execute(query)
            .getData<Map<String, Any>>()

    @Test
    fun `test get profile`() {
        val result = execute("""{ subscriber(id: "foo@test.com") { profile { email } } }""".trimIndent())
        assertEquals("{subscriber={profile={email=foo@test.com}}}", "$result")
    }

    @Test
    fun `test get bundles and products`() {
        val result = execute("""{ subscriber(id: "foo@test.com") { bundles { id, balance } products { sku, price { amount, currency } } } }""".trimIndent())
        assertEquals("{subscriber={bundles=[{id=foo@test.com, balance=1000000000}], products=[{sku=SKU, price={amount=10000, currency=NOK}}]}}", "$result")
    }

    @Test
    fun `test get subscriptions`() {
        val result = execute("""{ subscriber(id: "foo@test.com") { subscriptions { msisdn, alias } } }""".trimIndent())
        assertEquals("{subscriber={subscriptions=[{msisdn=4790300123, alias=}]}}", "$result")
    }

    @Test
    fun `test get purchase history`() {
        val result = execute("""{ subscriber(id: "foo@test.com") { purchases { id, product { sku, price { amount, currency } } } } }""".trimIndent())
        assertEquals("{subscriber={purchases=[{id=PID, product={sku=SKU, price={amount=10000, currency=NOK}}}]}}", "$result")
    }
}

class MockGraphStore : GraphStore by mock(GraphStore::class.java) {

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

class MockDocumentStore : DocumentStore by mock(DocumentStore::class.java)