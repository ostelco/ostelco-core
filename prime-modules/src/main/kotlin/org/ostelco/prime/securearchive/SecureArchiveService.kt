package org.ostelco.prime.securearchive

import arrow.core.Either
import org.ostelco.prime.storage.StoreError

interface SecureArchiveService {

    fun archiveEncrypted(
            customerId: String,
            regionCodes: Collection<String>,
            fileName: String,
            dataMap: Map<String, ByteArray>): Either<StoreError, Unit>
}