package org.ostelco.prime.storage

import org.ostelco.prime.apierror.InternalError

sealed class StoreError(val type: String, val id: String, var message: String, val error: InternalError?) : InternalError()

class NotFoundError(type: String, id: String, error: InternalError? = null) : StoreError(type, id, message = "$type - $id not found.", error = error)

class AlreadyExistsError(type: String, id: String, error: InternalError? = null) : StoreError(type, id, message = "$type - $id already exists.", error = error)

class NotCreatedError(
        type: String,
        id: String = "",
        val expectedCount: Int = 1,
        val actualCount:Int = 0,
        error: InternalError? = null) : StoreError(type, id, message = "Failed to create $type - $id", error = error)

class NotUpdatedError(type: String, id: String, error: InternalError? = null) : StoreError(type, id, message = "$type - $id not updated.", error = error)

class NotDeletedError(type: String, id: String, error: InternalError? = null) : StoreError(type, id, message = "$type - $id not deleted.", error = error)

class ValidationError(
        type: String,
        id: String,
        message: String,
        error: InternalError? = null) : StoreError(type, id, message = message, error = error)

class FileDownloadError(
        filename: String,
        status: String,
        error: InternalError? = null) : StoreError("Download Error", filename,  message = "File download error : $filename, status : $status", error = error)
