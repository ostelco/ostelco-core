package org.ostelco.topup.api.db;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.ostelco.prime.client.api.model.Consent;
import org.ostelco.prime.client.api.model.SubscriptionStatus;
import org.ostelco.prime.model.Product;
import org.ostelco.prime.model.Subscriber;
import org.ostelco.topup.api.core.Error;

import java.util.Collection;

/**
 *
 */
public interface SubscriberDAO {

    Either<Error, Subscriber> getProfile(final String subscriptionId);

    Either<Error, Subscriber> createProfile(final String subscriptionId, final Subscriber profile);

    Either<Error, Subscriber> updateProfile(final String subscriptionId, final Subscriber profile);

    Either<Error, SubscriptionStatus> getSubscriptionStatus(final String subscriptionId);

    Either<Error, Collection<Product>> getProducts(final String subscriptionId);

    Option<Error> purchaseProduct(final String subscriptionId, final String sku);

    Either<Error, Collection<Consent>> getConsents(final String subscriptionId);

    Either<Error, Consent> acceptConsent(final String subscriptionId, final String consentId);

    Either<Error, Consent> rejectConsent(final String subscriptionId, final String consentId);

    Option<Error> reportAnalytics(final String subscriptionId, final String events);

    static boolean isValidProfile(final Subscriber profile) {
        return profile != null
                && !profile.getName().isEmpty()
                && !profile.getEmail().isEmpty();
    }
}