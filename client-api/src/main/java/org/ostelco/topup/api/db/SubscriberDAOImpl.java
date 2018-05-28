package org.ostelco.topup.api.db;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.ostelco.prime.client.api.model.Consent;
import org.ostelco.prime.client.api.model.SubscriptionStatus;
import org.ostelco.prime.model.Product;
import org.ostelco.prime.model.PurchaseRecord;
import org.ostelco.prime.model.Subscriber;
import org.ostelco.prime.ocs.OcsSubscriberService;
import org.ostelco.prime.storage.legacy.Storage;
import org.ostelco.prime.storage.legacy.StorageException;
import org.ostelco.topup.api.core.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class SubscriberDAOImpl implements SubscriberDAO {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriberDAOImpl.class);

    private final Storage storage;

    private final OcsSubscriberService ocsSubscriberService;

    /* Table for 'profiles'. */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> consentMap = new ConcurrentHashMap<>();

    public SubscriberDAOImpl(Storage storage, OcsSubscriberService ocsSubscriberService) {
        this.storage = storage;
        this.ocsSubscriberService = ocsSubscriberService;
    }

    @Override
    public Either<Error, Subscriber> getProfile(final String subscriptionId) {
        try {
            final Subscriber subscriber = storage.getSubscriber(subscriptionId);
            if (subscriber == null) {
                return Either.left(new Error("Incomplete profile description"));
            }
            return Either.right(new Subscriber(
                    subscriber.getEmail(),
                    subscriber.getName(),
                    subscriber.getAddress(),
                    subscriber.getPostCode(),
                    subscriber.getCity(),
                    subscriber.getCountry()));
        } catch (StorageException e) {
            LOG.error("Failed to fetch profile", e);
            return Either.left(new Error("Failed to fetch profile"));
        }
    }

    @Override
    public Either<Error, Subscriber> createProfile(final String subscriptionId, final Subscriber profile) {
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(new Error("Incomplete profile description"));
        }
        try {
            storage.addSubscriber(subscriptionId, new Subscriber(
                    profile.getEmail(),
                    profile.getName(),
                    profile.getAddress(),
                    profile.getPostCode(),
                    profile.getCity(),
                    profile.getCountry()));
        } catch (StorageException e) {
            LOG.error("Failed to create profile", e);
            return Either.left(new Error("Failed to create profile"));
        }
        return getProfile(subscriptionId);
    }

    @Override
    public Either<Error, Subscriber> updateProfile(final String subscriptionId, final Subscriber profile) {
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(new Error("Incomplete profile description"));
        }
        try {
            storage.updateSubscriber(subscriptionId, new Subscriber(
                    profile.getEmail(),
                    profile.getName(),
                    profile.getAddress(),
                    profile.getPostCode(),
                    profile.getCity(),
                    profile.getCountry()));
        } catch (StorageException e) {
            LOG.error("Failed to update profile", e);
            return Either.left(new Error("Failed to update profile"));
        }
        return getProfile(subscriptionId);
    }

    @Override
    public Either<Error, SubscriptionStatus> getSubscriptionStatus(final String subscriptionId) {
        try {
            final Long balance = storage.getBalance(subscriptionId);
            if (balance == null) {
                return Either.left(new Error("No subscription data found"));
            }
            final Collection<PurchaseRecord> purchaseRecords = storage.getPurchaseRecords(subscriptionId);
            final SubscriptionStatus subscriptionStatus = new SubscriptionStatus(
                    balance, new ArrayList<>(purchaseRecords));
            return Either.right(subscriptionStatus);
        } catch (StorageException e) {
            LOG.error("Failed to get balance", e);
            return Either.left(new Error("Failed to get balance"));
        }
    }

    @Override
    public Either<Error, Collection<Product>> getProducts(final String subscriptionId) {
        try {
            final Map<String, Product> products = storage.getProducts();
            if (products.isEmpty()) {
                return Either.left(new Error("No products found"));
            }
            products.forEach((key, value) -> value.setSku(key));
            return Either.right(products.values());

        } catch (StorageException e) {
            LOG.error("Failed to get Products", e);
            return Either.left(new Error("Failed to get Products"));
        }
    }

    @Override
    public Option<Error> purchaseProduct(final String subscriptionId, final String sku) {
        String msisdn = null;
        try {
            msisdn = storage.getSubscription(subscriptionId);
        } catch (StorageException e) {
            LOG.error("Did not find subscription", e);
        }
        if (msisdn == null) {
            return Option.of(new Error("Did not find subscription"));
        }

        Product product = null;
        try {
            product = storage.getProduct(sku);
        } catch (StorageException e) {
            LOG.error("Did not find product: sku = " + sku, e);
            return Option.of(new Error("Product unavailable"));
        }

        product.setSku(sku);
        final PurchaseRecord purchaseRecord = new PurchaseRecord(
                msisdn,
                product,
                Instant.now().toEpochMilli());
        try {
            storage.addPurchaseRecord(subscriptionId, purchaseRecord);
        } catch (StorageException e) {
            LOG.error("Failed to save purchase record", e);
            return Option.of(new Error("Failed to save purchase record"));
        }

        ocsSubscriberService.topup(msisdn, sku);
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
