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

public final class InnerFbStorage implements Storage {


    private static final Logger LOG = LoggerFactory.getLogger(InnerFbStorage.class);

    public void addTopupProduct(String sku, long noOfBytes) {
        productCache.addTopupProduct(sku, noOfBytes);
    }

    public boolean isValidSKU(String sku) {
        return productCache.isValidSKU(sku);
    }

    public Product getProductForSku(String sku) {
        return productCache.getProductForSku(sku);
    }

    private ProductDescriptionCache productCache = ProductDescriptionCacheImpl.getInstance(); // XXX

    private final DatabaseReference authorativeUserData;
    private final DatabaseReference clientRequests;
    private final DatabaseReference clientVisibleSubscriberRecords;
    private final DatabaseReference recordsOfPurchase;
    private final DatabaseReference quickBuyProducts;
    private final DatabaseReference products;

    private final FirebaseDatabase firebaseDatabase;

    private final String databaseName;
    private final String configFile;

    private final StorageInitiatedEventExecutor executor;

    public InnerFbStorage(final String databaseName,
                          final String configFile,
                          final OcsState ocsState) throws StorageException {
        this.configFile = checkNotNull(configFile);
        this.databaseName = checkNotNull(databaseName);
        this.executor = new StorageInitiatedEventExecutor();
        this.firebaseDatabase = setupFirebaseInstance(databaseName, configFile);

        // XXX Read this, then fix something that reports connectivity status through the
        //     health mechanism.
        // https://www.firebase.com/docs/web/guide/offline-capabilities.html#section-connection-state
        // this.firebaseDatabase.getReference("/.info/connected").addValueEventListener()

        this.authorativeUserData = firebaseDatabase.getReference("authorative-user-storage");
        this.clientRequests = firebaseDatabase.getReference("client-requests");
        this.clientVisibleSubscriberRecords = firebaseDatabase.getReference("profiles");
        this.recordsOfPurchase = firebaseDatabase.getReference("records-of-purchase");

        // XXX quick-fix
        // Load subscriber balance from firebase to in-memory OcsState
        loadSubscriberBalanceDataFromFirebaseToInMemoryStructure(ocsState);

        // Used to listen in on new products from the Firebase product catalog.
        this.quickBuyProducts = firebaseDatabase.getReference("quick-buy-products");
        this.products = firebaseDatabase.getReference("products");

        // Get listeners for various events
        final ValueEventListener productCatalogValueEventListener = newCatalogDataChangedEventListner();
        final ChildEventListener productCatalogListener = newProductDefChangedListener();


        // Scoop up products left and right (and don't worry about duplicates, race conditions or
        // anything else by sending them to the listeners.
        this.quickBuyProducts.addChildEventListener(productCatalogListener);
        this.quickBuyProducts.addValueEventListener(productCatalogValueEventListener);
        this.products.addChildEventListener(productCatalogListener);
        this.products.addValueEventListener(productCatalogValueEventListener);

        addPurchaseEventListener();
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

    private void addPurchaseEventListener() {
        this.clientRequests.addChildEventListener(
                new AbstractChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                        LOG.info("onChildAdded");
                        if (snapshotIsInvalid(snapshot)) return;

                        try {
                            final FbPurchaseRequest req =
                                    snapshot.getValue(FbPurchaseRequest.class);
                            req.setId(snapshot.getKey());
                            req.setMillisSinceEpoch(Instant.now().toEpochMilli());
                            executor.onPurchaseRequest(req);
                        } catch (Exception e) {
                            LOG.error("Couldn't transform req into FbPurchaseRequest", e);
                        }
                    }
                });
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
        for (Subscriber subscriber : getAllSubscribers()) {
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

        final Map<String, Object> displayRep = new HashMap<>();
        displayRep.put("phoneNumber", subscriber.getMsisdn());

        // XXX This is both:
        //     a) A layering violation, since it mixes a backend server/information
        //        layer with actual text formatting in an frontend/UX layer.
        //     b) Possibly also a good idea, since it makes it really quick to change
        //        the UI on a  per-user basis if we so desire.
        final long noOfBytes = subscriber.getNoOfBytesLeft();
        final float noOfGBLeft = noOfBytes / 1000000000.0f;
        final String gbLeft = String.format("%.2f GB", noOfGBLeft);

        final String key = getKeyFromPhoneNumber(clientVisibleSubscriberRecords, msisdn);
        if (key == null) {
            LOG.error("Could not find entry for phoneNumber = " + msisdn + " Not updating user visible storage");
            return;
        }

        displayRep.put("usage", gbLeft);

        clientVisibleSubscriberRecords
                .child(key)
                .updateChildren(displayRep);
    }

