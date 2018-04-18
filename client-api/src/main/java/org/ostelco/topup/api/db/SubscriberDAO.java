package org.ostelco.topup.api.db;

import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Grant;
import org.ostelco.topup.api.core.Offer;
import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.core.Consent;
import org.ostelco.topup.api.core.SignUp;
import org.ostelco.topup.api.core.SubscriptionStatus;

import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;

/**
 *
 */
public interface SubscriberDAO {

    public Option<Error> signUp(final SignUp signUp);

    public Either<Error, String> handleGrant(final Grant grant);

    public Either<Error, Profile> getProfile(final String subscriptionId);

    public Option<Error> updateProfile(final String subscriptionId, final Profile profile);

    public Either<Error, SubscriptionStatus> getSubscriptionStatus(final String subscriptionId);

    public Either<Error, List<Offer>> getOffers(final String subscriptionId);

    public Option<Error> acceptOffer(final String subscriptionId, final String offerId);

    public Option<Error> rejectOffer(final String subscriptionId, final String offerId);

    public Either<Error, List<Consent>> getConsents(final String subscriptionId);

    public Option<Error> acceptConsent(final String subscriptionId, final String consentId);

    public Option<Error> rejectConsent(final String subscriptionId, final String consentId);

    public Option<Error> reportAnalytics(final String subscriptionId, final String events);
}
