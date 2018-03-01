package org.ostelco.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import org.ostelco.dropwizard.firebase.FirebaseSetupException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class SingleValueEventReader<T> {

    private final static long TIMEOUT = 10L;
    private final AtomicReference<DataSnapshot> snapshot = new AtomicReference<>();
    private final AtomicReference<DatabaseError> theError = new AtomicReference<>();
    private final CountDownLatch cdl = new CountDownLatch(1);
    private final Class<T> returnType;
    private final ValueEventListener valueEventListener;

    public SingleValueEventReader(final Class<T> clazz) {
        checkNotNull(clazz);
        this.returnType = clazz;
        this.valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {
                SingleValueEventReader.this.snapshot.set(snapshot);
                cdl.countDown();
            }

            @Override
            public void onCancelled(final DatabaseError error) {
                theError.set(error);
                cdl.countDown();
            }
        };
    }

    public ValueEventListener getListener() {
       return this.valueEventListener;
    }

    public DataSnapshot getSnapshot() throws FirebaseSetupException {
        try {
            if (cdl.await(TIMEOUT, TimeUnit.SECONDS)) {
                return snapshot.get();
            } else {
                throw new FirebaseSetupException("getSubscription timed out");
            }
        } catch (InterruptedException e) {
            throw new FirebaseSetupException("getSubscription was interrupted", e);
        }
    }

    public T getValue() throws FirebaseSetupException {
        final DataSnapshot dataSnapshot = getSnapshot();
        if (dataSnapshot == null) {
            throw new FirebaseSetupException("Data snapshot was null, can't unpack that");
        }

        if (dataSnapshot.exists()) {
            return dataSnapshot.getValue(returnType);
        } else {
            return null;
        }
    }
}
