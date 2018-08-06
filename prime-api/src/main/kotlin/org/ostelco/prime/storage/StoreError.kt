package org.ostelco.prime.storage

sealed class StoreError(val type: String, val id: String) {
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
class ValidationError(
        type: String, id: String,
        override var message: String) : StoreError(type, id)