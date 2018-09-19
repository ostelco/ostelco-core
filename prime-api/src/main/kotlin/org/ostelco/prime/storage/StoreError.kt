package org.ostelco.prime.storage

import org.ostelco.prime.core.InternalError

sealed class StoreError(val type: String, val id: String) : InternalError() {
    open var message: String = ""
        protected set
}

class NotFoundError(type: String, id: String) : StoreError(type, id) {
    init {
        super.message = "$type - $id not found."
    }
}

class AlreadyExistsError(type: String, id: String) : StoreError(type, id) {
    init {
        super.message = "$type - $id already exists."
    }
}

class NotCreatedError(
        type: String,
        id: String = "",
        val expectedCount: Int = 1,
        val actualCount:Int = 0) : StoreError(type, id) {

    init {
        super.message = "Failed to create $type - $id"
    }
}

class NotUpdatedError(type: String, id: String) : StoreError(type, id) {
    init {
        super.message = "$type - $id not updated."
    }
}

class NotDeletedError(type: String, id: String) : StoreError(type, id) {
    init {
        super.message = "$type - $id not deleted."
    }
}

class ValidationError(
        type: String, id: String,
        override var message: String) : StoreError(type, id)