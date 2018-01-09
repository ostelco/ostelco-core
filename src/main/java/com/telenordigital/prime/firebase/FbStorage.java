package com.telenordigital.prime.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.FirebaseDatabase;
import com.telenordigital.prime.events.EventListeners;
import com.telenordigital.prime.storage.ProductDescriptionCache;
import com.telenordigital.prime.storage.ProductDescriptionCacheImpl;
import com.telenordigital.prime.storage.PurchaseRequestListener;
import com.telenordigital.prime.storage.Storage;
import com.telenordigital.prime.storage.StorageException;
import com.telenordigital.prime.storage.entities.Product;
import com.telenordigital.prime.storage.entities.PurchaseRequest;
import com.telenordigital.prime.storage.entities.Subscriber;
import com.telenordigital.prime.storage.entities.SubscriberImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class FbStorage implements Storage {

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

        facade.addProductCatalogItemListener(listeners::productCatalogItemListener);
        facade.addPurchaseRequestListener(listeners::purchaseRequestListener);

        // Load subscriber balance from firebase to in-memory OcsState
        listeners.loadSubscriberBalanceDataFromFirebaseToInMemoryStructure(getAllSubscribers());
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


    // XXX This method represents a bad design decision.  It's too circumspect to
    //     understand.  Fix!
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
        checkNotNull(id);
        facade.removeRecordOfPurchaseById(id);
    }

    @Override
    public String addRecordOfPurchaseByMsisdn(
            final String msisdn,
            final String sku,
            final long millisSinceEpoch) throws StorageException {
        checkNotNull(msisdn);
        checkNotNull(sku);
        checkArgument(millisSinceEpoch > 0);

        return facade.addRecordOfPurchaseByMsisdn(msisdn, sku, millisSinceEpoch);
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

    @Override
    public Subscriber getSubscriberFromMsisdn(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
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
