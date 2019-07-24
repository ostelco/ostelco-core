package org.ostelco.prime.store.datastore

import arrow.core.getOrElse
import org.junit.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

data class TestData(
        val id: String,
        @DatastoreExcludeFromIndex
        val name: String,
        val created: Long)


class SchemaTest {

    @Test
    fun `test add and fetch from data store`() {

        val testDataStore = EntityStore(entityClass = TestData::class)

        val testData = TestData(
                id = UUID.randomUUID().toString(),
                name = "Foo",
                created = Instant.now().toEpochMilli())

        val key = testDataStore.add(testData).getOrElse { null }
        assertNotNull(key)

        val fetched = testDataStore.fetch(key = key).getOrElse { null }
        assertNotNull(fetched)
        assertEquals(expected = testData, actual = fetched)
    }

    @Test
    fun `test add and fetch of long strings from data store`() {
        val testDataStore = EntityStore(entityClass = TestData::class)

        val testData = TestData(
                id = UUID.randomUUID().toString(),
                name = "Foo".repeat(1000),
                created = Instant.now().toEpochMilli())

        val key = testDataStore.add(testData).getOrElse { null }
        assertNotNull(key)

        val fetched = testDataStore.fetch(key = key).getOrElse { null }
        assertNotNull(fetched)
        assertEquals(expected = testData, actual = fetched)
    }
}