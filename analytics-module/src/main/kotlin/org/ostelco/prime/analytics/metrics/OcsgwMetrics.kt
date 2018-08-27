package org.ostelco.prime.analytics.metrics

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistry.name
import org.ostelco.prime.analytics.AnalyticsGrpcService
import java.time.Instant


class OcsgwMetrics(private val registry: MetricRegistry) {

    private val activeSessions: Gauge<Long>

    private val currentActiveSessions = object {
        var timestamp: Long = 0
        var count: Long = 0
    }

    init {
        this.activeSessions = registry.register(
                name(AnalyticsGrpcService::class.java, "active-sessions"),
                Gauge<Long> { currentActiveSessions.count })
    }

    /**
     * Records active user sessions.
     * @param timestamp - seconds since epoch
     * @param count - active sessions
     */
    @Synchronized
    fun setActiveSessions(count: Long) {
        currentActiveSessions.timestamp = Instant.now().toEpochMilli()
        currentActiveSessions.count = count
    }
}
