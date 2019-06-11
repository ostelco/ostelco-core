package org.ostelco.prime.admin

import arrow.core.Either
import arrow.core.right
import org.mockito.Mockito
import org.ostelco.prime.storage.DocumentStore
import org.ostelco.prime.storage.GraphStore
import org.ostelco.prime.storage.StoreError

private val mockDocumentStore = Mockito.mock(DocumentStore::class.java)

class MockDocumentStore : DocumentStore by mockDocumentStore

val mockGraphStore: GraphStore = Mockito.mock(GraphStore::class.java)

class MockGraphStore : GraphStore by mockGraphStore {
    override fun getCountryCodeForScan(scanId: String): Either<StoreError, String> = "SG".right()
}
