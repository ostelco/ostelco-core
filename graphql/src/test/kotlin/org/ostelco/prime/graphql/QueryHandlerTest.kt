package org.ostelco.prime.graphql

import org.junit.Assert.assertEquals
import org.junit.Test
import org.ostelco.prime.model.Identity
import java.io.File

class QueryHandlerTest {

    private val queryHandler = QueryHandler(File("src/test/resources/customer.graphqls"))

    private val email = "foo@test.com"

    private fun execute(query: String): Map<String, Any> = queryHandler
            .execute(identity = Identity(id = email, type = "EMAIL", provider = "email"), query = query)
            .getData<Map<String, Any>>()

    @Test
    fun `test get profile`() {
        val result = execute("""{ context(id: "invalid@test.com") { customer { nickname, contactEmail } } }""".trimIndent())
        assertEquals("{context={customer={nickname=foo, contactEmail=$email}}}", "$result")
    }

    @Test
    fun `test get bundles and products`() {
        val result = execute("""{ context(id: "invalid@test.com") { bundles { id, balance } products { sku, price { amount, currency } } } }""".trimIndent())
        assertEquals("{context={bundles=[{id=$email, balance=1000000000}], products=[{sku=SKU, price={amount=10000, currency=NOK}}]}}", "$result")
    }

    @Test
    fun `test get subscriptions`() {
        val result = execute("""{ context(id: "invalid@test.com") { subscriptions { msisdn } } }""".trimIndent())
        assertEquals("{context={subscriptions=[{msisdn=4790300123}]}}", "$result")
    }

    @Test
    fun `test get purchase history`() {
        val result = execute("""{ context(id: "invalid@test.com") { purchases { id, product { sku, price { amount, currency } } } } }""".trimIndent())
        assertEquals("{context={purchases=[{id=PID, product={sku=SKU, price={amount=10000, currency=NOK}}}]}}", "$result")
    }
}