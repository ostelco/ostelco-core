package org.ostelco.prime.client.api.store

import arrow.core.Either
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.model.Person
import org.ostelco.prime.model.*
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.SourceDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SourceInfo


/**
 *
 */
interface SubscriberDAO {

    fun getProfile(subscriberId: String): Either<ApiError, Subscriber>

    fun createProfile(subscriberId: String, profile: Subscriber, referredBy: String?): Either<ApiError, Subscriber>

    fun updateProfile(subscriberId: String, profile: Subscriber): Either<ApiError, Subscriber>

    fun getSubscriptions(subscriberId: String): Either<ApiError, Collection<Subscription>>

    fun getBundles(subscriberId: String): Either<ApiError, Collection<Bundle>>

    fun getPurchaseHistory(subscriberId: String): Either<ApiError, Collection<PurchaseRecord>>

    fun getProduct(subscriptionId: String, sku: String): Either<ApiError, Product>

    fun getMsisdn(subscriberId: String): Either<ApiError, String>

    fun getProducts(subscriberId: String): Either<ApiError, Collection<Product>>

    fun purchaseProduct(subscriberId: String, sku: String, sourceId: String?, saveCard: Boolean): Either<ApiError, ProductInfo>

    fun getConsents(subscriberId: String): Either<ApiError, Collection<Consent>>

    fun acceptConsent(subscriberId: String, consentId: String): Either<ApiError, Consent>

    fun rejectConsent(subscriberId: String, consentId: String): Either<ApiError, Consent>

    fun reportAnalytics(subscriberId: String, events: String): Either<ApiError, Unit>

    fun storeApplicationToken(msisdn: String, applicationToken: ApplicationToken): Either<ApiError, ApplicationToken>

    fun getReferrals(subscriberId: String): Either<ApiError, Collection<Person>>

    fun getReferredBy(subscriberId: String): Either<ApiError, Person>

    fun createSource(subscriberId: String, sourceId: String): Either<ApiError, SourceInfo>

    fun setDefaultSource(subscriberId: String, sourceId: String): Either<ApiError, SourceInfo>

    fun listSources(subscriberId: String): Either<ApiError, List<SourceDetailsInfo>>

    fun removeSource(subscriberId: String, sourceId: String): Either<ApiError, SourceInfo>

    fun getActivePseudonymForSubscriber(subscriberId: String): Either<ApiError, ActivePseudonyms>

    fun getStripeEphemeralKey(subscriberId: String, apiVersion: String): Either<ApiError, String>

    fun newEKYCScanId(subscriberId: String): Either<ApiError, ScanInformation>

    companion object {

        /**
         * Profile is only valid when name and email set.
         */
        fun isValidProfile(profile: Subscriber?): Boolean {
            return (profile != null
                    && !profile.name.isEmpty()
                    && !profile.email.isEmpty()
                    && !profile.country.isEmpty())
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
