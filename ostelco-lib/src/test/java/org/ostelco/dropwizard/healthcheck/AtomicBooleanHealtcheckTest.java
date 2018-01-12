package org.ostelco.dropwizard.healthcheck;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public final class AtomicBooleanHealtcheckTest {


    private static final String ERROR_MESSAGE = "Error message";

    @Test
    public void check() {

        final AtomicBoolean watched = new AtomicBoolean(false);
        final AtomicBooleanHealtcheck abhc =
                new AtomicBooleanHealtcheck(watched, ERROR_MESSAGE);
        assertFalse(abhc.check().isHealthy());
        watched.set(true);
        assertTrue(abhc.check().isHealthy());
    }
}