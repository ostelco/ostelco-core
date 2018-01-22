package org.ostelco.heartbeats;

/**
 * When the heartbeats are failing.
 */
class HeartbeatException extends Exception {
    public HeartbeatException(final String str) {
        super(str);
    }

    public HeartbeatException(final String str, final Throwable e) {
        super(str, e);
    }
}
