package com.telenordigital.prime.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.telenordigital.prime.events.EventListeners;
import com.telenordigital.prime.storage.ProductDescriptionCache;
import com.telenordigital.prime.storage.ProductDescriptionCacheImpl;
import com.telenordigital.prime.storage.PurchaseRequestListener;
import com.telenordigital.prime.storage.Storage;
import com.telenordigital.prime.storage.StorageException;
import com.telenordigital.prime.storage.entities.Product;
import com.telenordigital.prime.storage.entities.PurchaseRequest;
import com.telenordigital.prime.storage.entities.RecordOfPurchaseImpl;
import com.telenordigital.prime.storage.entities.Subscriber;
import com.telenordigital.prime.storage.entities.SubscriberImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FbStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(FbStorage.class);

    private final ProductDescriptionCache productCache;

    private final FbDatabaseFacade facade;

    private final EventListeners listeners;

    public FbStorage(final String databaseName,
                     final String configFile,
                     final EventListeners listeners) throws StorageException {

        checkNotNull(configFile);
        checkNotNull(databaseName);
        this.listeners = checkNotNull(listeners);

        this.productCache = ProductDescriptionCacheImpl.getInstance();

        final FirebaseDatabase firebaseDatabase;
        firebaseDatabase = setupFirebaseInstance(databaseName, configFile);

        this.facade = new FbDatabaseFacade(firebaseDatabase);

        // Load subscriber balance from firebase to in-memory OcsState
        listeners.loadSubscriberBalanceDataFromFirebaseToInMemoryStructure(getAllSubscribers());

        // Scoop up products left and right (and don't worry about duplicates, race conditions or
        // anything else by sending them to the listeners.

        // XXX The next two invocations represents glue between the FB storage
        //     and other components.  The code specifying the interface does not
        //     belong in this class, it should be moved up one level along with
        //     the executor that's used to facilitate.   Also, it should
        //     be considered if a disruptor is a better choice than
        //     an executor (it probably isn't but the reasoning should be made clear).
        facade.addProductCatalogItemListener(listeners::productCatalogItemListener);

        // When a purhase request arrives, then send it to the executor.
        facade.addPurchaseRequestListener(listeners::purchaseRequestListener);
    }

    @Override
    public void addTopupProduct(final String sku, final long noOfBytes) {
        productCache.addTopupProduct(sku, noOfBytes);
    }

    @Override
    public boolean isValidSKU(final String sku) {
        return productCache.isValidSKU(sku);
    }

    @Override
    public Product getProductForSku(final String sku) {
        return productCache.getProductForSku(sku);
    }

    private FirebaseDatabase setupFirebaseInstance(
            final String databaseName,
            final String configFile) throws StorageException {
        try (FileInputStream serviceAccount = new FileInputStream(configFile)) {

            final FirebaseOptions options = new FirebaseOptions.Builder().
                    setCredential(FirebaseCredentials.fromCertificate(serviceAccount)).
                    setDatabaseUrl("https://" + databaseName + ".firebaseio.com/").
                    build();

            try {
                FirebaseApp.getInstance();
            } catch (Exception e) {
                FirebaseApp.initializeApp(options);
            }

            return FirebaseDatabase.getInstance();

            // (un)comment next line to turn on/of extended debugging
            // from firebase.
            // this.firebaseDatabase.setLogLevel(com.google.firebase.database.Logger.Level.DEBUG);

        } catch (IOException ex) {
            throw new StorageException(ex);
        }
    }

    // XXX Very odd
    @Override
    public void addPurchaseRequestListener(final PurchaseRequestListener listener) {
        checkNotNull(listener);
        listeners.addPurchaseRequestListener(listener);
    }

    @Override
    public void updateDisplayDatastructure(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        final SubscriberImpl subscriber = (SubscriberImpl) getSubscriberFromMsisdn(msisdn);

        if (subscriber == null) {
            throw new StorageException("Unknown MSISDN " + msisdn);
        }

        // XXX This is both:
        //     a) A layering violation, since it mixes a backend server/information
        //        layer with actual text formatting in an frontend/UX layer.
        //     b) Possibly also a good idea, since it makes it really quick to change
        //        the UI on a  per-user basis if we so desire.
        final long noOfBytes = subscriber.getNoOfBytesLeft();
        final float noOfGBLeft = noOfBytes / 1.0E09f;
        final String gbLeft = String.format("%.2f GB", noOfGBLeft);

        facade.updateClientVisibleUsageString(msisdn, gbLeft);
    }

    @Override
    public void removeDisplayDatastructure(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        facade.removeByMsisdn(msisdn);
    }

    @Override
    public String injectPurchaseRequest(final PurchaseRequest pr) {
        checkNotNull(pr);
        return facade.injectPurchaseRequest(pr);
    }

    @Override
    public void removeRecordOfPurchaseById(final String id) {
        facade.removeRecordOfPurchaseById(id);
    }

    @Override
    public String addRecordOfPurchaseByMsisdn(
            final String msisdn,
            final String sku,
            final long millisSinceEpoch) throws StorageException {
        checkNotNull(msisdn);

        final RecordOfPurchaseImpl purchase =
                new RecordOfPurchaseImpl(msisdn, sku, millisSinceEpoch);

        // XXX This is iffy, why not send the purchase object
        //     directly to the fascade.  Seems bogus, probably is.
        final Map<String, Object> asMap = purchase.asMap();

        return facade.pushRecordOfPurchaseByMsisdn(asMap);
    }

    @Override
    public void removeSubscriberByMsisdn(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        facade.removeSubscriberByMsisdn(msisdn);
    }

    @Override
    public void removePurchaseRequestById(final String id) {
        checkNotNull(id);
        facade.removePurchaseRequestById(id);
    }

    // XXX Should this be removed? Doesn't look nice.
    static void handleDataChange(
            final DataSnapshot snapshot,
            final CountDownLatch cdl,
            final Set<String> result,
            final String msisdn) {
        if (!snapshot.hasChildren()) {
            cdl.countDown();
        } else {
            try {
                for (final DataSnapshot snap : snapshot.getChildren()) {
                    final String key = snap.getKey();
                    result.add(key);
                    cdl.countDown();
                }
            } catch (Exception e) {
                LOG.error("Something happened while looking for key = " + msisdn, e);
            }
        }
    }

    @Override
    public Subscriber getSubscriberFromMsisdn(final String msisdn) throws StorageException {
        return facade.getSubscriberFromMsisdn(msisdn);
    }

    @Override
    public void setRemainingByMsisdn(
            final String msisdn,
            final long noOfBytes) throws StorageException {

        if (msisdn == null) {
            throw new StorageException("msisdn can't be null");
        }
        if (noOfBytes < 0) {
            throw new StorageException("noOfBytes can't be negative");
        }

        final SubscriberImpl sub = (SubscriberImpl) getSubscriberFromMsisdn(msisdn);
        if (sub == null) {
            throw new StorageException("Unknown msisdn " + msisdn);
        }

        sub.setNoOfBytesLeft(noOfBytes);

        facade.updateAuthorativeUserData(sub);
    }

    @Override
    public String insertNewSubscriber(final String msisdn) {
        checkNotNull(msisdn);
        final SubscriberImpl sub = new SubscriberImpl();
        sub.setMsisdn(msisdn);
        return facade.insertNewSubscriber(sub);
    }

    @Override
    public Collection<Subscriber> getAllSubscribers() {
        return facade.getAllSubscribers();
    }
}
