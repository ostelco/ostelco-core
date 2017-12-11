package com.telenordigital.prime.firebase;

import com.telenordigital.prime.events.EventListeners;
import com.telenordigital.prime.events.EventProcessor;
import com.telenordigital.prime.events.EventProcessorException;
import com.telenordigital.prime.events.OcsBalanceUpdater;
import com.telenordigital.prime.ocs.state.OcsState;
import com.telenordigital.prime.storage.ProductDescriptionCacheImpl;
import com.telenordigital.prime.storage.Storage;
import com.telenordigital.prime.storage.StorageException;
import com.telenordigital.prime.storage.entities.NotATopupProductException;
import com.telenordigital.prime.storage.entities.PurchaseRequestImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.telenordigital.prime.events.EventProcessorTest.PAYMENT_TOKEN;
import static com.telenordigital.prime.storage.Products.DATA_TOPUP_3GB;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
                "src/test/resources/pantel-tests.json",
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
    public void insertNewSubscriberTest() throws InterruptedException, StorageException {
        assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));
    }

    @Test
    public void purchaseRequestRoundtripTest()
            throws EventProcessorException,
            StorageException,
            InterruptedException,
            NotATopupProductException {

        assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

        final CountDownLatch latch = new CountDownLatch(1);

        storage.addPurchaseRequestListener(req -> latch.countDown());

        final PurchaseRequestImpl req =
                new PurchaseRequestImpl(DATA_TOPUP_3GB, PAYMENT_TOKEN);
        req.setMsisdn(EPHERMERAL_MSISDN);

        assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

        final String prid = storage.injectPurchaseRequest(req);
        prids.add(prid);
        sleep(MINIMUM_MILLIS_TO_SLEEP_AFTER_MAKING_PURCHASE_REQUEST);

        assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

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
