package org.ostelco.prime.ocs;

import io.grpc.stub.StreamObserver;
import org.ostelco.ocs.api.ActivateResponse;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Helper class to keep track of
 * {@link io.grpc.stub.StreamObserver<org.ostelco.ocs.api.ActivateResponse>}
 * instance in a threadsafe manner.
 */
final class ActivateResponseHolder {

    private final Lock readLock;

    private final Lock writeLock;

    private StreamObserver<ActivateResponse> activateResponse;

    ActivateResponseHolder() {
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
    }

    public void setActivateResponse(final StreamObserver<ActivateResponse> ar) {
        writeLock.lock();
        try {
            activateResponse = ar;
        } finally {
            writeLock.unlock();
        }
    }

    public void onNextResponse(final ActivateResponse response) {
        readLock.lock();
        try {
            if (activateResponse != null) {
                activateResponse.onNext(response);
            }
        } finally {
            readLock.unlock();
        }
    }
}
