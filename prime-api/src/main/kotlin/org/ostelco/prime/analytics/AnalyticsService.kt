package org.ostelco.prime.analytics

interface AnalyticsService {
    fun reportTrafficInfo(msisdn: String, usedBytes: Long, bundleBytes: Long)
}
