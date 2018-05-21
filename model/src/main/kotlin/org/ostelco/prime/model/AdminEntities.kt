package org.ostelco.prime.model

interface Entity {
    var id: String
}

data class Offer(
        override var id: String = "",
        val segments: List<Segment> = emptyList(),
        val products: List<AdminProduct> = emptyList()) : Entity

data class Segment(
        override var id: String = "",
        val subscribers: List<SubscriberV2> = emptyList()) : Entity

data class SubscriberV2(
        override var id: String = "",
        val name: String = "",
        val email: String = "",
        val address: String = "") : Entity

// TODO maybe rename this later
data class AdminProduct(
        var sku: String = "",
        val productClass: String = "",
        val properties: Map<String, String> = mapOf(),
        val presentation: Map<String, String> = mapOf()) : Entity {

    override var id: String
        get() = sku
        set(value) {
            sku = value
        }
}

data class ProductClass(
    override var id: String = "",
    val properties: List<String> = listOf()) : Entity
