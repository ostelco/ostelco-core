package org.ostelco.prime;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ostelco.prime.config.PrimeConfiguration;

import static org.junit.Assert.assertNotNull;

public class TestPrimeConfig {

    private static final DropwizardTestSupport<PrimeConfiguration> SUPPORT =
            new DropwizardTestSupport<>(PrimeApplication.class,
                    ResourceHelpers.resourceFilePath("config.yaml"));

    @BeforeClass
    public static void beforeClass() {
        SUPPORT.before();
    }

    @AfterClass
    public static void afterClass() {
        SUPPORT.after();
    }

    /**
     * Do nothing.
     * This test will just start and stop the server.
     * It will validate config file in 'src/test/resources/config.yaml'
     */
    @Test
    public void test() {
        assertNotNull(SUPPORT);
    }
}
