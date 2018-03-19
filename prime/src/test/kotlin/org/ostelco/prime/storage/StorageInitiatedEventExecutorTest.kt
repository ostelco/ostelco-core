package org.ostelco.prime.storage

import org.junit.Test
import org.ostelco.prime.storage.entities.PurchaseRequest
import org.ostelco.prime.storage.entities.PurchaseRequestImpl

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail

class StorageInitiatedEventExecutorTest {

    private val executor = StorageInitiatedEventExecutor()

    @Test
    @Throws(Exception::class)
    fun testRoundtrip() {

        val cdl = CountDownLatch(1)
        val req = PurchaseRequestImpl()

        executor.addPurchaseRequestListener(object : PurchaseRequestListener {
            override fun onPurchaseRequest(request: PurchaseRequest) {
                if (req == request) {
                    cdl.countDown()
                } else {
                    fail("Got the wrong purchase request.  How did that happen?")
                }
            }
        })
        executor.onPurchaseRequest(req)

        assertTrue(cdl.await(2, TimeUnit.SECONDS))
    }
}

