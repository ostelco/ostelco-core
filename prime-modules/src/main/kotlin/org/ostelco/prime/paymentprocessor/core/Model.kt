package org.ostelco.prime.paymentprocessor.core

class PlanInfo(val id: String)

class ProductInfo(val id: String)

class ProfileInfo(val id: String)

class SourceInfo(val id: String, val details: Map<String, Any>? = null)

class SubscriptionInfo(val id: String)
