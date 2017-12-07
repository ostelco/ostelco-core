package com.telenordigital.prime.firebase;

import com.telenordigital.prime.events.Subscriber;

import java.util.Collection;

/**
 * XXX This class is a leftover from a busy week.  It does not do
 * anything, it should either be removed, or have actual functionality
 * added to it.
 */
final class SubscriberCache {
    // private final Set<SubscriberEntry> dirtyEntries;

    // private final Map<String, SubscriberEntry> map;

    public SubscriberCache() {
        // this.map = new ConcurrentHashMap<>();
        // this.dirtyEntries = new ConcurrentSkipListSet<>();
    }


    public void primeCache(Collection<Subscriber> allSubscribers) {
        // XXX To be written, either delete class or actually do that.
    }

    public void readLock(String msisdn) {
        // XXX To be written, either delete class or actually do that.
    }

    public void writeLock(String msisdn) {
        // XXX To be written, either delete class or actually do that.
    }

    public boolean containsSubscriber(final String msisdn) {
        return false;
    }

    public Subscriber getSubscriber(final String msisdn) {
        return null;
    }

    public void removeSubscriber(final String msisdn) {
        // XXX To be written, either delete class or actually do that.
    }

    // XXX Being pretentious
    public void unlock(final String msisdn) {
        // XXX To be written, either delete class or actually do that.
    }

    public void insertSubscriber(final Subscriber sub) {
        // XXX To be written, either delete class or actually do that.
    }
}
