package org.ostelco.prime.analytics

import org.ostelco.prime.analytics.MetricType.COUNTER
import org.ostelco.prime.analytics.MetricType.GAUGE

interface AnalyticsService {
    fun reportTrafficInfo(msisdn: String, usedBytes: Long, bundleBytes: Long)
    fun reportMetric(primeMetric: PrimeMetric, value: Long)
}

enum class PrimeMetric(val metricType: MetricType) {

    // sorted alphabetically
    ACTIVE_SESSIONS(GAUGE),
    MEGABYTES_CONSUMED(COUNTER),
    REVENUE(COUNTER),
    TOTAL_USERS(GAUGE),
    USERS_ACQUIRED_THROUGH_REFERRALS(GAUGE),
    USERS_PAID_AT_LEAST_ONCE(GAUGE);

    val metricName: String
        get() = name.toLowerCase()
}

enum class MetricType {
    COUNTER,
    GAUGE,
}