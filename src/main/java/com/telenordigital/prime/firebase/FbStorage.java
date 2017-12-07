package com.telenordigital.prime.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.*;
import com.telenordigital.prime.events.*;
import com.telenordigital.prime.ocs.state.OcsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FbStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(FbStorage.class);

    private final ProductDescriptionCache productCache;


    final FbDatabaseFacade facade;

    private final StorageInitiatedEventExecutor executor;

    public FbStorage(final String databaseName,
                     final String configFile,
                     final OcsState ocsState) throws StorageException {
        checkNotNull(configFile);
        checkNotNull(databaseName);
        this.executor = new StorageInitiatedEventExecutor();

        this.productCache = ProductDescriptionCacheImpl.getInstance();

        final FirebaseDatabase firebaseDatabase;
        firebaseDatabase = setupFirebaseInstance(databaseName, configFile);

        this.facade = new FbDatabaseFacade(firebaseDatabase);

        // XXX quick-fix
        // Load subscriber balance from firebase to in-memory OcsState
        loadSubscriberBalanceDataFromFirebaseToInMemoryStructure(ocsState);

        // Get listeners for various events
        final ValueEventListener productCatalogValueEventListener = newCatalogDataChangedEventListener();

        // Scoop up products left and right (and don't worry about duplicates, race conditions or
        // anything else by sending them to the listeners.
        facade.addProductCatalogListener(this::addOrUpdateProduct);
        facade.addProductCatalogValueListener(productCatalogValueEventListener);
        facade.addPurchaseEventListener(newChildListenerThatDispatchesPurchaseRequestToExecutor());
    }

    private AbstractChildEventListener newChildListenerThatDispatchesPurchaseRequestToExecutor() {
        return new AbstractChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                LOG.info("onChildAdded");
                if (snapshotIsInvalid(snapshot)) return;

                try {
                    final FbPurchaseRequest req =
                            snapshot.getValue(FbPurchaseRequest.class);
                    req.setId(snapshot.getKey());
                    req.setMillisSinceEpoch(getMillisSinceEpoch());
                    executor.onPurchaseRequest(req);
                } catch (Exception e) {
                    LOG.error("Couldn't transform req into FbPurchaseRequest", e);
                }
            }
        };
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


    private ValueEventListener newCatalogDataChangedEventListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                LOG.info("onDataChange");
                interpretDataSnapshotAsProductCatalogItem(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };
    }

    private long getMillisSinceEpoch() {
        return Instant.now().toEpochMilli();
    }

    private void addOrUpdateProduct(final DataSnapshot snapshot) {
        if (snapshotIsInvalid(snapshot)) return;

        try {
            final ProductCatalogItem item =
                    snapshot.getValue(ProductCatalogItem.class);
            if (item.getSku() != null) {
                addTopupProduct(item.getSku(), item.getNoOfBytes());
            }
            LOG.info("Just read a product catalog item: {}", item);
        } catch (Exception e) {
            LOG.error("Couldn't transform req into FbPurchaseRequest", e);
        }
    }




    private static boolean snapshotIsInvalid(final DataSnapshot snapshot) {
        if (snapshot == null) {
            LOG.error("dataSnapshot can't be null");
            return true;
        }

        if (!snapshot.exists()) {
            LOG.error("dataSnapshot must exist");
            return true;
        }
        return false;
    }

    private void loadSubscriberBalanceDataFromFirebaseToInMemoryStructure(OcsState ocsState) {
        LOG.info("Loading initial balance from storage to in-memory OcsState");
        for (final Subscriber subscriber : getAllSubscribers()) {
            LOG.info("{} - {}", subscriber.getMsisdn(), subscriber.getNoOfBytesLeft());
            if (subscriber.getNoOfBytesLeft() > 0) {
                String msisdn = subscriber.getMsisdn();
                // XXX removing '+'
                if (msisdn.charAt(0) == '+') {
                    msisdn = msisdn.substring(1);
                }
                ocsState.addDataBytes(msisdn, subscriber.getNoOfBytesLeft());
            }
        }
    }

    private FirebaseDatabase setupFirebaseInstance(String databaseName, String configFile) throws StorageException {
        try (final FileInputStream serviceAccount = new FileInputStream(configFile)) {

            final FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                    .setDatabaseUrl("https://" + databaseName + ".firebaseio.com/")
                    .build();

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

    private void interpretDataSnapshotAsProductCatalogItem(DataSnapshot snapshot) {
        if (snapshotIsInvalid(snapshot)) return;

        try {
            final ProductCatalogItem item =
                    snapshot.getValue(ProductCatalogItem.class);
            if (item.getSku() != null) {
                addTopupProduct(item.getSku(), item.getNoOfBytes());
            }
            LOG.info("Just read a product catalog item: " + item);
        } catch (Exception e) {
            LOG.error("Couldn't transform req into FbPurchaseRequest", e);
        }
    }


    @Override
    public void addPurchaseRequestListener(final PurchaseRequestListener listener) {
        checkNotNull(listener);
        executor.addPurchaseRequestListener(listener);
    }

    @Override
    public void updatedisplaydatastructure(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        final FbSubscriber subscriber = (FbSubscriber) getSubscriberFromMsisdn(msisdn);
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
    public void removeDisplayDatastructure(String msisdn) throws StorageException {
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

        final FbRecordOfPurchase purchase =
                new FbRecordOfPurchase(msisdn, sku, millisSinceEpoch);
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

    static void handleDataChange(final DataSnapshot snapshot, final CountDownLatch cdl, final Set<String> result, final String msisdn) {
        if (!snapshot.hasChildren()) {
            cdl.countDown();
        } else try {
            for (final DataSnapshot snap : snapshot.getChildren()) {
                final String key = snap.getKey();
                result.add(key);
                cdl.countDown();
            }
        } catch (Exception e) {
            LOG.error("Something happened while looking for key = " + msisdn, e);
        }
    }

    @Override
    public Subscriber getSubscriberFromMsisdn(final String msisdn) throws StorageException {
        return facade.getSubscriberFromMsisdn(msisdn);
    }


    @Override
    public void setRemainingByMsisdn(final String msisdn, final long noOfBytes) throws StorageException {
        if (msisdn == null) {
            throw new StorageException("msisdn can't be null");
        }
        if (noOfBytes < 0) {
            throw new StorageException("noOfBytes can't be negative");
        }

        final FbSubscriber sub = (FbSubscriber) getSubscriberFromMsisdn(msisdn);
        if (sub == null) {
            throw new StorageException("Unknown msisdn " + msisdn);
        }

        sub.setNoOfBytesLeft(noOfBytes);

        facade.updateAuthorativeUserData(sub);
    }

    @Override
    public String insertNewSubscriber(final String msisdn) {
        checkNotNull(msisdn);
        final FbSubscriber sub = new FbSubscriber();
        sub.setMsisdn(msisdn);
        return facade.insertNewSubscriber(sub);
    }

    @Override
    public Collection<Subscriber> getAllSubscribers() {
        return facade.getAllSubscribers();
    }
}
