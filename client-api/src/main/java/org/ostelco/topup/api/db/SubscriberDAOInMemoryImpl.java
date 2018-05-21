package org.ostelco.topup.api.db;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.ostelco.prime.client.api.model.Product;
import org.ostelco.topup.api.core.Consent;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.core.SubscriptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An 'in-memory' store for testing.
 *
 */
public class SubscriberDAOInMemoryImpl implements SubscriberDAO {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriberDAOInMemoryImpl.class);

    /* Table for 'profiles'. */
    private final ConcurrentHashMap<String, Profile> profileTable = new ConcurrentHashMap<>();

    /* Table for 'profiles'. */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> consentMap = new ConcurrentHashMap<>();

    @Override
    public Either<Error, Profile> getProfile(final String subscriptionId) {
        LOG.info("getProfile({})",subscriptionId);
        if (profileTable.containsKey(subscriptionId)) {
            return Either.right(profileTable.get(subscriptionId));
        }
        return Either.left(new Error("No profile found"));
    }

    @Override
    public Option<Error> createProfile(final String subscriptionId, final Profile profile) {
        LOG.info("createProfile({})",subscriptionId);
        if (!profile.isValid()) {
            return Option.of(new Error("Incomplete profile description"));
        }
        LOG.info("save Profile({})",subscriptionId);
        profileTable.put(subscriptionId, profile);
        return Option.none();
    }

    @Override
    public Option<Error> updateProfile(final String subscriptionId, final Profile profile) {
        LOG.info("updateProfile({})",subscriptionId);
        if (!profile.isValid()) {
            return Option.of(new Error("Incomplete profile description"));
        }
        LOG.info("save update Profile({})",subscriptionId);
        profileTable.put(subscriptionId, profile);
        return Option.none();
    }

    @Override
    public Either<Error, SubscriptionStatus> getSubscriptionStatus(final String subscriptionId) {
        return Either.right(new SubscriptionStatus(1_000_000_000,
                Collections.singletonList(new Product("DataTopup3GB", 250f, "NOK"))));
    }

    @Override
    public Either<Error, List<Product>> getProducts(final String subscriptionId) {
        return Either.right(Collections.singletonList(new Product("DataTopup3GB", 250f, "NOK")));
    }

    @Override
    public Option<Error> purchaseProduct(final String subscriptionId, final String sku) {
        return Option.none();
    }

    @Override
    public Either<Error, List<Consent>> getConsents(final String subscriptionId) {
        consentMap.putIfAbsent(subscriptionId, new ConcurrentHashMap<>());
        consentMap.get(subscriptionId).putIfAbsent("privacy", false);
        return Either.right(Collections.singletonList(new Consent(
                "privacy",
                "Grant permission to process personal data",
                consentMap.get(subscriptionId).get("privacy"))));
    }

    @Override
    public Option<Error> acceptConsent(final String subscriptionId, final String consentId) {
        consentMap.putIfAbsent(subscriptionId, new ConcurrentHashMap<>());
        consentMap.get(subscriptionId).put(consentId, true);
        return Option.none();
    }

    @Override
    public Option<Error> rejectConsent(final String subscriptionId, final String consentId) {
        consentMap.putIfAbsent(subscriptionId, new ConcurrentHashMap<>());
        consentMap.get(subscriptionId).put(consentId, false);
        return Option.none();
    }

    @Override
    public Option<Error> reportAnalytics(final String subscriptionId, final String events) {
        return Option.none();
    }
}
