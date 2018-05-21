package org.ostelco.prime.model

interface Entity {
    var id: String
}

class Offer : Entity {
    override var id: String = ""
    var segments: List<Segment> = emptyList()
    var products: List<AdminProduct> = emptyList()
}

class Segment : Entity {
    override var id: String = ""
    var subscribers: List<SubscriberV2> = emptyList()
}

class SubscriberV2 : Entity {
    override var id: String = ""
    var name: String = ""
    var email: String = ""
    var address: String = ""
}

// TODO maybe rename this later
class AdminProduct : Entity {
    override var id: String
        get() = sku
        set(value) {
            sku = value
        }
    var sku: String = ""
    var productClass: String = ""
    var properties: Map<String, String> = mapOf()
    var presentation: Map<String, String> = mapOf()
}

class ProductClass : Entity {
    override var id: String = ""
    var properties: List<String> = listOf()
}
