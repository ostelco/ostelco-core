package com.telenordigital.prime.firebase;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.telenordigital.prime.storage.ProductCatalogItem;
import com.telenordigital.prime.storage.StorageException;
import com.telenordigital.prime.storage.entities.PurchaseRequest;
import com.telenordigital.prime.storage.entities.PurchaseRequestImpl;
import com.telenordigital.prime.storage.entities.Subscriber;
import com.telenordigital.prime.storage.entities.SubscriberImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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

    private static final String PHONE_NUMBER = "phoneNumber";

    private static final String MSISDN = "msisdn";

    private final DatabaseReference authorativeUserData;

    private final DatabaseReference clientRequests;

    private final DatabaseReference clientVisibleSubscriberRecords;

    private final DatabaseReference recordsOfPurchase;

    private final DatabaseReference quickBuyProducts;

    private final DatabaseReference products;

    FbDatabaseFacade(final FirebaseDatabase firebaseDatabase) {
        checkNotNull(firebaseDatabase);

        this.authorativeUserData = firebaseDatabase.getReference("authorative-user-storage");
        this.clientRequests = firebaseDatabase.getReference("client-requests");
        this.clientVisibleSubscriberRecords = firebaseDatabase.getReference("profiles");
        this.recordsOfPurchase = firebaseDatabase.getReference("records-of-purchase");

        // Used to listen in on new products from the Firebase product catalog.
        this.quickBuyProducts = firebaseDatabase.getReference("quick-buy-products");
        this.products = firebaseDatabase.getReference("products");
    }

    private ValueEventListener newCatalogDataChangedEventListener(
            final Consumer<ProductCatalogItem> consumer) {
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

    private static AbstractChildEventListener  listenerForPurchaseRequests(
            final BiFunction<String, PurchaseRequestImpl, Void> consumer) {
        checkNotNull(consumer);
        return new AbstractChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot snapshot, final String previousChildName) {
                if (snapshotIsInvalid(snapshot)) {
                    return;
                }
                try {
                    final PurchaseRequestImpl req =
                            snapshot.getValue(PurchaseRequestImpl.class);
                    consumer.apply(snapshot.getKey(), req);
                } catch (Exception e) {
                    LOG.error("Couldn't dispatch purchase request to consumer", e);
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
            LOG.error("Couldn't transform req into PurchaseRequestImpl", e);
        }
    }

    public void addProductCatalogItemListener(final Consumer<ProductCatalogItem> consumer) {
        addProductCatalogItemChildListener(consumer);
        addProductCatalogValueListener(consumer);
    }

    public void addPurchaseRequestListener(
            final BiFunction<String, PurchaseRequestImpl, Void> consumer) {
        addPurchaseEventListener(listenerForPurchaseRequests(consumer));
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
        final ValueEventListener productCatalogValueEventListener =
                newCatalogDataChangedEventListener(consumer);
        addProductCatalogValueListener(productCatalogValueEventListener);
    }


    public void addProductCatalogValueListener(
            final ValueEventListener productCatalogValueEventListener) {
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
            LOG.error("Could not find entry for phoneNumber = "
                    + msisdn
                    + " Not updating user visible storage");
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
                        // XXX This is unclean, fix!
                        FbStorage.handleDataChange(snapshot, cdl, result, msisdn);
                    }

                    @Override
                    public void onCancelled(final DatabaseError error) {
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
            throw new StorageException(e);
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

    public void removeByMsisdn(final String msisdn) throws StorageException {
        removeByMsisdn(clientVisibleSubscriberRecords, msisdn);
    }

    public String injectPurchaseRequest(final PurchaseRequest pr) {
        final PurchaseRequestImpl cr = (PurchaseRequestImpl) pr;
        final DatabaseReference dbref = clientRequests.push();
        final Map<String, Object> crAsMap = cr.asMap();
        dbref.setValue(crAsMap);
        return dbref.getKey();
    }

    public void removeRecordOfPurchaseById(final String id) {
        removeChild(recordsOfPurchase, id);
    }

    private void removeChild(final DatabaseReference db, final String childId) {
        // XXX Removes whole tree, not just the subtree for id.
        //     how do I fix this?
        checkNotNull(db);
        checkNotNull(childId);
        db.child(childId).getRef().removeValue();
    }

    public void removePurchaseRequestById(final String id) {
        checkNotNull(id);
        removeChild(clientRequests, id);
    }

    public Subscriber getSubscriberFromMsisdn(final String msisdn) throws StorageException {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Set<Subscriber> result = new HashSet<>();

        final Query q = authorativeUserData.orderByChild(MSISDN).equalTo(msisdn).limitToFirst(1);

        final ValueEventListener listenerThatWillReadSubcriberData =
                newListenerThatWillReadSubcriberData(cdl, result);

        q.addListenerForSingleValueEvent(
                listenerThatWillReadSubcriberData);

        return waitForSubscriberData(msisdn, cdl, result);
    }

    private String logSubscriberDataProcessing(
            final String msisdn,
            final String userData,
            final String result) {
        final String msg = "authorativeuserdata = '" + userData
                + "', msisdn = '" + msisdn
                + "' => " + result;
        LOG.info(msg);
        return msg;
    }

    private Subscriber waitForSubscriberData(
            final String msisdn,
            final CountDownLatch cdl,
            final Set<Subscriber> result) throws StorageException {
        final String userDataString = authorativeUserData.toString();
        try {
            if (!cdl.await(10, TimeUnit.SECONDS)) {
                final String msg = logSubscriberDataProcessing(
                        msisdn, userDataString, "timeout");
                throw new StorageException(msg);
            } else if (result.isEmpty()) {
                logSubscriberDataProcessing(msisdn, userDataString, "null");
                return null;
            } else {
                final Subscriber r = result.iterator().next();
                logSubscriberDataProcessing(msisdn, userDataString, r.toString());
                return r;
            }
        } catch (InterruptedException e) {
            final String msg = logSubscriberDataProcessing(msisdn, userDataString, "interrupted");
            throw new StorageException(msg, e);
        }
    }

    private ValueEventListener newListenerThatWillReadSubcriberData(
            final CountDownLatch cdl,
            final Set<Subscriber> result) {
        return new AbstractValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {
                if (!snapshot.hasChildren()) {
                    cdl.countDown();
                    return;
                } else {
                    for (final DataSnapshot snap : snapshot.getChildren()) {
                        final SubscriberImpl sub =
                                snap.getValue(SubscriberImpl.class);
                        final String key = snap.getKey();
                        sub.setFbKey(key);
                        result.add(sub);
                        cdl.countDown();
                    }
                }
            }
        };
    }

    public void updateAuthorativeUserData(final SubscriberImpl sub) {
        checkNotNull(sub);
        final DatabaseReference dbref = authorativeUserData.child(sub.getFbKey());
        dbref.updateChildren(sub.asMap());
    }

    public String insertNewSubscriber(final SubscriberImpl sub) {
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
                    final SubscriberImpl subscriber = child.getValue(SubscriberImpl.class);
                    subscriber.setFbKey(child.getKey());
                    subscribers.add(subscriber);
                }
                cdl.countDown();
            }

            @Override
            public void onCancelled(final DatabaseError error) {
                LOG.error(error.getMessage(), error.toException());
            }
        };
    }

    public ChildEventListener newProductDefChangedListener(
            final Consumer<DataSnapshot> snapshotConsumer) {
        return new AbstractChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot snapshot, final String previousChildName) {
                snapshotConsumer.accept(snapshot);
            }

            @Override
            public void onChildChanged(final DataSnapshot snapshot, final String previousChildName) {
                snapshotConsumer.accept(snapshot);
            }
        };
    }
}
