package com.telenordigital.prime.firebase;

import com.telenordigital.prime.events.Subscriber;

import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

final class SubscriberEntry {
    private final Subscriber subscriber;
    private final ReentrantLock lock;

    public SubscriberEntry(final Subscriber subscriber) {
        this.subscriber = checkNotNull(subscriber);
        this.lock = new ReentrantLock();
    }
}
