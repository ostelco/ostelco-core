package org.ostelco.ocsgw.datasource

import org.ostelco.diameter.CreditControlContext

/**
 * Interface to interact with a datasource.
 *
 */
interface DataSource {
    /**
     * Initiates the datasource
     */
    fun init()

    /**
     * Forward a new initial/update/terminate request.
     *
     * @param context That holds the request and session
     */
    fun handleRequest(context: CreditControlContext)

    /**
     * Check if a subscriber is on the blocked-list.
     * If a subscriber is blocked then requests for this
     * subscriber can not be handled locally.
     *
     * @param msisdn Subscriber msisdn to check
     */
    fun isBlocked(msisdn: String): Boolean
}