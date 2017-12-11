package com.telenordigital.prime.storage;

import com.telenordigital.prime.storage.entities.PurchaseRequest;
import com.telenordigital.prime.storage.entities.PurchaseRequestImpl;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

public class StorageInitiatedEventExecutorTest {

    private StorageInitiatedEventExecutor executor =
            new StorageInitiatedEventExecutor();

    @Test
    public void testRoundtrip() throws Exception {

        final CountDownLatch cdl = new CountDownLatch(1);
        final PurchaseRequest req = new PurchaseRequestImpl();

        executor.addPurchaseRequestListener(new PurchaseRequestListener() {
            @Override
            public void onPurchaseRequest(final PurchaseRequest request) {
                if (request == req) {
                    cdl.countDown();
                } else {
                    fail("Got the wrong purchase request.  How did that happen?");
                }
            }
        });
        executor.onPurchaseRequest(req);

        assertTrue(cdl.await(2, TimeUnit.SECONDS));
    }
}