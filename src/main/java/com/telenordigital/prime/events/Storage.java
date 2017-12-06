package com.telenordigital.prime.events;

/**
 * Interface that abstracts the interactions that
 * are necessary to get/update customer data and to both
 * with respect to (slow) accounting, and (fast) provisioning.
 * Typically this interface will represent a fascade towards
 * multiple specialized storage solutions.
 */
public interface Storage  extends ProductDescriptionCache {  // XXX Shouldn't extend anything I think.

    String injectPurchaseRequest(final PurchaseRequest pr);

    void updatedisplaydatastructure(String msisdn) throws StorageException;

    void removeDisplayDatastructure(String msisdn) throws StorageException;

    void setRemainingByMsisdn(final String msisdn, final long noOfBytes) throws StorageException;

    Subscriber getSubscriberFromMsisdn(final String msisdn) throws StorageException;

    String insertNewSubscriber(final String msisdn) throws StorageException;

    void removeSubscriberByMsisdn(String msisdn) throws StorageException;

    void addPurchaseRequestListener(final PurchaseRequestListener listener);

    String addRecordOfPurchaseByMsisdn(String ephermeralMsisdn, String sku, long now) throws StorageException;

    void removePurchaseRequestById(final String id);

    void removeRecordOfPurchaseById(final String id);
}

