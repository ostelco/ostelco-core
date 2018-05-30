package org.ostelco.prime.client.api.store

import io.vavr.control.Either
import io.vavr.control.Option
import org.ostelco.prime.client.api.core.ApiError
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.model.SubscriptionStatus
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.Subscriber

/**
 *
 */
interface SubscriberDAO {

    fun getProfile(subscriptionId: String): Either<ApiError, Subscriber>

    fun createProfile(subscriptionId: String, profile: Subscriber): Either<ApiError, Subscriber>

    fun updateProfile(subscriptionId: String, profile: Subscriber): Either<ApiError, Subscriber>

    fun getSubscriptionStatus(subscriptionId: String): Either<ApiError, SubscriptionStatus>

    fun getProducts(subscriptionId: String): Either<ApiError, Collection<Product>>

    fun purchaseProduct(subscriptionId: String, sku: String): Option<ApiError>

    fun getConsents(subscriptionId: String): Either<ApiError, Collection<Consent>>

    fun acceptConsent(subscriptionId: String, consentId: String): Either<ApiError, Consent>

    fun rejectConsent(subscriptionId: String, consentId: String): Either<ApiError, Consent>

    fun reportAnalytics(subscriptionId: String, events: String): Option<ApiError>

    companion object {

        fun isValidProfile(profile: Subscriber?): Boolean {
            return (profile != null
                    && !profile.name.isEmpty()
                    && !profile.email.isEmpty())
        }
    }
}