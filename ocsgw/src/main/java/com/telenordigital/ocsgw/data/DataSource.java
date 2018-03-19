package com.telenordigital.ocsgw.data;


import com.telenordigital.ostelco.diameter.CreditControlContext;

/**
 * Interface to interact with a datasource.
 *
 */
public interface DataSource {

    /**
     *  Initiates the data source
     */
    void init();

    /**
     * Forward a new initial/update/terminate request.
     *
     * @param context That holds the request and session
     */
    void handleRequest(CreditControlContext context);

    /**
     * Check if a subscriber is on the blockedlist
     *
     * @param msisdn Subscriber msisdn to check
     */
    boolean isBlocked(final String msisdn);
}
