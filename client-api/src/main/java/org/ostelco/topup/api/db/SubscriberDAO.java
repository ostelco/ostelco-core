package org.ostelco.topup.api.db;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.ostelco.prime.client.api.model.Consent;
import org.ostelco.prime.client.api.model.Product;
import org.ostelco.prime.client.api.model.Profile;
import org.ostelco.prime.client.api.model.SubscriptionStatus;
import org.ostelco.topup.api.core.Error;

import java.util.List;

/**
 *
 */
public interface SubscriberDAO {

    Either<Error, Profile> getProfile(final String subscriptionId);

    Option<Error> createProfile(final String subscriptionId, final Profile profile);

    Option<Error> updateProfile(final String subscriptionId, final Profile profile);

    Either<Error, SubscriptionStatus> getSubscriptionStatus(final String subscriptionId);

    Either<Error, List<Product>> getProducts(final String subscriptionId);

    Option<Error> purchaseProduct(final String subscriptionId, final String sku);

    Either<Error, List<Consent>> getConsents(final String subscriptionId);

    Option<Error> acceptConsent(final String subscriptionId, final String consentId);

    Option<Error> rejectConsent(final String subscriptionId, final String consentId);

    Option<Error> reportAnalytics(final String subscriptionId, final String events);

    static boolean isValidProfile(final Profile profile) {
        return profile != null
                && profile.getName() != null
                && !profile.getName().isEmpty()
                && profile.getEmail() != null
                && !profile.getEmail().isEmpty();
    }
}