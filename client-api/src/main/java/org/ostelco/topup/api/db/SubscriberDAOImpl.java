package org.ostelco.topup.api.db;

import org.ostelco.topup.api.core.Consent;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Grant;
import org.ostelco.topup.api.core.Product;
import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.core.SubscriptionStatus;

import com.google.cloud.datastore.Datastore;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 *
 */
@AllArgsConstructor
public class SubscriberDAOImpl implements SubscriberDAO {

    @NonNull
    private Datastore store;

    @Override
    public Option<Error> signUp(final Profile profile) {
        return Option.none();
    }

    @Override
    public Either<Error, String> handleGrant(final Grant grant) {
        return Either.right("");
    }

    @Override
    public Either<Error, Profile> getProfile(final String subscriptionId) {
        return Either.left(new Error("Incomplete profile description"));
    }

    @Override
    public Option<Error> updateProfile(final String subscriptionId, final Profile profile) {
        if (!profile.isValid()) {
            return Option.of(new Error("Incomplete profile description"));
        }
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
