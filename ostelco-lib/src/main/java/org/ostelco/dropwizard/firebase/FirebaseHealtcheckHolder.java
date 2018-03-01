package org.ostelco.dropwizard.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.ostelco.dropwizard.healthcheck.AtomicBooleanHealtcheck;
import org.ostelco.firebase.AbstractValueEventListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class that will construct and hold a {@Link HealthCheck} instance
 * that can be used to check for connectivity to a firebase realtime
 * database.
 */
public final class FirebaseHealtcheckHolder {

    private static final Logger LOG =
            getLogger(FirebaseHealtcheckHolder.class);

    /**
     * True iff the checker is already attached to a firebase instance.
     * If it isn't then the status of the healtcheck will be false.
     */
    private final AtomicBoolean isAttached;

    /**
     * The actual healthcheck instance produced by this class.  This healtcheck
     * is intended to be used by consumers of healtchecks.
     */
    private final HealthCheck healthCheck;

    /**
     * Reflects the state of the connection to the Realtime Database. True
     * iff the connection is up and active.
     */
    private final AtomicBoolean isConnected;


    public FirebaseHealtcheckHolder() {
        this.isConnected = new AtomicBoolean(false);
        this.isAttached = new AtomicBoolean(false);
        this.healthCheck =
                new AtomicBooleanHealtcheck(isConnected, "Firebase connectivity is down");
    }


    /**
     * Attaches to a firebase database and will listen to changes
     * to the magic ".info/connected" destination in the realtime database
     * to check if it is attached or not.
     */
    public void attachTo(final FirebaseDatabase firebaseDatabase) throws FirebaseHealthcheckException {

        if (!isAttached.compareAndSet(false, true)) {
            throw new FirebaseHealthcheckException("Healthcheck already attached");
        }

        final DatabaseReference connectedRef =
                firebaseDatabase.getReference(".info/connected");

        connectedRef.addValueEventListener(new AbstractValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {
                final Object value = snapshot.getValue();
                try {
                    isConnected.getAndSet((Boolean) value);
                } catch (Exception ex) {
                    LOG.error("Could not convert to Boolean: " + value, ex);
                }
            }
        });
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }
}
