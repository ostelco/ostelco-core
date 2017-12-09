package com.telenordigital.prime.firebase;

import com.google.firebase.database.*;
import com.telenordigital.prime.events.ProductCatalogItem;
import com.telenordigital.prime.events.PurchaseRequest;
import com.telenordigital.prime.events.StorageException;
import com.telenordigital.prime.events.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Presenting a facade for the many and  varied firebase databases
 * we're using.
 */
public final class FbDatabaseFacade {

    private static final Logger LOG = LoggerFactory.getLogger(FbDatabaseFacade.class);
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String MSISDN = "msisdn";

    private final DatabaseReference authorativeUserData;
    private final DatabaseReference clientRequests;
    private final DatabaseReference clientVisibleSubscriberRecords;
    private final DatabaseReference recordsOfPurchase;
    private final DatabaseReference quickBuyProducts;
    private final DatabaseReference products;

    FbDatabaseFacade(final FirebaseDatabase firebaseDatabase) {
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

    private ValueEventListener newCatalogDataChangedEventListener(final Consumer<ProductCatalogItem> consumer) {
        checkNotNull(consumer);
        return new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {
                LOG.info("onDataChange");
                interpretDataSnapshotAsProductCatalogItem(snapshot, consumer);
            }

            @Override
            public void onCancelled(final DatabaseError error) {
                // Intentionally left blank.
            }
        };
    }

