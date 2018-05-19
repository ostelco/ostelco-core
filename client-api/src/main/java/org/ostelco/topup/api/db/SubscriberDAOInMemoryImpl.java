package org.ostelco.topup.api.db;

import org.ostelco.topup.api.core.Consent;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Product;
import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.core.SubscriptionStatus;

import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;

/**
 * An 'in-memory' store for testing.
 *
 */
public class SubscriberDAOInMemoryImpl implements SubscriberDAO {

    /* Table for 'profiles'. */
    private final ConcurrentHashMap<String, Profile> profileTable = new ConcurrentHashMap<>();

    @Override
    public Either<Error, Profile> getProfile(final String subscriptionId) {
        if (profileTable.containsKey(subscriptionId)) {
            return Either.right(profileTable.get(subscriptionId));
        }
        return Either.left(new Error("No profile found"));
    }

    @Override
    public Option<Error> createProfile(final String subscriptionId, final Profile profile) {
        if (!profile.isValid()) {
            return Option.of(new Error("Incomplete profile description"));
        }
        profileTable.put(subscriptionId, profile);
        return Option.none();
    }

    @Override
    public Option<Error> updateProfile(final String subscriptionId, final Profile profile) {
        if (!profile.isValid()) {
            return Option.of(new Error("Incomplete profile description"));
        }
        profileTable.put(subscriptionId, profile);
        return Option.none();
    }

    @Override
    public Either<Error, SubscriptionStatus> getSubscriptionStatus(final String subscriptionId) {
        return Either.left(new Error("No subscription data found"));
    }

    @Override
    public Either<Error, List<Product>> getProducts(final String subscriptionId) {
        return Either.left(new Error("No products found"));
    }

    @Override
    public Option<Error> purchaseProduct(final String subscriptionId, final String sku) {
        return Option.none();
    }

    @Override
    public Either<Error, List<Consent>> getConsents(final String subscriptionId) {
        return Either.left(new Error("No consents found"));
    }

    @Override
    public Option<Error> acceptConsent(final String subscriptionId, final String consentId) {
        return Option.none();
    }

    @Override
    public Option<Error> rejectConsent(final String subscriptionId, final String consentId) {
        return Option.none();
    }

    @Override
    public Option<Error> reportAnalytics(final String subscriptionId, final String events) {
        return Option.none();
    }
}
