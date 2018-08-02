package org.ostelco.prime.client.api.store

import io.vavr.control.Either
import io.vavr.control.Option
import org.ostelco.prime.client.api.core.ApiError
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.model.Person
import org.ostelco.prime.client.api.model.SubscriptionStatus
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription

/**
 *
 */
interface SubscriberDAO {

    fun getProfile(subscriptionId: String): Either<ApiError, Subscriber>

    fun createProfile(subscriptionId: String, profile: Subscriber, referredBy: String?): Either<ApiError, Subscriber>

    fun updateProfile(subscriptionId: String, profile: Subscriber): Either<ApiError, Subscriber>

    @Deprecated("use getSubscriptions", ReplaceWith("getSubscriptions", "org.ostelco.prime.client.api.model.Subscription"))
    fun getSubscriptionStatus(subscriptionId: String): Either<ApiError, SubscriptionStatus>

    fun getSubscriptions(subscriptionId: String): Either<ApiError, Collection<Subscription>>

    fun getPurchaseHistory(subscriptionId: String): Either<ApiError, Collection<PurchaseRecord>>

    fun getMsisdn(subscriptionId: String): Either<ApiError, String>

    fun getProducts(subscriptionId: String): Either<ApiError, Collection<Product>>

    fun purchaseProduct(subscriptionId: String, sku: String): Option<ApiError>

    fun getConsents(subscriptionId: String): Either<ApiError, Collection<Consent>>

    fun acceptConsent(subscriptionId: String, consentId: String): Either<ApiError, Consent>

    fun rejectConsent(subscriptionId: String, consentId: String): Either<ApiError, Consent>

    fun reportAnalytics(subscriptionId: String, events: String): Option<ApiError>

    fun storeApplicationToken(msisdn: String, applicationToken: ApplicationToken): Either<ApiError, ApplicationToken>

    fun getReferrals(name: String): Either<ApiError, Collection<Person>>

    fun getReferredBy(name: String): Either<ApiError, Person>

    companion object {

        /**
         * Profile is only valid when name and email set.
         */
        fun isValidProfile(profile: Subscriber?): Boolean {
            return (profile != null
                    && !profile.name.isEmpty()
                    && !profile.email.isEmpty())
        }

        /**
         * The application token is only valid if token,
         * applicationID and token type is set.
         */
        fun isValidApplicationToken(appToken: ApplicationToken?): Boolean {
            return (appToken != null
                    && !appToken.token.isEmpty()
                    && !appToken.applicationID.isEmpty()
                    && !appToken.tokenType.isEmpty())
        }
    }
}