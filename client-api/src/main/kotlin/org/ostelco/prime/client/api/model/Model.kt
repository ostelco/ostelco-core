package org.ostelco.prime.client.api.model

data class Consent(
        var consentId: String? = null,
        var description: String? = null,
        var accepted: Boolean = false)

data class ConsentList(val consents: List<Consent>)

//data class Grant(
//        val grantType: String,
//        val code: String,
//        val refreshToken: String)

data class Profile(var email: String? = null,
                   var name: String? = null,
                   var address: String? = null,
                   var postCode: String? = null,
                   var city: String? = null) {

    constructor(email: String) : this(email = email, name = null)
}

data class Product(
        var sku: String? = null,
        var price: Price? = null)

data class Price(
        var amount: Int? = null,
        var currency: String? = null)

data class ProductList(val products: List<Product>)

data class SubscriptionStatus(
        var remaining: Int = 0,
        var acceptedProducts: List<Product> = emptyList())

//data class Subscription(
//        val subscriptionId: String,
//        val name: String,
//        val email: String,
//        val msisdn: String,
//        val imsi: String)
