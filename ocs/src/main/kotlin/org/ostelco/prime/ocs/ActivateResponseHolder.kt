package org.ostelco.prime.ocs

import io.grpc.stub.StreamObserver
import org.ostelco.ocs.api.ActivateResponse
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Helper class to keep track of
 * [<]
 * instance in a threadsafe manner.
 */
internal class ActivateResponseHolder {

    private val readLock: Lock

    private val writeLock: Lock

    private var activateResponse: StreamObserver<ActivateResponse>? = null

    init {
        val readWriteLock = ReentrantReadWriteLock()

        this.readLock = readWriteLock.readLock()
        this.writeLock = readWriteLock.writeLock()
    }

    fun setActivateResponse(ar: StreamObserver<ActivateResponse>) {
        writeLock.lock()
        try {
            activateResponse = ar
        } finally {
            writeLock.unlock()
        }
    }

    fun onNextResponse(response: ActivateResponse) {
        readLock.lock()
        try {
            if (activateResponse != null) {
                activateResponse!!.onNext(response)
            }
        } finally {
            readLock.unlock()
        }
    }
}
