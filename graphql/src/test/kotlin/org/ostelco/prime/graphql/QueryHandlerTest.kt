package org.ostelco.prime.graphql

import org.junit.Assert.assertEquals
import org.junit.Test
import org.ostelco.prime.model.Identity
import java.io.File

class QueryHandlerTest {

    private val queryHandler = QueryHandler(File("src/test/resources/subscriber.graphqls"))

    private val email = "foo@test.com"

    private fun execute(query: String): Map<String, Any> = queryHandler
            .execute(identity = Identity(id = email, type = "EMAIL", provider = "email"), query = query)
            .getData<Map<String, Any>>()

    @Test
    fun `test get profile`() {
        val result = execute("""{ subscriber(id: "invalid@test.com") { profile { email } } }""".trimIndent())
        assertEquals("{subscriber={profile={email=foo@test.com}}}", "$result")
    }

    @Test
    fun `test get bundles and products`() {
        val result = execute("""{ subscriber(id: "invalid@test.com") { bundles { id, balance } products { sku, price { amount, currency } } } }""".trimIndent())
        assertEquals("{subscriber={bundles=[{id=foo@test.com, balance=1000000000}], products=[{sku=SKU, price={amount=10000, currency=NOK}}]}}", "$result")
    }

    @Test
    fun `test get subscriptions`() {
        val result = execute("""{ subscriber(id: "invalid@test.com") { subscriptions { msisdn, alias } } }""".trimIndent())
        assertEquals("{subscriber={subscriptions=[{msisdn=4790300123, alias=}]}}", "$result")
    }

    @Test
    fun `test get purchase history`() {
        val result = execute("""{ subscriber(id: "invalid@test.com") { purchases { id, product { sku, price { amount, currency } } } } }""".trimIndent())
        assertEquals("{subscriber={purchases=[{id=PID, product={sku=SKU, price={amount=10000, currency=NOK}}}]}}", "$result")
    }
}