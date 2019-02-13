package org.ostelco.prime.ocs

import org.mockito.Mockito
import org.ostelco.prime.storage.DocumentStore
import org.ostelco.prime.storage.GraphStore

private val mockDocumentStore = Mockito.mock(DocumentStore::class.java)

class MockDocumentStore : DocumentStore by mockDocumentStore

val mockGraphStore: GraphStore = Mockito.mock(GraphStore::class.java)

class MockGraphStore : GraphStore by mockGraphStore