    private static AbstractChildEventListener
    newChildListenerThatDispatchesPurchaseRequestToExecutor(
            final BiFunction<String, FbPurchaseRequest, Void> consumer) {
        checkNotNull(consumer);
        return new AbstractChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot snapshot, final String previousChildName) {
                LOG.info("onChildAdded");
                if (snapshotIsInvalid(snapshot)) {
                    return;
                }
                try {
                    final FbPurchaseRequest req =
                            snapshot.getValue(FbPurchaseRequest.class);
                    consumer.apply(snapshot.getKey(), req);
                } catch (Exception e) {
                    LOG.error("Couldn't transform req into FbPurchaseRequest", e);
                }
            }
        };
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

    private void interpretDataSnapshotAsProductCatalogItem(
            final DataSnapshot snapshot, Consumer<ProductCatalogItem> consumer) {
        checkNotNull(consumer);
        if (snapshotIsInvalid(snapshot)) {
            return;
        }

        try {
            final ProductCatalogItem item =
                    snapshot.getValue(ProductCatalogItem.class);
            if (item.getSku() != null) {
                consumer.accept(item);
            }
            LOG.info("Just read a product catalog item: " + item);
        } catch (Exception e) {
            LOG.error("Couldn't transform req into ProductCatalogItem", e);
        }
    }


    private void addOrUpdateProduct(
            final DataSnapshot snapshot,
            final Consumer<ProductCatalogItem> consumer) {
        checkNotNull(consumer);
        if (snapshotIsInvalid(snapshot)) {
            return;
        }

        try {
            final ProductCatalogItem item =
                    snapshot.getValue(ProductCatalogItem.class);
            if (item.getSku() != null) {
                consumer.accept(item);
            }
            LOG.info("Just read a product catalog item: {}", item);
        } catch (Exception e) {
            LOG.error("Couldn't transform req into FbPurchaseRequest", e);
        }
    }

    public void addProductCatalogItemListener(final Consumer<ProductCatalogItem> consumer) {
        addProductCatalogItemChildListener(consumer);
        addProductCatalogValueListener(consumer);
    }

    public void addPurchaseRequestListener(BiFunction<String, FbPurchaseRequest, Void> consumer) {
        addPurchaseEventListener(newChildListenerThatDispatchesPurchaseRequestToExecutor(consumer));
    }


    public void addProductCatalogItemChildListener(final Consumer<ProductCatalogItem> consumer) {
        checkNotNull(consumer);
        final ChildEventListener productCatalogListener =
                newProductDefChangedListener(snapshot -> addOrUpdateProduct(snapshot, consumer));
        addProductCatalogListener(productCatalogListener);
    }

    public void addProductCatalogListener(final Consumer<DataSnapshot> consumer) {
        checkNotNull(consumer);
        final ChildEventListener productCatalogListener =
                newProductDefChangedListener(consumer);
        addProductCatalogListener(productCatalogListener);
    }


    public void addProductCatalogListener(final ChildEventListener productCatalogListener) {
        checkNotNull(productCatalogListener);
        this.quickBuyProducts.addChildEventListener(productCatalogListener);
        this.products.addChildEventListener(productCatalogListener);
    }

    public void addProductCatalogValueListener(final Consumer<ProductCatalogItem> consumer) {
        checkNotNull(consumer);
        final ValueEventListener productCatalogValueEventListener = newCatalogDataChangedEventListener(consumer);
        addProductCatalogValueListener(productCatalogValueEventListener);
    }


    public void addProductCatalogValueListener(final ValueEventListener productCatalogValueEventListener) {
        checkNotNull(productCatalogValueEventListener);
        this.quickBuyProducts.addValueEventListener(productCatalogValueEventListener);
        this.products.addValueEventListener(productCatalogValueEventListener);
    }

    public void addPurchaseEventListener(final ChildEventListener cel) {
        checkNotNull(cel);
        this.clientRequests.addChildEventListener(cel);
    }

    public String pushRecordOfPurchaseByMsisdn(final Map<String, Object> asMap) {
        checkNotNull(asMap);
        final DatabaseReference dbref = recordsOfPurchase.push();

        dbref.updateChildren(asMap);
        return dbref.getKey();
    }

    public void updateClientVisibleUsageString(
            final String msisdn,
            final String gbLeft) throws StorageException {
        checkNotNull(msisdn);
        checkNotNull(gbLeft);
        final String key = getKeyFromPhoneNumber(clientVisibleSubscriberRecords, msisdn);
        if (key == null) {
            LOG.error("Could not find entry for phoneNumber = " +
                    msisdn +
                    " Not updating user visible storage");
            return;
        }

        final Map<String, Object> displayRep = new HashMap<>();
        displayRep.put(PHONE_NUMBER, msisdn);

        displayRep.put("usage", gbLeft);

        clientVisibleSubscriberRecords.
                child(key).
                updateChildren(displayRep);
    }

    public void removeSubscriberByMsisdn(final String msisdn) throws StorageException {
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


    private String getKeyFromLookupKey(
            final DatabaseReference dbref,
            final String msisdn,
            final String lookupKey) throws StorageException {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Set<String> result = new HashSet<>();
        dbref.orderByChild(lookupKey).
                equalTo(msisdn).
                limitToFirst(1).
                addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot snapshot) {
                        FbStorage.handleDataChange(snapshot, cdl, result, msisdn);  // XXX This is unclean, fix!
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Empty on purpose.
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

    private String getKeyFromPhoneNumber(
            final DatabaseReference dbref,
            final String msisdn) throws StorageException {
        return getKeyFromLookupKey(dbref, msisdn, PHONE_NUMBER);
    }

    private String getKeyFromMsisdn(
            final DatabaseReference dbref,
            final String msisdn) throws StorageException {
        return getKeyFromLookupKey(dbref, msisdn, MSISDN);
    }

    public void removeByMsisdn(String msisdn) throws StorageException {
        removeByMsisdn(clientVisibleSubscriberRecords, msisdn);
    }

    public String injectPurchaseRequest(final PurchaseRequest pr) {
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

        final Query q = authorativeUserData.orderByChild(MSISDN).equalTo(msisdn).limitToFirst(1);

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
                throw new StorageException("Query timed out. authorativeuserdata = '" +
                        authorativeUserData +
                        "', msisdn = '" +
                        msisdn + "'");
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

    public void updateAuthorativeUserData(final FbSubscriber sub) {
        checkNotNull(sub);
        final DatabaseReference dbref = authorativeUserData.child(sub.getFbKey());
        dbref.updateChildren(sub.asMap());
    }

    public String insertNewSubscriber(final FbSubscriber sub) {
        checkNotNull(sub);
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

    private static ValueEventListener newListenerThatWillCollectAllSubscribers(
            final Set<Subscriber> subscribers,
            final CountDownLatch cdl) {
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

    public ChildEventListener newProductDefChangedListener(Consumer<DataSnapshot> snapshotConsumer) {
        return new AbstractChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot snapshot, final String previousChildName) {
                LOG.info("onChildAdded");
                snapshotConsumer.accept(snapshot);
            }

            @Override
            public void onChildChanged(final DataSnapshot snapshot, final String previousChildName) {
                LOG.info("onChildChanged");
                snapshotConsumer.accept(snapshot);
            }
        };
    }
}
