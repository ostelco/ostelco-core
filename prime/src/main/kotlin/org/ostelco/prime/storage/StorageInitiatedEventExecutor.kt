package org.ostelco.prime.storage

import org.ostelco.prime.storage.entities.PurchaseRequest
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class StorageInitiatedEventExecutor {
    private val executor: ExecutorService

    private val monitor = Any()

    private val purchaseRequestListeners: MutableSet<PurchaseRequestListener>

    init {
        val tf = ThreadProducer()
        this.executor = Executors.newCachedThreadPool(tf)
        this.purchaseRequestListeners = HashSet()
    }

    fun addPurchaseRequestListener(listener: PurchaseRequestListener) {

        synchronized(monitor) {
            purchaseRequestListeners.add(listener)
        }
    }

    private inner class ThreadProducer : ThreadFactory {

        private val tf = Executors.defaultThreadFactory()

        override fun newThread(r: Runnable): Thread {
            val t = tf.newThread(r)
            t.name = "FbstorageEventHandler"
            return t
        }
    }

    fun onPurchaseRequest(req: PurchaseRequest) {
        synchronized(monitor) {
            for (l in purchaseRequestListeners) {
                applyPurchaseRequestThroughExecutor(req, l)
            }
        }
    }

    private fun applyPurchaseRequestThroughExecutor(
            req: PurchaseRequest,
            l: PurchaseRequestListener) {
        executor.execute { l.onPurchaseRequest(req) }
    }
}
