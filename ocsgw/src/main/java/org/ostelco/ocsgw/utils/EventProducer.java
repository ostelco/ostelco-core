package org.ostelco.ocsgw.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EventProducer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(EventProducer.class);

    private final ConcurrentLinkedQueue<T> queue;

    public EventProducer(ConcurrentLinkedQueue<T> queue) {
        this.queue = queue;
    }

    public void queueEvent(T event) {
        try {
            queue.add(event);
            synchronized (queue) {
                queue.notifyAll();
            }
        } catch (NullPointerException e) {
            LOG.error("Failed to queue Event", e);
        }
    }
}