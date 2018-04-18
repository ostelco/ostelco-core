package org.ostelco.topup.api.db;

import org.ostelco.topup.api.core.Consent;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Grant;
import org.ostelco.topup.api.core.Offer;
import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.core.SignUp;
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
    public Option<Error> signUp(final SignUp signUp) {
        return Option.of(null);
    }

    @Override
    public Either<Error, String> handleGrant(final Grant grant) {
        return Either.right("");
    }

    @Override
    public Either<Error, Profile> getProfile(final String subscriptionId) {
        return Either.left(new Error());
    }

    @Override
    public Option<Error> updateProfile(final String subscriptionId, final Profile profile) {
        return Option.of(null);
    }

    @Override
    public Either<Error, SubscriptionStatus> getSubscriptionStatus(final String subscriptionId) {
        return Either.left(new Error());
    }

    @Override
    public Either<Error, List<Offer>> getOffers(final String subscriptionId) {
        return Either.left(new Error());
    }

    @Override
    public Option<Error> acceptOffer(final String subscriptionId, final String offerId) {
        return Option.of(null);
    }

    @Override
    public Option<Error> rejectOffer(final String subscriptionId, final String offerId) {
        return Option.of(null);
    }

    @Override
    public Either<Error, List<Consent>> getConsents(final String subscriptionId) {
        return Either.left(new Error());
    }

    @Override
    public Option<Error> acceptConsent(final String subscriptionId, final String consentId) {
        return Option.of(null);
    }

    @Override
    public Option<Error> rejectConsent(final String subscriptionId, final String consentId) {
        return Option.of(null);
    }

    @Override
    public Option<Error> reportAnalytics(final String subscriptionId, final String events) {
        return Option.of(null);
    }
}
