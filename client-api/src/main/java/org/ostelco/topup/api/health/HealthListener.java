package org.ostelco.topup.api.health;

/**
 * Interface for 'health' monitoring.
 */
@FunctionalInterface
public interface HealthListener {

    /**
     * Returns 'health state' of connection.
     * @return true if healthy
     */
    public boolean isHealthy();
}
