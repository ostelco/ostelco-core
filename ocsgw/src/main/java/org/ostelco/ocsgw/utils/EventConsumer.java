package org.ostelco.ocsgw.utils;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EventConsumer<T> implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(EventConsumer.class);

    private final ConcurrentLinkedQueue<T> queue;
    private StreamObserver<T> streamObserver;

    public EventConsumer(ConcurrentLinkedQueue<T> queue, StreamObserver<T> streamObserver) {
        this.queue = queue;
        this.streamObserver = streamObserver;
    }

    @Override
    public void run() {
        while(true) {
            consume();
            try {
                synchronized (queue) {
                    queue.wait();
                }
            } catch (InterruptedException e) {
                LOG.info("Interrupted");
                break;
            }
        }
    }

    private void consume() {
        while (!queue.isEmpty()) {
            T event = queue.poll();
            if (event != null) {
                streamObserver.onNext(event);
            }
        }
    }
}