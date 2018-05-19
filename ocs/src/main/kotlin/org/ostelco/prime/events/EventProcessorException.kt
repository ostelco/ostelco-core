package org.ostelco.prime.events

import org.ostelco.prime.model.PurchaseRequest
import org.ostelco.prime.storage.legacy.entities.NotATopupProductException
import java.util.*

class EventProcessorException : Exception {

    private val pr: PurchaseRequest?

    constructor(t: Throwable) : super(t) {
        this.pr = null
    }

    constructor(str: String, pr: PurchaseRequest) : super(str) {
        this.pr = pr
    }

    constructor(
            str: String,
            pr: PurchaseRequest,
            ex: NotATopupProductException) : super(str, ex) {
        this.pr = pr
    }

    constructor(str: String, ex: Throwable) : super(str, ex) {
        this.pr = null
    }

    override fun toString(): String {
        return super.toString() + ", pr = " + Objects.requireNonNull<PurchaseRequest>(pr).toString()
    }
}
