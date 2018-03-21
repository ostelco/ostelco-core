package org.ostelco.prime.firebase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.ostelco.prime.events.EventListeners;
import org.ostelco.prime.events.EventProcessor;
import org.ostelco.prime.events.EventProcessorException;
import org.ostelco.prime.events.EventProcessorTest;
import org.ostelco.prime.events.OcsBalanceUpdater;
import org.ostelco.prime.ocs.OcsState;
import org.ostelco.prime.storage.ProductDescriptionCacheImpl;
import org.ostelco.prime.storage.Storage;
import org.ostelco.prime.storage.StorageException;
import org.ostelco.prime.storage.entities.NotATopupProductException;
import org.ostelco.prime.storage.entities.PurchaseRequestImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.ostelco.prime.storage.Products.DATA_TOPUP_3GB;

public final class FbPurchaseEventRoundtripTest {

    private static final String EPHERMERAL_MSISDN = "+4747116996";

    private static final int MINIMUM_MILLIS_TO_SLEEP_AFTER_MAKING_PURCHASE_REQUEST = 3000;

    private static final int SECONDS_TO_WAIT_FOR_SUBSCRIPTION_PROCESSING_TO_FINISH = 10;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    public OcsBalanceUpdater ocsBalanceUpdater;

    private Collection<String> prids;

    private FbStorage fbStorage;

    private Storage storage;

    @Before
    public void setUp() throws Exception {
        this.fbStorage = new FbStorage(
                "pantel-tests",
                "src/integration-tests/resources/pantel-tests.json",
                new EventListeners(new OcsState()));
        this.storage = fbStorage;
        final int millisToSleepDuringStartup = 3000;
        sleep(millisToSleepDuringStartup);
        storage.removeSubscriberByMsisdn(EPHERMERAL_MSISDN);
        storage.insertNewSubscriber(EPHERMERAL_MSISDN);

        final EventProcessor processor = new EventProcessor(storage, ocsBalanceUpdater);
        processor.start();
        this.prids = new ArrayList<>();
    }

    @After
    public void cleanUp() throws StorageException {
        if (storage != null) {
            storage.removeSubscriberByMsisdn(EPHERMERAL_MSISDN);
        }

        if (this.prids != null) {
            for (final String prid : this.prids) {
                fbStorage.removePurchaseRequestById(prid);
            }
        }
    }

    @Test
    public void insertNewSubscriberTest() throws StorageException {
        Assert.assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));
    }

    @Test
    public void purchaseRequestRoundtripTest()
            throws EventProcessorException,
            StorageException,
            InterruptedException,
            NotATopupProductException {

        Assert.assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

        final CountDownLatch latch = new CountDownLatch(1);

        storage.addPurchaseRequestListener(req -> latch.countDown());

        final PurchaseRequestImpl req =
                new PurchaseRequestImpl(DATA_TOPUP_3GB, EventProcessorTest.PAYMENT_TOKEN);
        req.setMsisdn(EPHERMERAL_MSISDN);

        Assert.assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

        final String prid = storage.injectPurchaseRequest(req);
        prids.add(prid);
        sleep(MINIMUM_MILLIS_TO_SLEEP_AFTER_MAKING_PURCHASE_REQUEST);

        Assert.assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

        if (!latch.await(SECONDS_TO_WAIT_FOR_SUBSCRIPTION_PROCESSING_TO_FINISH, TimeUnit.SECONDS)) {
            fail("Read/react failed");
        }

        final long topupBytes = ProductDescriptionCacheImpl.
                DATA_TOPUP_3GB.
                asTopupProduct().
                getNoOfBytes();

        // Then verify
        verify(ocsBalanceUpdater).updateBalance(eq(EPHERMERAL_MSISDN), eq(topupBytes));

        // XXX Verification of data stored in firebase not verified.
    }
}
