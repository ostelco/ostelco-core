package org.ostelco.prime.events;

import org.ostelco.prime.ocs.OcsState;
import org.ostelco.prime.storage.ProductCatalogItem;
import org.ostelco.prime.storage.ProductDescriptionCacheImpl;
import org.ostelco.prime.storage.PurchaseRequestListener;
import org.ostelco.prime.storage.StorageInitiatedEventExecutor;
import org.ostelco.prime.storage.entities.PurchaseRequestImpl;
import org.ostelco.prime.storage.entities.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

// Badly named class
public final class EventListeners {

    private static final Logger LOG = LoggerFactory.getLogger(EventListeners.class);

    private final StorageInitiatedEventExecutor executor;

    private final OcsState ocsState;

    public EventListeners(final OcsState ocsState) {
        this.executor = new StorageInitiatedEventExecutor();
        this.ocsState = checkNotNull(ocsState);
    }

    public void loadSubscriberBalanceDataFromFirebaseToInMemoryStructure(
            final Collection<Subscriber> subscribers) {
        LOG.info("Loading initial balance from storage to in-memory OcsState");
        for (final Subscriber subscriber : subscribers) {
            ocsState.injectSubscriberIntoOCS(subscriber);
        }
    }

    private long getMillisSinceEpoch() {
        return Instant.now().toEpochMilli();
    }


    public Void purchaseRequestListener(final String key, final PurchaseRequestImpl req) {
        req.setId(key);
        req.setMillisSinceEpoch(getMillisSinceEpoch());
        executor.onPurchaseRequest(req);
        return null; // XXX Hack to satisfy BiFunction's void return type
    }

    public void productCatalogItemListener(final ProductCatalogItem item) {
        ProductDescriptionCacheImpl.
                getInstance(). // XXX This is an awful hack!
                addTopupProduct(item.getSku(), item.getNoOfBytes());
    }

    // XXX I don't like this!
    public void addPurchaseRequestListener(final PurchaseRequestListener listener) {
        executor.addPurchaseRequestListener(listener);
    }
}
