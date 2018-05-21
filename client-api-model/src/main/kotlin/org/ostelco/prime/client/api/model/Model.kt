package org.ostelco.prime.client.api.model

//data class Consent(
//        val consentId: String,
//        val description: String,
//        val accepted: Boolean = false)

//data class Grant(
//        val grantType: String,
//        val code: String,
//        val refreshToken: String)

//data class Profile(
//        val name: String,
//        val email: String)

data class Product(
        var sku: String? = null,
        var amount: Float? = null,
        var currency: String? = null)

//data class SubscriptionStatus(
//        val remaining: Int = 0,
//        val acceptedProducts: List<Product>)

//data class Subscription(
//        val subscriptionId: String,
//        val name: String,
//        val email: String,
//        val msisdn: String,
//        val imsi: String)
