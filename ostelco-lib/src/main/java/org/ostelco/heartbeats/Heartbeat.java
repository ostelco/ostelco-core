package org.ostelco.heartbeats;

import java.util.function.Consumer;

/**
 * Helper class to facilitate sending/receiving a heartbeat
 * for sending and receiving "heartbeats" to/from Firebase.
 * <p>
 * The general idea is to send a heartbeat, the listen for the
 * database to perform a callback indicating that the heartbeat
 * has been received, and then deleting the heartbeat record in
 * the database.
 * <p>
 * If all of this goes well, then it's safe to assume that the
 * database is available and functioning reasonably well.
 */
public interface Heartbeat {

    /**
     * Write a heartbeat record to the database, return an unique ID
     * for the record.
     *
     * @return Unique record for this particular heartbeat.
     */
    String sendHeartbeat() throws HeartbeatException;


    /**
     * Register a listener method that will invoke the consumer
     * whenever a heartbeat is received.
     *
     * @param consumer A consumer of strings.
     */
    void registerHeartbeatListener(Consumer<String> consumer) throws HeartbeatException;

    /**
     * Delete the record corresponding to a particular heartbeat.
     *
     * @param id id of a heartbeat.
     */
    void deleteHeartbeat(final String id) throws HeartbeatException;
}
