package com.telenordigital.prime.storage;

import com.telenordigital.prime.storage.entities.PurchaseRequest;
import com.telenordigital.prime.storage.entities.Subscriber;

import java.util.Collection;

/**
 * Interface that abstracts the interactions that
 * are necessary to get/update customer data and to both
 * with respect to (slow) accounting, and (fast) provisioning.
 * Typically this interface will represent a facade towards
 * multiple specialized storage solutions.
 */
public interface Storage extends ProductDescriptionCache {
    // XXX Shouldn't extend anything I think.

    String injectPurchaseRequest(PurchaseRequest pr);

    void updateDisplayDatastructure(String msisdn) throws StorageException;

    void removeDisplayDatastructure(String msisdn) throws StorageException;

    void setRemainingByMsisdn(String msisdn, long noOfBytes) throws StorageException;

    Subscriber getSubscriberFromMsisdn(String msisdn) throws StorageException;

    String insertNewSubscriber(String msisdn) throws StorageException;

    void removeSubscriberByMsisdn(String msisdn) throws StorageException;

    void addPurchaseRequestListener(PurchaseRequestListener listener);

    String addRecordOfPurchaseByMsisdn(String ephermeralMsisdn, String sku, long now)
            throws StorageException;

    void removePurchaseRequestById(String id);

    void removeRecordOfPurchaseById(String id);

    Collection<Subscriber> getAllSubscribers();
}
