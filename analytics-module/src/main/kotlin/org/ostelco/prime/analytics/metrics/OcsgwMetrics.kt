package org.ostelco.prime.analytics.metrics

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry


class OcsgwMetrics(registry: MetricRegistry) {

    private val activeSessions: Gauge<Long>

    private val currentActiveSessions = object {
        var count: Long = 0
    }

    init {
        this.activeSessions = registry.register(
                "active_sessions",
                Gauge<Long> { currentActiveSessions.count })
    }

    /**
     * Records active user sessions.
     * @param timestamp - seconds since epoch
     * @param count - active sessions
     */
    @Synchronized
    fun setActiveSessions(count: Long) {
        currentActiveSessions.count = count
    }
}
