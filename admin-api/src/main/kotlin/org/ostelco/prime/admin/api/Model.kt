package org.ostelco.prime.admin.api

import org.ostelco.prime.model.Entity

class Offer : Entity {
    override var id: String = ""
    var segments: Array<String> = emptyArray()
    var products: Array<String> = emptyArray()
}

class Segment : Entity {
    override var id: String = ""
    var subscribers: Array<String> = emptyArray()
}