package com.telenordigital.prime.firebase;

import com.telenordigital.prime.events.Product;
import com.telenordigital.prime.events.PurchaseRequest;
import com.telenordigital.prime.events.PurchaseRequestListener;
import com.telenordigital.prime.events.Storage;
import com.telenordigital.prime.events.StorageException;
import com.telenordigital.prime.events.Subscriber;
import com.telenordigital.prime.ocs.state.OcsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Interface towards a firebase implementation of storage.
 * It's also a very simple cache that will use cached
 * instances of subscriber records instead of asking the
 * firebase database for authorative data.
 *
 * It will also listen for changes coming from firebase, but
 * clearly there is room for race conditions in the present
 * implementation.
 *
 * The FbStorage is also not a pure cache, since it makes
 * the assumption that the backend is an InnerFbStorage instance,
 * not a generic Storage instance.
 */
public final class FbStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(FbStorage.class);

    private final InnerFbStorage innerStorage;

    private final SubscriberCache cache;

    public FbStorage(final String databaseName,
                     final String configFile,
                     final OcsState ocsState) throws StorageException {
        checkNotNull(databaseName);
        checkNotNull(configFile);
        this.innerStorage = new InnerFbStorage(databaseName, configFile, ocsState);
        this.cache = new SubscriberCache();
    }

    public void primeCache() {
        final Collection<Subscriber> allSubscribers = innerStorage.getAllSubscribers();
        this.cache.primeCache(allSubscribers);
    }

    @Override
    public void addPurchaseRequestListener(final PurchaseRequestListener listener) {
        innerStorage.addPurchaseRequestListener(listener);
    }

    @Override
    public String addRecordOfPurchaseByMsisdn(
            final String msisdn,
            final String sku,
            final long now)
            throws StorageException {
        checkNotNull(msisdn);
        return innerStorage.addRecordOfPurchaseByMsisdn(msisdn, sku, now);
    }

    @Override
    public void removePurchaseRequestById(final String id) {
        innerStorage.removePurchaseRequestById(id);
    }

    @Override
    public void removeRecordOfPurchaseById(final String id) {
        innerStorage.removeRecordOfPurchaseById(id);
    }

    @Override
    public String injectPurchaseRequest(final PurchaseRequest pr) {
        return innerStorage.injectPurchaseRequest(pr);
    }

    @Override
    public void updatedisplaydatastructure(String msisdn) throws StorageException {
        checkNotNull(msisdn);
        innerStorage.updatedisplaydatastructure(msisdn);
    }

    @Override
    public void removeDisplayDatastructure(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        innerStorage.removeDisplayDatastructure(msisdn);
    }

    @Override
    public void setRemainingByMsisdn(final String msisdn, final long noOfBytes)
            throws StorageException {
        checkNotNull(msisdn);
        cache.writeLock(msisdn);
        try {

            // Subscriber sub = cache.getSubscriber(msisdn);
            // XXX TBD
            innerStorage.setRemainingByMsisdn(msisdn, noOfBytes);
        } finally {
            cache.unlock(msisdn);
        }
    }

    @Override
    public Subscriber getSubscriberFromMsisdn(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        cache.readLock(msisdn);
        try {
            if (cache.containsSubscriber(msisdn)) {
                return cache.getSubscriber(msisdn);
            } else {
                cache.readLock(msisdn);
                final FbSubscriber sub = (FbSubscriber) innerStorage.getSubscriberFromMsisdn(msisdn);
                cache.insertSubscriber(sub);
                return sub;
            }
        } finally {
            cache.unlock(msisdn);
        }
    }

    @Override
    public String insertNewSubscriber(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        cache.writeLock(msisdn);
        try {
            if (cache.containsSubscriber(msisdn)) {
                // Or an error?
                final FbSubscriber fbsub = (FbSubscriber) cache.getSubscriber(msisdn);
                return fbsub.getFbKey();
            } else {
                // XXX I dislike this  "getKey" nonsense.  It's a layering violation
                //     I would rather do without.
                final String key = innerStorage.insertNewSubscriber(msisdn);
                final Subscriber sub = innerStorage.getSubscriberFromMsisdn(msisdn);
                cache.insertSubscriber(sub);
                return key;
            }
        } finally {
            cache.unlock(msisdn);
        }
    }


    @Override
    public void removeSubscriberByMsisdn(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        cache.writeLock(msisdn);
        try {
            cache.removeSubscriber(msisdn);
            innerStorage.removeSubscriberByMsisdn(msisdn);
        } finally {
            cache.unlock(msisdn);
        }
    }

    @Override
    public void addTopupProduct(final String sku, final long noOfBytes) {
        innerStorage.addTopupProduct(sku, noOfBytes);
    }

    @Override
    public boolean isValidSKU(final String sku) {
        return innerStorage.isValidSKU(sku);
    }

    @Override
    public Product getProductForSku(final String sku) {
       return innerStorage.getProductForSku(sku);
    }
}
