package org.ostelco.prime.storage.documentstore

import com.google.cloud.Timestamp
import org.ostelco.prime.store.datastore.DatastoreExcludeFromIndex

data class CustomerActivity(
        val timestamp: Timestamp,
        val severity: String,
        @DatastoreExcludeFromIndex val message: String
)