package org.ostelco.prime.ocs

import arrow.core.Either
import arrow.core.right
import org.mockito.Mockito
import org.ostelco.prime.storage.ConsumptionResult
import org.ostelco.prime.storage.DocumentStore
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.StoreError

private val mockDocumentStore = Mockito.mock(DocumentStore::class.java)

class MockDocumentStore : DocumentStore by mockDocumentStore

val mockGraphStore: GraphStore = Mockito.mock(GraphStore::class.java)

class MockGraphStore : GraphStore by mockGraphStore {
    override suspend fun consume(msisdn: String, usedBytes: Long, requestedBytes: Long, callback: (Either<StoreError, ConsumptionResult>) -> Unit) {
        callback(ConsumptionResult(msisdnAnalyticsId = "", granted = 100L, balance = 200L).right())
    }
}
