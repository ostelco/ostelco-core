package org.ostelco.prime.analytics.metrics

import com.codahale.metrics.Counter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistry.name
import org.ostelco.prime.analytics.AnalyticsGrpcService


class OcsgwMetrics(private val registry: MetricRegistry) {

    private val activeSessions : Counter

    init {
        this.activeSessions = registry.counter(name(AnalyticsGrpcService::class.java,
                "active-sessions"))
    }

    /**
     * Records active user sessions.
     * @param timestamp - seconds since epoch
     * @param count - active sessions
     */
    @Synchronized fun setActiveSessions(timestamp: Long, count: Long) {
        val currentCount = activeSessions.getCount()
        if (currentCount < count) {
            activeSessions.inc(count - currentCount)
        } else if (currentCount > count) {
            activeSessions.dec(currentCount - count)
        }
    }
}
