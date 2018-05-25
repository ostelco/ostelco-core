package org.ostelco.topup.api.db;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.ostelco.prime.client.api.model.Consent;
import org.ostelco.prime.client.api.model.SubscriptionStatus;
import org.ostelco.prime.model.Price;
import org.ostelco.prime.model.Product;
import org.ostelco.prime.model.Subscriber;
import org.ostelco.topup.api.core.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An 'in-memory' store for testing.
 *
 */
public class SubscriberDAOInMemoryImpl implements SubscriberDAO {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriberDAOInMemoryImpl.class);

    /* Table for 'profiles'. */
    private final ConcurrentHashMap<String, Subscriber> profileTable = new ConcurrentHashMap<>();

    /* Table for 'profiles'. */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> consentMap = new ConcurrentHashMap<>();

    @Override
    public Either<Error, Subscriber> getProfile(final String subscriptionId) {
        LOG.info("getProfile({})",subscriptionId);
        if (profileTable.containsKey(subscriptionId)) {
            return Either.right(profileTable.get(subscriptionId));
        }
        return Either.left(new Error("No profile found"));
    }

    @Override
    public Either<Error, Subscriber> createProfile(final String subscriptionId, final Subscriber profile) {
        LOG.info("createProfile({})",subscriptionId);
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(new Error("Incomplete profile description"));
        }
        LOG.info("save Profile({})",subscriptionId);
        profileTable.put(subscriptionId, profile);
        return getProfile(subscriptionId);
    }

    @Override
    public Either<Error, Subscriber> updateProfile(final String subscriptionId, final Subscriber profile) {
        LOG.info("updateProfile({})",subscriptionId);
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(new Error("Incomplete profile description"));
        }
        LOG.info("save update Profile({})",subscriptionId);
        profileTable.put(subscriptionId, profile);
        return getProfile(subscriptionId);
    }

    @Override
    public Either<Error, SubscriptionStatus> getSubscriptionStatus(final String subscriptionId) {
        return Either.right(new SubscriptionStatus(1_000_000_000,
                Collections.singletonList(
                        new Product("DataTopup3GB",
                                new Price(250, "NOK"),
                                Collections.emptyMap(),
                                Collections.emptyMap()))));
    }

    @Override
    public Either<Error, Collection<Product>> getProducts(final String subscriptionId) {
        return Either.right(Collections.singleton(
                new Product("DataTopup3GB",
                        new Price(250, "NOK"),
                        Collections.emptyMap(),
                        Collections.emptyMap())));
    }

    @Override
    public Option<Error> purchaseProduct(final String subscriptionId, final String sku) {
        return Option.none();
    }

    @Override
    public Either<Error, Collection<Consent>> getConsents(final String subscriptionId) {
        consentMap.putIfAbsent(subscriptionId, new ConcurrentHashMap<>());
        consentMap.get(subscriptionId).putIfAbsent("privacy", false);
        return Either.right(Collections.singletonList(new Consent(
                "privacy",
                "Grant permission to process personal data",
                consentMap.get(subscriptionId).get("privacy"))));
    }

    @Override
    public Either<Error, Consent> acceptConsent(final String subscriptionId, final String consentId) {
        consentMap.putIfAbsent(subscriptionId, new ConcurrentHashMap<>());
        consentMap.get(subscriptionId).put(consentId, true);
        return Either.right(new Consent(consentId, "Grant permission to process personal data", true));
    }

    @Override
    public Either<Error, Consent> rejectConsent(final String subscriptionId, final String consentId) {
        consentMap.putIfAbsent(subscriptionId, new ConcurrentHashMap<>());
        consentMap.get(subscriptionId).put(consentId, false);
        return Either.right(new Consent(consentId, "Grant permission to process personal data", false));
    }

    @Override
    public Option<Error> reportAnalytics(final String subscriptionId, final String events) {
        return Option.none();
    }
}
