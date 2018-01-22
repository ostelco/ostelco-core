package org.ostelco.dropwizard.firebase;

import com.codahale.metrics.health.HealthCheck;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.FirebaseDatabase;
import io.dropwizard.lifecycle.Managed;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ManagedFirebaseDatabase implements Managed {

    /**
     * Name where  configuration for firebase is stored.
     */
    private final String configFile;

    /**
     * Name of firebase database we will be using.
     */
    private final String databaseName;

    private final FirebaseHealtcheckHolder healthCheckHolder;

    private FirebaseDatabase fdb;


    /**
     * Count down latches to keep track of those waiting for this
     * instance to start up.  Beware of deadlocks, you're on your own.
     */
    private final Collection<CountDownLatch> cdls = new ArrayList<>();


    /**
     * A monitor used to synchronize access to the firebaseDatabase field.
     */
    private final Object firebaseDatabaseMonitor;

    public ManagedFirebaseDatabase(final String databaseName,
                            final String configFile) {
        this.healthCheckHolder = new FirebaseHealtcheckHolder();
        this.configFile = checkNotNull(configFile);
        this.databaseName = checkNotNull(databaseName);
        this.firebaseDatabaseMonitor = new Object();
    }

    /**
     * Create a connection to the database and return that.
     *
     * @param databaseName Database  name.
     * @param configFile   Config file for access to the database.
     * @return The FirebaseDatabase instance used  for the access.
     * @throws FirebaseSetupException If a connection to firebase somehow couldn't be established.
     */
    private FirebaseDatabase setupFirebaseInstance(
            final String databaseName,
            final String configFile) throws FirebaseSetupException {
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
            // this.firebaseDatabase.setLogLevel(
            // com.google.firebase.database.Logger.Level.DEBUG);

        } catch (IOException ex) {
            throw new FirebaseSetupException(ex);
        }
    }


    @Override
    public void start() throws Exception {
        synchronized(firebaseDatabaseMonitor) {
            if (this.fdb != null) {
                return;
            }
            this.fdb = setupFirebaseInstance(databaseName, configFile);
            try {
                healthCheckHolder.attachTo(this.fdb);
            } catch (FirebaseHealthcheckException e) {
                throw new FirebaseSetupException("Could not attach to health check holder", e);
            }
            for (final CountDownLatch cdl : cdls) {
                cdl.countDown();
            }
        }
    }

    /**
     * Run when service is stopped. Does not do anything, necessary to fulfill
     * contract for interface {@link io.dropwizard.lifecycle.Managed}.
     * @throws Exception
     */
    @Override
    public void stop() throws Exception {
    }

    /**
     * Return a {@link com.codahale.metrics.health.HealthCheck} instance to
     * be used by Dropwizard (and others) to check if the connection to the
     * backend server is up or not.
     *
     * @return A healthcheck.
     */
    public HealthCheck getHealthCheck() {
        return healthCheckHolder.getHealthCheck();
    }

    /**
     * Get the realtime database associated with this managed database instance.
     * @return Database instance.
     */
    public FirebaseDatabase getRealtimeDatabase() throws ManagedFirebaseException {
        synchronized (firebaseDatabaseMonitor) {
            if (fdb != null) {
                return fdb;
            } else {
                throw new ManagedFirebaseException("Attempt to get realtime database instance from a stopped "
                        + this.getClass().getName() + " instance");
            }
        }
    }

    /**
     * Wait until  start has been invoked on this instance, then return.
     */
    public void waitUntilStarted() throws InterruptedException {
        final CountDownLatch cdl;
        synchronized(firebaseDatabaseMonitor) {
            if (fdb == null) {
                cdl = new CountDownLatch(1);
                cdls.add(cdl);
            } else {
                cdl = null;
            }
        }
        /// XXX Wait forever if necessary
        if (cdl != null) {
            cdl.await(10, TimeUnit.SECONDS);
        }
    }
}
