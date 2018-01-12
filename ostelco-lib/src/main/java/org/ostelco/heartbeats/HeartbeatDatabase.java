package org.ostelco.heartbeats;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple database used to keep track of heartbeats that are
 * added and received by the process running it.
 *
 * There may be other processes elsewhere producing heartbeat requests
 * and this datbase not be confused by heartbeat requests generated
 * by other processes, in the example below by registering with
 * a heartbeat sending/receiving implementer  of the {@link Heartbeat}
 * interface.
 *
 * Typical use is:
 *
 * <code>
 *     final Consumer<String> consumer = new Consumer<String>() {
 *            {@literal @}Override
 *             public void accept(final String key) {
 *                 cache.register(key).signalCallback();
 *             }
 *      }
 *
 *     // Make sure we're called every time a heartbeat is produced.
 *     heartbeat.registerHeartbeatListener(consumer);
 * </code>
 */
final class HeartbeatDatabase {

    /**
     * Number of seconds to wait before assuming a heartbeat won't
     * be received.
     */
    private  static final long TIMEOUT_IN_SECONDS = 10L;


    /**
     * An entry for all in-flight and received heartbeats.
     */
    private final Map<String, Entry> entries;

    /**
     * A monitor used to synchronise access to the entries
     * data structure.
     */
    private final Object entriesMonitor;


    HeartbeatDatabase() {
        this.entries = new HashMap<>();
        this.entriesMonitor = new Object();
    }


    /**
     * Register a heartbeat with a particular ID.   If the
     * ID is already registerered, return the existing entry,
     * otherwise create a new one, store it, then return it.
     * @param hbId the ID of the heartbeat.
     * @return An entry object representing the heartbeat.
     */
    public Entry register(final String hbId) {
        synchronized (entriesMonitor) {
            if (entries.containsKey(hbId)) {
                return entries.get(hbId);
            } else {
                final Entry entry = new Entry(hbId);
                entries.put(hbId, entry);
                return entry;
            }
        }
    }

    /**
     * Delete the keyed entry, and all other entries that are timed out.
     * @param key Unique key for heartbeat
     */
    public void purge(final String key) {
        purge(key, System.currentTimeMillis() - (TIMEOUT_IN_SECONDS * 1000));
    }

    /**
     * Delete the keyed entry, and all other entries that are timed out.
     * @param key Unique key for heartbeat
     * @param timeout the timeout for when entries should time out, in milliseconds
     *                since epoch.
     */
    private void purge(final String key, final long timeout) {
        checkNotNull(key);
        synchronized (entriesMonitor) {
            final Collection<String> keysToDelete = new HashSet<>();
            keysToDelete.add(key);
            entries.values().stream().
                    filter(entry -> entry.timestamp < timeout).map(entry -> entry.key).
                    forEach(keysToDelete::add);

            keysToDelete.forEach(entries::remove);
        }
    }

    /**
     * Internal entry class, main purpose is await and signal
     * callbacks.   The timestamp entries are used when purging old
     * instances (timed out).
     */
    public final static class Entry {

        /**
         * The time at which the heartbeat was first entered into the
         * database.
         */
        private final long timestamp;

        /**
         * An unique key identifying the heartbeat.
         */
        private final String key;

        /**
         * A count down latch, counting down from one.
         * The intent is that this latch is created when the entry
         * is created, and counted down when a callback is received
         * through the heartbeat protocol.
         */
        private final CountDownLatch cdl;


        /**
         * Create a new entry for storage in the database.
         * @param ts A timestamp, milliseconds since epoch.
         * @param key Key uniquely identifying the heartbeat.
         */
        Entry(final long ts, final String key) {
            this.timestamp = ts;
            this.key = key;
            this.cdl = new CountDownLatch(1);
        }


        /**
         * Create a new entry for storage in the database.
         * Timestamp will be time of invocation for this constructor.
         * @param key Key uniquely identifying the heartbeat.
         */
        Entry(final String key) {
            this(System.currentTimeMillis(), key);
        }


        /**
         * Hash of the key.
         * @return a hash of the key.
         */
        @Override
        public int hashCode() {
            return key.hashCode();
        }


        /**
         * Check for equality by comparing keys.
         * @param obj Another object.
         * @return true iff the other object has a key identical to this.
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Entry && ((Entry) obj).key.equals(key);
        }


        /**
         * Await callback, return true iff the callback happened within
         * the timeout limit.
         * @return True if callback was received in a timely manner.
         */
        public boolean awaitCallback() throws HeartbeatException {
            try {
                return cdl.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new HeartbeatException("Interrupted ", e);
            }
        }

        /**
         * Signal that at a callback has occurred.
         */
        public void signalCallback() {
            cdl.countDown();
        }
    }
}
