package org.ostelco.simcards.smdpplus;

import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import junit.framework.TestCase.assertEquals
import org.junit.ClassRule
import org.junit.Test

public class SmDpPlusTest {

    companion object {
        @JvmField
        @ClassRule
        val SM_DP_PLUS_RULE = DropwizardAppRule(SmDpPlusApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yml"))
    }

    @Test
    fun testThatCorrectNumberOfProfilesAreLoaded() {
        val app: SmDpPlusApplication = SM_DP_PLUS_RULE.getApplication<SmDpPlusApplication>()
        assertEquals(100, app.noOfEntries())
    }
}
