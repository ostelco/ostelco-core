package org.ostelco.prime.client.api.health

/**
 * Interface for 'health' monitoring.
 */
@FunctionalInterface
interface HealthListener {

    /**
     * Returns 'health state' of connection.
     * @return true if healthy
     */
    val isHealthy: Boolean
}
