package org.ostelco.prime.storage

import org.ostelco.prime.module.getResource

// Access

interface ClientDataSource : ClientDocumentStore, ClientGraphStore

interface AdminDataSource : ClientDataSource,  AdminDocumentStore, AdminGraphStore

// Types

interface DocumentStore : ClientDocumentStore, AdminDocumentStore

interface GraphStore : ClientGraphStore, AdminGraphStore

// Type instances

val documentStore: DocumentStore = getResource()

val graphStore: GraphStore = getResource()

// Mixin(s) / Access Implementations

class ClientDataSourceImpl : ClientDataSource,
        ClientDocumentStore by documentStore,
        ClientGraphStore by graphStore

class AdminDataSourceImpl : AdminDataSource,
        DocumentStore by documentStore,
        GraphStore by graphStore

