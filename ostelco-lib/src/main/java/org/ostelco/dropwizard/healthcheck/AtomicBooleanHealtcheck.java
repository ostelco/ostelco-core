package org.ostelco.dropwizard.healthcheck;

import com.codahale.metrics.health.HealthCheck;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A healtcheck that will check an {@link java.util.concurrent.atomic.AtomicBoolean}
 * instance, and if it's true then the health is healthy, if not then the health
 * is unhealthy.  A static string indicating how health is bad will also be reported
 * as part of the {@link com.codahale.metrics.health.HealthCheck.Result}
 * instance returned by the "check" method.
 */
public class AtomicBooleanHealtcheck extends HealthCheck {

    /**
     * The variable we're watching.
     */
    private final AtomicBoolean watchedVariable;

    /**
     * The message to provide if health is unhealthy. Typically
     * an explanation for this particular healthcheck e.g. "Ansible
     * connection to Bugger Homeworld expeditionary task force server lost."
     */
    private final String errorMessage;

    /**
     * Create a new healthcheck instance.
     * @param watched The variable we're watching.
     * @param errorMessage The message to provide if health is unhealthy.
     */
    public AtomicBooleanHealtcheck(
            final AtomicBoolean watched,
            final String errorMessage) {
        super();
        this.watchedVariable = checkNotNull(watched);
        this.errorMessage = checkNotNull(errorMessage);
    }

    @Override
    protected Result check() {
        if (watchedVariable.get()) {
            return Result.healthy();
        } else {
            return Result.healthy(errorMessage);
        }
    }
}
