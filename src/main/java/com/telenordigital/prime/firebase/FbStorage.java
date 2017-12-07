package com.telenordigital.prime.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.telenordigital.prime.events.Product;
import com.telenordigital.prime.events.ProductDescriptionCache;
import com.telenordigital.prime.events.ProductDescriptionCacheImpl;
import com.telenordigital.prime.events.PurchaseRequest;
import com.telenordigital.prime.events.PurchaseRequestListener;
import com.telenordigital.prime.events.Storage;
import com.telenordigital.prime.events.StorageException;
import com.telenordigital.prime.events.Subscriber;
import com.telenordigital.prime.ocs.state.OcsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FbStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(FbStorage.class);

    private final ProductDescriptionCache productCache;


    /**
     * Presenting a fascade for the many and  varied firebase databases
     * we're using.
     */
    public  final static class FbDatabaseFacade {

        private final DatabaseReference authorativeUserData;
        private final DatabaseReference clientRequests;
        private final DatabaseReference clientVisibleSubscriberRecords;
        private final DatabaseReference recordsOfPurchase;
        private final DatabaseReference quickBuyProducts;
        private final DatabaseReference products;

        FbDatabaseFacade(FirebaseDatabase firebaseDatabase) {
            checkNotNull(firebaseDatabase);
            // XXX Read this, then fix something that reports connectivity status through the
            //     health mechanism.
            // https://www.firebase.com/docs/web/guide/offline-capabilities.html#section-connection-state
            // this.firebaseDatabase.getReference("/.info/connected").addValueEventListener()

            this.authorativeUserData = firebaseDatabase.getReference("authorative-user-storage");
            this.clientRequests = firebaseDatabase.getReference("client-requests");
            this.clientVisibleSubscriberRecords = firebaseDatabase.getReference("profiles");
            this.recordsOfPurchase = firebaseDatabase.getReference("records-of-purchase");

            // Used to listen in on new products from the Firebase product catalog.
            this.quickBuyProducts = firebaseDatabase.getReference("quick-buy-products");
            this.products = firebaseDatabase.getReference("products");
        }

        public void addProductCatalogListener(final ChildEventListener productCatalogListener) {
            checkNotNull(productCatalogListener);
            this.quickBuyProducts.addChildEventListener(productCatalogListener);
            this.products.addChildEventListener(productCatalogListener);
        }

        public void addProductCatalogValueListener(ValueEventListener productCatalogValueEventListener) {
            checkNotNull(productCatalogValueEventListener);
            this.quickBuyProducts.addValueEventListener(productCatalogValueEventListener);
            this.products.addValueEventListener(productCatalogValueEventListener);
        }

        public void addPurchaseEventListener(final ChildEventListener cel) {
            checkNotNull(cel);
            this.clientRequests.addChildEventListener(cel);
        }

        public String pushRecordOfPurchaseByMsisdn(Map<String, Object> asMap) {

            final DatabaseReference dbref = recordsOfPurchase.push();

            dbref.updateChildren(asMap);
            return dbref.getKey();
        }

        public void updateClientVisibleUsageString(String msisdn, String gbLeft) throws StorageException {
            final String key = getKeyFromPhoneNumber(clientVisibleSubscriberRecords, msisdn);
            if (key == null) {
                LOG.error("Could not find entry for phoneNumber = " + msisdn + " Not updating user visible storage");
                return;
            }

            final Map<String, Object> displayRep = new HashMap<>();
            displayRep.put("phoneNumber", msisdn);

            displayRep.put("usage", gbLeft);

            clientVisibleSubscriberRecords
                    .child(key)
                    .updateChildren(displayRep);
        }

        public void removeSubscriberByMsisdn(String msisdn)  throws StorageException{
            removeByMsisdn(authorativeUserData, msisdn);
        }

        private void removeByMsisdn(
                final DatabaseReference dbref,
                final String msisdn) throws StorageException {
            checkNotNull(msisdn);
            checkNotNull(dbref);
            final String key = getKeyFromMsisdn(dbref, msisdn);
            if (key != null) {
                removeChild(dbref, key);
            }
        }


        private  String getKeyFromLookupKey(final DatabaseReference dbref, String msisdn, String lookupKey) throws StorageException {
            final CountDownLatch cdl = new CountDownLatch(1);
            final Set<String> result = new HashSet<>();
            dbref.orderByChild(lookupKey)
                    .equalTo(msisdn)
                    .limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot snapshot) {
                    handleDataChange(snapshot, cdl, result, msisdn);
                }

                @Override
                public void onCancelled(DatabaseError error) {

                }
            });

            try {
                if (!cdl.await(10, TimeUnit.SECONDS)) {
                    throw new StorageException("Query timed out");
                } else if (result.isEmpty()) {
                    return null;
                } else {
                    return result.iterator().next();
                }
            } catch (InterruptedException e) {
                throw new StorageException("Interrupted", e);
            }
        }

        private  String getKeyFromPhoneNumber(final DatabaseReference dbref, final String msisdn) throws StorageException {
            final String lookupKey = "phoneNumber";
            return getKeyFromLookupKey(dbref, msisdn, lookupKey);
        }

        private  String getKeyFromMsisdn(final DatabaseReference dbref, final String msisdn) throws StorageException {
            final String lookupKey = "msisdn";
            return getKeyFromLookupKey(dbref, msisdn, lookupKey);
        }

        public void removeByMsisdn(String msisdn) throws StorageException {
            removeByMsisdn(clientVisibleSubscriberRecords, msisdn);
        }

        public String injectPurchaseRequest(PurchaseRequest pr) {
            final FbPurchaseRequest cr = (FbPurchaseRequest) pr;
            final DatabaseReference dbref = clientRequests.push();
            final Map<String, Object> crAsMap = cr.asMap();
            dbref.setValue(crAsMap);
            return dbref.getKey();
        }

        public void removeRecordOfPurchaseById(String id) {
            removeChild(recordsOfPurchase, id);
        }

        private void removeChild(final DatabaseReference db, final String childId) {
            // XXX Removes whole tree, not just the subtree for id.
            //     how do I fix this?
            checkNotNull(db);
            checkNotNull(childId);
            db.child(childId).getRef().removeValue();
        }

        public void removePurchaseRequestById(String id) {
            checkNotNull(id);
            removeChild(clientRequests, id);
        }

        public Subscriber getSubscriberFromMsisdn(String msisdn) throws StorageException {
            final CountDownLatch cdl = new CountDownLatch(1);
            final Set<Subscriber> result = new HashSet<>();

            final Query q = authorativeUserData.orderByChild("msisdn").equalTo(msisdn).limitToFirst(1);

            final ValueEventListener listenerThatWillReadSubcriberData = new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot snapshot) {

                    if (!snapshot.hasChildren()) {
                        cdl.countDown();
                        return;
                    } else {
                        for (final DataSnapshot snap : snapshot.getChildren()) {

                            final FbSubscriber sub =
                                    snap.getValue(FbSubscriber.class);
                            final String key = snap.getKey();

                            sub.setFbKey(key);
                            result.add(sub);
                            cdl.countDown();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                }
            };

            q.addListenerForSingleValueEvent(
                    listenerThatWillReadSubcriberData);

            try {
                if (!cdl.await(10, TimeUnit.SECONDS)) {
                    LOG.info("authorativeuserdata = '" + authorativeUserData
                            + "', msisdn = '" + msisdn
                            + "' => timeout");
                    throw new StorageException("Query timed out. authorativeuserdata = '" + authorativeUserData + "', msisdn = '" + msisdn + "'");
                } else if (result.isEmpty()) {
                    LOG.info("authorativeuserdata = '" + authorativeUserData
                            + "', msisdn = '" + msisdn
                            + "' => null");
                    return null;
                } else {
                    final Subscriber r = result.iterator().next();
                    LOG.info("authorativeuserdata = '" + authorativeUserData + "', msisdn = '" + msisdn + "' => " + r);
                    return r;
                }
            } catch (InterruptedException e) {
                LOG.info("authorativeuserdata = '" + authorativeUserData + "', msisdn = '" + msisdn + "' => interrupted");
                throw new StorageException("Interrupted", e);
            }
        }

        public void updateAuthorativeUserData(FbSubscriber sub) {
            final DatabaseReference dbref = authorativeUserData.child(sub.getFbKey());
            dbref.updateChildren(sub.asMap());
        }

        public String insertNewSubscriber(FbSubscriber sub) {
            final DatabaseReference dbref = authorativeUserData.push();
            sub.setFbKey(dbref.getKey());
            dbref.updateChildren(sub.asMap());
            return dbref.getKey();
        }

        public Collection<Subscriber> getAllSubscribers() {
            final Query q = authorativeUserData.orderByKey();
            final Set<Subscriber> subscribers = new LinkedHashSet<>();
            final CountDownLatch cdl = new CountDownLatch(1);
            final ValueEventListener collectingVisitor =
                    newListenerThatWillCollectAllSubscribers(subscribers, cdl);

            q.addListenerForSingleValueEvent(collectingVisitor);

            try {
                cdl.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.error("Failed to get all subscribers");
            }
            if (cdl.getCount() > 0) {
                LOG.error("Failed to get all subscribers");
            }
            return subscribers;
        }

        private  static ValueEventListener newListenerThatWillCollectAllSubscribers(final Set<Subscriber> subscribers, final CountDownLatch cdl) {
            checkNotNull(subscribers);
            checkNotNull(cdl);
            return new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot snapshot) {
                    if (!snapshot.hasChildren()) {
                        cdl.countDown();
                        return;
                    }
                    for (final DataSnapshot child : snapshot.getChildren()) {
                        final FbSubscriber subscriber = child.getValue(FbSubscriber.class);
                        subscriber.setFbKey(child.getKey());
                        subscribers.add(subscriber);
                    }
                    cdl.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    LOG.error(error.getMessage(), error.toException());
                }
            };
        }
    }


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
        final ValueEventListener productCatalogValueEventListener = newCatalogDataChangedEventListner();
        final ChildEventListener productCatalogListener = newProductDefChangedListener();

        // Scoop up products left and right (and don't worry about duplicates, race conditions or
        // anything else by sending them to the listeners.
        facade.addProductCatalogListener(productCatalogListener);
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


    private ValueEventListener newCatalogDataChangedEventListner() {
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

    private ChildEventListener newProductDefChangedListener() {
        return new AbstractChildEventListener() {
                @Override
                public void onChildAdded(final DataSnapshot snapshot, final String previousChildName) {
                    LOG.info("onChildAdded");
                    addOrUpdateProduct(snapshot);
                }

                @Override
                public void onChildChanged(final DataSnapshot snapshot, final String previousChildName) {
                    LOG.info("onChildChanged");
                    addOrUpdateProduct(snapshot);
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
            };
    }

    private boolean snapshotIsInvalid(DataSnapshot snapshot) {
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

    private static void handleDataChange(final DataSnapshot snapshot, final CountDownLatch cdl, final Set<String> result, final String msisdn) {
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
