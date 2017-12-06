package com.telenordigital.prime.firebase;

import com.telenordigital.prime.events.*;
import com.telenordigital.prime.ocs.state.OcsState;
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

import static com.telenordigital.prime.events.Products.DATA_TOPUP_3GB;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class FbPurchaseEventRoundtripTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private final static String EPHERMERAL_MSISDN = "+4747116996";

    private InnerFbStorage fbStorage;
    private Storage storage;


    @Mock
    OcsBalanceUpdater ocsBalanceUpdater;

    private EventProcessor processor;
    private Collection<String> prids;
    private PurchaseRequest req;

    @Before
    public void setUp() throws  Exception {
        this.fbStorage = new InnerFbStorage(
                "pantel-tests",
                "src/test/resources/pantel-tests.json" ,
                new OcsState());
        this.storage = fbStorage;
        sleep(3000);
        storage.removeSubscriberByMsisdn(EPHERMERAL_MSISDN);
        storage.insertNewSubscriber(EPHERMERAL_MSISDN);

        this.processor = new EventProcessor(storage, ocsBalanceUpdater);
        this.processor.start();
        this.prids = new ArrayList<>();

        req = new PurchaseRequest() {
            @Override
            public String getSku() {
                return ProductDescriptionCacheImpl.getInstance().DATA_TOPUP_3GB.getSku();
            }

            @Override
            public String getPaymentToken() {
                return PAYMENT_TOKEN;
            }

            @Override
            public String getMsisdn() {
                return EPHERMERAL_MSISDN;
            }

            @Override
            public long getMillisSinceEpoch() {
                return 0;
            }

            @Override
            public String getId() {
                return "godzilla";
            }
        };
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
    public void purchaseRequestRoundtripTest() throws EventProcessorException, StorageException, InterruptedException {

        assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

        final CountDownLatch latch = new CountDownLatch(1);

        storage.addPurchaseRequestListener(new PurchaseRequestListener() {
            @Override
            public void onPurchaseRequest(final PurchaseRequest req) {
                latch.countDown();
            }
        });

        final FbPurchaseRequest cr =
                new FbPurchaseRequest(DATA_TOPUP_3GB, PAYMENT_TOKEN);
        cr.setMsisdn(EPHERMERAL_MSISDN);

        assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

        final String prid = storage.injectPurchaseRequest(cr);
        prids.add(prid);
        sleep(3000);

        assertNotEquals(null, storage.getSubscriberFromMsisdn(EPHERMERAL_MSISDN));

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Read/react failed");
        }

        final long topupBytes = ProductDescriptionCacheImpl.getInstance().DATA_TOPUP_3GB.asTopupProduct().getTopUpInBytes();

        // Then verify
        verify(ocsBalanceUpdater).updateBalance(eq(EPHERMERAL_MSISDN), eq(topupBytes));

        // XXX Verification of data stored in firebase not verified.
    }
}
