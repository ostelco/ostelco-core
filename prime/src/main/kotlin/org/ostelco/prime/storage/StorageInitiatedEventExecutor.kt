package org.ostelco.prime.storage

import org.ostelco.prime.model.PurchaseRequest
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class StorageInitiatedEventExecutor {
    private val executor: ExecutorService

    private val monitor = Any()

    private val purchaseRequestHandlers: MutableSet<PurchaseRequestHandler>

    init {
        val tf = ThreadProducer()
        this.executor = Executors.newCachedThreadPool(tf)
        this.purchaseRequestHandlers = HashSet()
    }

    fun addPurchaseRequestHandler(handler: PurchaseRequestHandler) {

        synchronized(monitor) {
            purchaseRequestHandlers.add(handler)
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
            for (handler in purchaseRequestHandlers) {
                applyPurchaseRequestThroughExecutor(req, handler)
            }
        }
    }

    private fun applyPurchaseRequestThroughExecutor(
            req: PurchaseRequest,
            handler: PurchaseRequestHandler) {
        executor.execute { handler.onPurchaseRequest(req) }
    }
}
