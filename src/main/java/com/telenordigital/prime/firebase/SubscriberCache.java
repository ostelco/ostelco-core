package com.telenordigital.prime.firebase;

import com.telenordigital.prime.events.Subscriber;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

// XXX Is this even used? It looks completely stupid.
final class SubscriberCache {
    private final Set<SubscriberEntry> dirtyEntries;

    private final Map<String, SubscriberEntry> map;

    public SubscriberCache() {
        this.map = new ConcurrentHashMap<>();
        this.dirtyEntries = new ConcurrentSkipListSet<>();
    }


    public void primeCache(Collection<Subscriber> allSubscribers) {
    }

    public void readLock(String msisdn) {
    }

    public void writeLock(String msisdn) {
    }

    public boolean containsSubscriber(final String msisdn) {
        return false;
    }

    public Subscriber getSubscriber(final String msisdn) {
        return null;
    }

    public void removeSubscriber(final String msisdn) {

    }

    // XXX Being pretentious
    public void unlock(final String msisdn) {

    }

     public void insertSubscriber(final Subscriber sub) {

     }
}
