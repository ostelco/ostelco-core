package org.ostelco.prime.firebase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ostelco.prime.events.EventListeners;
import org.ostelco.prime.ocs.OcsState;
import org.ostelco.prime.storage.Storage;
import org.ostelco.prime.storage.StorageException;
import org.ostelco.prime.storage.entities.PurchaseRequestImpl;
import org.ostelco.prime.storage.entities.Subscriber;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.ostelco.prime.storage.Products.DATA_TOPUP_3GB;

public class FbStorageTest {

    private static final String PAYMENT_TOKEN = "thisIsAPaymentToken";

    private static final String EPHERMERAL_MSISDN = "+4747116996";

    private static final int MILLIS_TO_WAIT_WHEN_STARTING_UP = 3000;

    private static final long RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS = 92L;

    private static final int TIMEOUT_IN_SECONDS = 10;

    private FbStorage fbStorage;

    private Storage storage;

    private Collection<String> prids;

    @Before
    public void setUp() throws StorageException, InterruptedException {
        this.fbStorage = new FbStorage(
                "pantel-tests",
                "src/integration-tests/resources/pantel-tests.json" ,
                new EventListeners(new OcsState()));
        this.storage = fbStorage;
        sleep(MILLIS_TO_WAIT_WHEN_STARTING_UP);
        storage.removeSubscriberByMsisdn(EPHERMERAL_MSISDN);
        storage.insertNewSubscriber(EPHERMERAL_MSISDN);
        this.prids = new ArrayList<>();
    }

    @After
    public void cleanUp() throws StorageException {
        storage.removeSubscriberByMsisdn(EPHERMERAL_MSISDN);
        for (final String prid : prids) {
            storage.removePurchaseRequestById(prid);
        }
    }

    @Test
    public void getStorageByMsisdnTest() throws StorageException {
        final Subscriber subscriberByMsisdn = storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN);
        assertNotEquals(null, subscriberByMsisdn);
        assertEquals(EPHERMERAL_MSISDN, subscriberByMsisdn.getMsisdn());
    }

    @Test
    public void insertNewSubscriberTest() throws StorageException {
        Assert.assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));
    }

    @Test
    public void setRemainingByMsisdnTest() throws StorageException {
        storage.setRemainingByMsisdn(
                EPHERMERAL_MSISDN,
                RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS);
        Assert.assertEquals(RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS,
                storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN).getNoOfBytesLeft());
        storage.setRemainingByMsisdn(EPHERMERAL_MSISDN, 0);
        Assert.assertEquals(0L,
                storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN).getNoOfBytesLeft());
    }

    @Test
    public void updateDisplayDatastructureTest() throws StorageException {
        storage.setRemainingByMsisdn(EPHERMERAL_MSISDN,
                RANDOM_NO_OF_BYTES_TO_USE_BY_REMAINING_MSISDN_TESTS);
        storage.updateDisplayDatastructure(EPHERMERAL_MSISDN);
        // XXX  Some verification missing, but it looks like the right thing
    }

    @Test
    public void  addRecordOfPurchaseByMsisdnTest() throws StorageException {
        final long now;
        now = Instant.now().toEpochMilli();
        final String id =
                storage.addRecordOfPurchaseByMsisdn(
                        EPHERMERAL_MSISDN,
                        DATA_TOPUP_3GB.getSku(),
                        now);
        storage.removeRecordOfPurchaseById(id);
    }

    @Test
    public void testWriteThenReactToUpdateRequest() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(2);

        storage.addPurchaseRequestListener(req -> {
            assertNotEquals(null, req);
            assertEquals(PAYMENT_TOKEN, req.getPaymentToken());
            assertEquals(DATA_TOPUP_3GB.getSku(), req.getSku());
            latch.countDown();
        });

        final PurchaseRequestImpl cr =
                new PurchaseRequestImpl(DATA_TOPUP_3GB, PAYMENT_TOKEN);
        final String id = fbStorage.injectPurchaseRequest(cr);
        final String id2 = fbStorage.injectPurchaseRequest(cr);
        prids.add(id);
        prids.add(id2);

        if (!latch.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
            fail("Read/react failed");
        }

        storage.removePurchaseRequestById(id);
        storage.removePurchaseRequestById(id2);
    }
}
