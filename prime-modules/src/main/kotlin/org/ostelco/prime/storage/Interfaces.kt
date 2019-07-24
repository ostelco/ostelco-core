package org.ostelco.prime.storage

import arrow.core.Either
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.model.CustomerActivity
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.module.getResource
import javax.ws.rs.core.MultivaluedMap

// Access

interface ClientDataSource : ClientDocumentStore, ClientGraphStore

interface AdminDataSource : ClientDataSource, AdminDocumentStore, AdminGraphStore

// Generic

interface ScanInformationStore {

    // Function to upsert scan information data from the 3rd party eKYC scan
    fun upsertVendorScanInformation(
            customerId: String,
            countryCode: String,
            vendorData: MultivaluedMap<String, String>
    ): Either<StoreError, Unit>

    fun getExtendedStatusInformation(
            scanInformation: ScanInformation
    ): Map<String, String>
}

interface AuditLogStore {

    /**
     * Log Customer Activity so that we have activity history
     */
    fun logCustomerActivity(
            customerId: String,
            customerActivity: CustomerActivity
    )

    /**
     * Get all customer activity history
     */
    fun getCustomerActivityHistory(
            customerId: String
    ): Either<String, Collection<CustomerActivity>>
}

// Types

interface DocumentStore : ClientDocumentStore, AdminDocumentStore, AuditLogStore

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

class AuditLogStoreImpl : AuditLogStore by documentStore