package org.ostelco.prime.store.datastore

import org.junit.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals

data class TestData(
        val id: String,
        val name: String,
        val created: Long)


class SchemaTest {

    @Test
    fun `test save and fetch from data store`() {

        val testDataStore = EntityStore(entityClass = TestData::class.java)

        val testData = TestData(
                id = UUID.randomUUID().toString(),
                name = "Foo",
                created = Instant.now().toEpochMilli())

        val key = testDataStore.add(testData)

        val fetched = testDataStore.fetch(key = key)

        assertEquals(expected = testData, actual = fetched)

    }

}