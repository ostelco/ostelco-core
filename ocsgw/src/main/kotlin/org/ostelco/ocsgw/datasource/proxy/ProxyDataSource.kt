package org.ostelco.ocsgw.datasource.proxy

import org.ostelco.diameter.CreditControlContext
import org.ostelco.diameter.getLogger
import org.ostelco.ocs.api.CreditControlRequestType
import org.ostelco.ocsgw.datasource.DataSource
import org.ostelco.ocsgw.datasource.local.LocalDataSource

/**
 * Proxy DataSource is a combination of the Local DataSource and any other
 * DataSource.
 *
 * With the proxy approach the CCR-I request will not use the LocalDataStore. If the
 * reply for the CCR-I is accepted the following CCR-U requests will use the LocalDataSource
 * to quickly reply and accept the query. But it will also send the same request on using the
 * secondary DataSource. When the secondary DataSource deny a CCR-U then the next request will be denied.
 *
 * The DataSource will keep a block list of msisdns that has failed to query new buckets when doing CCR.
 */
class ProxyDataSource(private val secondary: DataSource) : DataSource {
    private val local: DataSource = LocalDataSource()
    override fun init() { // No init needed
    }

    override fun handleRequest(context: CreditControlContext) {
        when (context.originalCreditControlRequest.requestTypeAVPValue) {
            CreditControlRequestType.INITIAL_REQUEST_VALUE -> secondary.handleRequest(context)
            CreditControlRequestType.UPDATE_REQUEST_VALUE -> if (!secondary.isBlocked(context.creditControlRequest.msisdn)) {
                proxyAnswer(context)
            } else {
                secondary.handleRequest(context)
            }
            CreditControlRequestType.TERMINATION_REQUEST_VALUE -> proxyAnswer(context)
            else -> logger.warn("Unknown request type : {}", context.originalCreditControlRequest.requestTypeAVPValue)
        }
    }

    /**
     * Use the local data source to send an answer directly to P-GW.
     * Use secondary to report the usage to OCS.
     */
    private fun proxyAnswer(context: CreditControlContext) {
        local.handleRequest(context)
        context.skipAnswer = true
        secondary.handleRequest(context)
    }

    override fun isBlocked(msisdn: String): Boolean {
        return secondary.isBlocked(msisdn)
    }

    companion object {
        private val logger by getLogger()
    }
}