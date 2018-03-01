package org.ostelco.heartbeats;

import java.util.function.Consumer;

/**
 * Implement a Dropwizard healthcheck based on heartbeats.
 * The heartbeat protocol consists of sending a heartbeat downstream,
 * and then listening for a reply through a callback.   If the
 * reply is received within a set
 * timeout (presently ten seconds), then the system is assumed to be
 * healthy, otherwise it is assumed to be unhealthy.
 */
public final class HeartbeatHealtcheck extends HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatHealtcheck.class);

    /**
     * Timeout in seconds to wait for the return of a heartbeat before
     * assuming failure.
     */
    private final static long TIMEOUT_IN_SECONDS = 10L;

    /**
     * The instance in the other end of the heartbeat healthcheck.
     * Must obey the heartbeat protocol as
     */
    private final Heartbeat heartbeat;

    /**
     * A database of all heartbeats either sent into flight, or
     * received from the heartbeat  instance, but not yet purged.
     */
    private final HeartbeatDatabase heartbeatDatabase;

    /**
     * Create a  new healthcheck instance connected to an instance
     * implementing the {@link Heartbeat} interface.
     * @param heartbeat The heartbeat instance
     * @throws HeartbeatException
     *    If creating the healtcheck instance didn't work out.
     */
    public HeartbeatHealtcheck(final Heartbeat heartbeat)
            throws HeartbeatException {

        this.heartbeat = checkNotNull(heartbeat);
        this.heartbeatDatabase = new HeartbeatDatabase();

        // A callback that is called everything the heartbeat
        // instance detects an incoming heartbeat.
        final Consumer<String> consumer =
                key -> heartbeatDatabase.register(key).signalCallback();

        // Make sure we're called every time a heartbeat is produced.
        heartbeat.registerHeartbeatListener(consumer);
    }

    /**
     * Wait for a particular heartbeat to be received from the
     * heartbeat instance.  Return true if the heartbeat was
     * received within the timeout limit, otherwise return false.
     * @param key Key to the heartbeat.
     * @return True if the heartbeat succeeded.
     */
    private boolean waitForHb(final String key) {
        checkNotNull(key);
        final HeartbeatDatabase.Entry e =
                heartbeatDatabase.register(key);
        try {
            return e.awaitCallback();
        } catch (HeartbeatException ex) {
            LOG.info("heartbeat failed", ex);
            return false;
        } finally {
            heartbeatDatabase.purge(key);
        }
    }


    /**
     * Helper method.  If returning true the heartbeat succeeded in
     * sending and receiving a heartbeat, if false then not.
     * @return True iff heartbeat succeeded.
     */
    private boolean heartIsBeating() {
        try {
            final String hbId = heartbeat.sendHeartbeat();
            return waitForHb(hbId);
        } catch (HeartbeatException e) {
            return false;
        }
    }

    /**
     * Implementation if the healthcheck protocol by using the heartbeat
     * protocol.
     * @return A result indicating if the heartbeat timed out or not.
     */
    @Override
    protected Result check() {
        if (!heartIsBeating()) {
            return Result.unhealthy("Heartbeat timed out. Timeout value is " +
                    TIMEOUT_IN_SECONDS +
                    " seconds");
        }
        return Result.healthy();
    }
}
