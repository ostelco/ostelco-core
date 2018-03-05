package com.telenordigital.prime.events;

import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import com.telenordigital.prime.storage.PurchaseRequestListener;
import com.telenordigital.prime.storage.Storage;
import com.telenordigital.prime.storage.StorageException;
import com.telenordigital.prime.storage.entities.NotATopupProductException;
import com.telenordigital.prime.storage.entities.Product;
import com.telenordigital.prime.storage.entities.PurchaseRequest;
import com.telenordigital.prime.storage.entities.TopUpProduct;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public final class EventProcessor implements EventHandler<PrimeEvent>, Managed {

    private static final Logger LOG = LoggerFactory.getLogger(EventProcessor.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Storage storage;

    private final OcsBalanceUpdater ocsBalanceUpdater;

    public EventProcessor(
            final Storage storage,
            final OcsBalanceUpdater ocsBalanceUpdater) {
        this.storage = checkNotNull(storage);
        this.ocsBalanceUpdater = checkNotNull(ocsBalanceUpdater);
    }

    public void handlePurchaseRequest(final PurchaseRequest pr) throws EventProcessorException {
        checkNotNull(pr);
        LOG.info("Handling purchase request = " + pr);

        validatePaymentToken(pr);

        final String sku = getValidSku(pr);
        final String msisdn = getValidMsisdn(pr);

        try {
            final TopUpProduct topup = getValidTopUpProduct(pr, sku);
            handleTopupProduct(pr, msisdn, topup);
        } catch (NotATopupProductException ex) {
            LOG.info("Ignoring non-topup purchase request " + pr);
        }
    }

    private TopUpProduct getValidTopUpProduct(
            final PurchaseRequest pr,
            final String sku) throws EventProcessorException, NotATopupProductException {
        final Product product;
        product = storage.getProductForSku(sku);
        if (!product.isTopUpProject()) {
            throw new EventProcessorException("Unknown product type, must be a topup product "
                    + product.toString(), pr);
        }
        return product.asTopupProduct();
    }

    private String getValidMsisdn(final PurchaseRequest pr) throws EventProcessorException {
        final String msisdn = pr.getMsisdn();
        if (msisdn == null) {
            throw new EventProcessorException("MSISDN cannot be null", pr);
        }
        return msisdn;
    }

    private String getValidSku(final PurchaseRequest pr) throws EventProcessorException {
        final String sku = pr.getSku();
        if (sku == null) {
            throw new EventProcessorException("SKU can't be null", pr);
        }

        if (!storage.isValidSKU(sku)) {
            throw new EventProcessorException("Not a valid SKU: " + sku, pr);
        }

        return sku;
    }

    private void validatePaymentToken(final PurchaseRequest pr) throws EventProcessorException {
        final String paymentToken = pr.getPaymentToken();
        if (paymentToken == null) {
            throw new EventProcessorException("payment token cannot be null", pr);
        }
    }

    private void handleTopupProduct(
            final PurchaseRequest pr,
            final String msisdn,
            final TopUpProduct topup) throws EventProcessorException {
        try {
            LOG.info("Handling topup product = " + pr);

            storage.updateDisplayDatastructure(msisdn);
            storage.addRecordOfPurchaseByMsisdn(msisdn, pr.getSku(), pr.getMillisSinceEpoch());
            storage.removePurchaseRequestById(pr.getId());
            ocsBalanceUpdater.updateBalance(msisdn, topup.getNoOfBytes());
        } catch (StorageException e) {
            throw new EventProcessorException(e);
        }
    }

    private void setRemainingByMsisdn(
            final String msisdn,
            final long noOfBytes) throws EventProcessorException {
        try {
            storage.setRemainingByMsisdn(msisdn, noOfBytes);
            storage.updateDisplayDatastructure(msisdn);
        } catch (StorageException e) {
            throw new EventProcessorException(e);
        }
    }

    @Override
    public void onEvent(
            final PrimeEvent event,
            final long sequence,
            final boolean endOfBatch) throws Exception {


        // CREDIT_CONTROL_REQUEST is a high frequency operation. If we do want to
        // include data balance updates from this event type, then we can
        // skip 'switch' stmt since we will do 'setRemainingByMsisdn' for all cases.

        try {
            // XXX adding '+' prefix
            LOG.info("Updating data bundle balance for {} to {} bytes",
                    event.getMsisdn(), event.getBundleBytes());
            setRemainingByMsisdn("+" + event.getMsisdn(), event.getBundleBytes());
        } catch (Exception e) {
            LOG.warn("Exception handling prime event in EventProcessor", e);
        }

        /*
        switch (event.getMessageType()) {

            // Continuous updates of data consumption. High frequency!
            case CREDIT_CONTROL_REQUEST:

            // response to my request, this is the new balance.
            case TOPUP_DATA_BUNDLE_BALANCE:

            // Return the amount to the balance. The typical use-case is that the
            // user has been allocated some balance but hasn't used it, and now the device\\
            // is switched off  so the data is returned to the backend/slow storage.

            case RETURN_UNUSED_DATA_BUCKET:
            case GET_DATA_BUNDLE_BALANCE:

                // XXX adding '+' prefix
                setRemainingByMsisdn("+" + event.getMsisdn(), event.getBundleBytes());
                break;
        }
        */
    }


    @Override
    public void start() throws Exception {
        if (running.compareAndSet(false, true)) {
            addNewPurchaseRequestListener();
        }
    }

    private void addNewPurchaseRequestListener() {
        storage.addPurchaseRequestListener(new PurchaseRequestListener() {
            @Override
            public void onPurchaseRequest(final PurchaseRequest request) {
                try {
                    handlePurchaseRequest(request);
                } catch (EventProcessorException e) {
                    LOG.error("Could not handle purchase request " + request, e);
                }
            }
        });
    }

    @Override
    public void stop() throws Exception {
        // Only for completeness, don't do anything special.
    }
}
