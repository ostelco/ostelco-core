package org.ostelco.prime.analytics.metrics

import com.codahale.metrics.Counter
import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import org.ostelco.prime.analytics.MetricType.COUNTER
import org.ostelco.prime.analytics.MetricType.GAUGE
import org.ostelco.prime.analytics.PrimeMetric

/**
 * Singleton wrapper dropwizard metrics.
 */
object CustomMetricsRegistry {

    private lateinit var registry: MetricRegistry
    // boolean flag to avoid access to late init registry if it not yet initialized
    private var isInitialized = false

    // map of long values which will act as cache for Gauge
    private val gaugeValueMap: MutableMap<PrimeMetric, Long> = mutableMapOf()

    // map of counters
    private val counterMap: MutableMap<PrimeMetric, Counter> = mutableMapOf()

    @Synchronized
    fun init(registry: MetricRegistry) {
        this.registry = registry
        isInitialized = true
        counterMap.keys.forEach { registerCounter(it) }
        gaugeValueMap.keys.forEach { registerGauge(it) }
    }

    /**
     * Update metric value.
     *
     * If metric is of type COUNTER, then the counter is increment by that value.
     * If metric is of type GAUGE, then the gauge source is set to that value.
     *
     * @param primeMetric
     * @param value
     */
    @Synchronized
    fun updateMetricValue(primeMetric: PrimeMetric, value: Long) {
        when (primeMetric.metricType) {
            COUNTER -> {
                val counterExists = counterMap.containsKey(primeMetric)
                counterMap.getOrPut(primeMetric) { Counter() }.inc(value)
                if (isInitialized && !counterExists) {
                    registerCounter(primeMetric)
                }
            }
            GAUGE -> {
                val existingGaugeValue = gaugeValueMap.put(primeMetric, value)
                if (isInitialized && existingGaugeValue == null) {
                    registerGauge(primeMetric)
                }
            }
        }
    }

    // Register counter with value from counterMap
    private fun registerCounter(primeMetric: PrimeMetric) {
        registry.register(primeMetric.metricName, counterMap[primeMetric])
    }

    // Register gauge with value from gaugeValueMap as its source
    private fun registerGauge(primeMetric: PrimeMetric) {
        registry.register(primeMetric.metricName, Gauge<Long> { gaugeValueMap[primeMetric] })
    }
}