    @Override
    public void removeDisplayDatastructure(String msisdn) throws StorageException {
        checkNotNull(msisdn);
        removeByMsisdn(clientVisibleSubscriberRecords, msisdn);
    }


    @Override
    public String injectPurchaseRequest(final PurchaseRequest pr) {
        checkNotNull(pr);
        FbPurchaseRequest cr = (FbPurchaseRequest) pr;
        final DatabaseReference dbref = clientRequests.push();
        final Map<String, Object> crAsMap = cr.asMap();
        dbref.setValue(crAsMap);
        return dbref.getKey();
    }

    @Override
     public void removeRecordOfPurchaseById(final String id) {
        removeChild(recordsOfPurchase, id);
    }

    @Override
    public String addRecordOfPurchaseByMsisdn(
            final String msisdn,
            final String sku,
            final long millisSinceEpoch) throws StorageException {
        checkNotNull(msisdn);

        final FbRecordOfPurchase purchase =
                new FbRecordOfPurchase(msisdn, sku, millisSinceEpoch);

        final DatabaseReference dbref = recordsOfPurchase.push();
        final Map<String, Object> asMap = purchase.asMap();
        dbref.updateChildren(asMap);
        return dbref.getKey();
    }

    private void removeChild(final DatabaseReference db, final String childId) {
        // XXX Removes whole tree, not just the subtree for id.
        //     how do I fix this?
        checkNotNull(db);
        checkNotNull(childId);
        db.child(childId).getRef().removeValue();
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

    @Override
    public void removeSubscriberByMsisdn(final String msisdn) throws StorageException {
        checkNotNull(msisdn);
        removeByMsisdn(authorativeUserData, msisdn);
    }


    @Override
    public void removePurchaseRequestById(final String id) {
        checkNotNull(id);
        removeChild(clientRequests, id);
    }

    private String getKeyFromLookupKey(DatabaseReference dbref, String msisdn, String lookupKey) throws StorageException {
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

    public String getKeyFromPhoneNumber(final DatabaseReference dbref, final String msisdn) throws StorageException {

        final String lookupKey = "phoneNumber";
        return getKeyFromLookupKey(dbref, msisdn, lookupKey);
    }

    public String getKeyFromMsisdn(final DatabaseReference dbref, final String msisdn) throws StorageException {
        final String lookupKey = "msisdn";
        return getKeyFromLookupKey(dbref, msisdn, lookupKey);
    }

    private void handleDataChange(DataSnapshot snapshot, CountDownLatch cdl, Set<String> result, String msisdn) {
        if (!snapshot.hasChildren()) {
            cdl.countDown();
            return;
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
        final CountDownLatch cdl = new CountDownLatch(1);
        final Set<Subscriber> result = new HashSet<>();
        LOG.info("authorativeuserdata = '" + authorativeUserData + "', msisdn = '" + msisdn + "'");

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

        final DatabaseReference dbref = authorativeUserData.child(sub.getFbKey());
        dbref.updateChildren(sub.asMap());
    }

    @Override
    public String insertNewSubscriber(final String msisdn) {
        checkNotNull(msisdn);
        final FbSubscriber sub = new FbSubscriber();
        sub.setMsisdn(msisdn);
        final DatabaseReference dbref = authorativeUserData.push();
        sub.setFbKey(dbref.getKey());
        dbref.updateChildren(sub.asMap());
        return dbref.getKey();
    }

    public Collection<Subscriber> getAllSubscribers() {

        final Query q = authorativeUserData.orderByKey();

        final Set<Subscriber> subscribers = new LinkedHashSet<>();


        final CountDownLatch cdl = new CountDownLatch(1);
        // one time listener
        final ValueEventListener listener = new ValueEventListener() {
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
        q.addListenerForSingleValueEvent(listener);
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
}
