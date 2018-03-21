package org.ostelco.prime.events;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.ostelco.prime.disruptor.PrimeEvent;
import org.ostelco.prime.storage.PurchaseRequestListener;
import org.ostelco.prime.storage.Storage;
import org.ostelco.prime.storage.StorageException;
import org.ostelco.prime.storage.entities.NotATopupProductException;
import org.ostelco.prime.storage.entities.PurchaseRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ostelco.prime.disruptor.PrimeEventMessageType.GET_DATA_BUNDLE_BALANCE;
import static org.ostelco.prime.disruptor.PrimeEventMessageType.RELEASE_RESERVED_BUCKET;
import static org.ostelco.prime.storage.Products.DATA_TOPUP_3GB;

public final class EventProcessorTest {

    public static final String PAYMENT_TOKEN = "a weird token";

    private static final String MSISDN = "12345678";

    private static final String PLUS_USED_TO_BEGIN_INTERNATIONAL_PREFIX_IN_MSISSDN = "+";

    private static final long NO_OF_BYTES = 4711L;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    public Storage storage;

    @Mock
    public OcsBalanceUpdater ocsBalanceUpdater;

    private EventProcessor processor;

    @Before
    public void setUp() {
        when(storage.isValidSKU(DATA_TOPUP_3GB.getSku())).
                thenReturn(true);

        when(storage.getProductForSku(DATA_TOPUP_3GB.getSku())).
                thenReturn(DATA_TOPUP_3GB);

        this.processor = new EventProcessor(storage, ocsBalanceUpdater);
        this.processor.start();
    }

    @Test
    public void handlePurchaseRequest() {
    }

    @Test
    public void onEvent() {
    }

    private static final class DummyPurchaseRequest implements PurchaseRequest {

        @Override
        public String getSku() {
            return DATA_TOPUP_3GB.getSku();
        }

        @Override
        public String getPaymentToken() {
            return PAYMENT_TOKEN;
        }

        @Override
        public String getMsisdn() {
            return MSISDN;
        }

        @Override
        public long getMillisSinceEpoch() {
            return 0;
        }

        @Override
        public String getId() {
            return "Sir Tristram, violer d'amores";
        }
    }

    @Test
    public void handlePurchaseRequestTest() throws EventProcessorException, StorageException {

        final PurchaseRequest req;
        req = new DummyPurchaseRequest();

        // Process a little
        processor.handlePurchaseRequest(req);

        // Then verify that the appropriate actions has been performed.
        final long topupBytes;
        try {
            topupBytes = DATA_TOPUP_3GB.asTopupProduct().getNoOfBytes();
        } catch (NotATopupProductException ex) {
            throw new EventProcessorException("Programming error, this shouldn't happen", ex);
        }

        verify(storage).addPurchaseRequestListener(any(PurchaseRequestListener.class));
        verify(storage).addRecordOfPurchaseByMsisdn(eq(MSISDN), eq(req.getSku()), anyLong());
        verify(storage).updateDisplayDatastructure(eq(MSISDN));
        verify(storage).removePurchaseRequestById(eq(req.getId()));
        verify(ocsBalanceUpdater).updateBalance(eq(MSISDN), eq(topupBytes));
    }

    @Test
    public void testPrimeEventReleaseReservedDataBucket() throws Exception {
        final long noOfBytes = 4711L;
        final PrimeEvent primeEvent = new PrimeEvent();
        primeEvent.setMessageType(RELEASE_RESERVED_BUCKET);
        primeEvent.setMsisdn(MSISDN);
        primeEvent.setBundleBytes(noOfBytes);

        processor.onEvent(primeEvent, 0L, false);

        verify(storage).setRemainingByMsisdn(eq(
                PLUS_USED_TO_BEGIN_INTERNATIONAL_PREFIX_IN_MSISSDN + MSISDN), eq(noOfBytes));
    }

    @Test
    public void testPrimeEventGetDataBundleBalance() throws StorageException{
        final PrimeEvent primeEvent = new PrimeEvent();
        primeEvent.setMessageType(GET_DATA_BUNDLE_BALANCE);
        primeEvent.setMsisdn(MSISDN);
        primeEvent.setBundleBytes(NO_OF_BYTES);

        processor.onEvent(primeEvent, 0L, false);

        // Verify a little.
        final String inernationalMsisdn =
                PLUS_USED_TO_BEGIN_INTERNATIONAL_PREFIX_IN_MSISSDN + MSISDN;
        verify(storage).setRemainingByMsisdn(eq(inernationalMsisdn),
                eq(NO_OF_BYTES));
    }

    // XXX Are we missing an event type here?
}
