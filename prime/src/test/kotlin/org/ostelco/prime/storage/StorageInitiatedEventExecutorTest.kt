package org.ostelco.prime.storage

import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import org.junit.Test
import org.ostelco.prime.model.PurchaseRequest
import org.ostelco.prime.storage.legacy.PurchaseRequestHandler
import org.ostelco.prime.storage.firebase.StorageInitiatedEventExecutor
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StorageInitiatedEventExecutorTest {

    private val executor = StorageInitiatedEventExecutor()

    @Test
    @Throws(Exception::class)
    fun testRoundtrip() {

        val cdl = CountDownLatch(1)
        val req = PurchaseRequest("sku",
                "paymentToken",
                "msisdn",
                Instant.now().toEpochMilli(),
                "id")

        executor.addPurchaseRequestHandler(object : PurchaseRequestHandler {
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

