package org.ostelco.pseudonym.service

import com.google.cloud.bigquery.BigQuery
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import org.ostelco.pseudonym.ConfigRegistry
import org.ostelco.pseudonym.PseudonymServerConfig
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

class PseudonymizerServiceTest {

    private val testMsisdn = "4790303333"
    private val testSubscriberId = "foo@test.com"

    /**
     * Test get Msisdn Pseudonym
     */
    @Test
    fun `test getMsisdnPseudonym`() {

        val now = Instant.now().toEpochMilli()
        val pseudonymEntity = PseudonymizerServiceSingleton.getMsisdnPseudonym(msisdn = testMsisdn, timestamp = now)
        assertEquals(expected = testMsisdn, actual = pseudonymEntity.sourceId)
    }

    /**
     * Test get Subscriber ID Pseudonym
     */
    @Test
    fun `test getSubscriberIdPseudonym`() {

        val now = Instant.now().toEpochMilli()
        val pseudonymEntity = PseudonymizerServiceSingleton.getSubscriberIdPseudonym(subscriberId = testSubscriberId, timestamp = now)
        assertEquals(expected = testSubscriberId, actual = pseudonymEntity.sourceId)
    }

    /**
     * Test get Active Pseudonym for SubscriberId
     */
    @Test
    fun `test getActivePseudonymsForSubscriberId`() {

        val now = Instant.now().toEpochMilli()
        val pseudonymEntity = PseudonymizerServiceSingleton.getSubscriberIdPseudonym(subscriberId = testSubscriberId, timestamp = now)
        assertEquals(expected = testSubscriberId, actual = pseudonymEntity.sourceId)

        val activePseudonyms = PseudonymizerServiceSingleton.getActivePseudonymsForSubscriberId(subscriberId = testSubscriberId)

        assertEquals(expected = pseudonymEntity, actual = activePseudonyms.current)
        assertEquals(expected = activePseudonyms.current.end + 1, actual = activePseudonyms.next.start)
    }

    /**
     * Test a finding a pseudonym
     */
    @Test
    fun `test findMsisdnPseudonym`() {

        val now = Instant.now().toEpochMilli()
        val pseudonym = PseudonymizerServiceSingleton.getMsisdnPseudonym(msisdn = testMsisdn, timestamp = now).pseudonym

        val msisdn = PseudonymizerServiceSingleton.findMsisdnPseudonym(pseudonym = pseudonym)?.sourceId ?: fail()

        assertEquals(expected = testMsisdn, actual = msisdn)
    }

    /**
     * Test deleting All Msisdn Pseudonyms
     */
    fun `test deleteAllMsisdnPseudonyms`() {

        val now = Instant.now().toEpochMilli()
        val pseudonym = PseudonymizerServiceSingleton.getMsisdnPseudonym(msisdn = testMsisdn, timestamp = now).pseudonym

        PseudonymizerServiceSingleton.deleteAllMsisdnPseudonyms(msisdn = testMsisdn)

        val pseudonymEntity = PseudonymizerServiceSingleton.findMsisdnPseudonym(pseudonym = pseudonym)

        assertNull(pseudonymEntity)
    }

    companion object {

        @JvmStatic
        @BeforeClass
        fun init() {
            ConfigRegistry.config = PseudonymServerConfig()
                    .apply { this.datastoreType = "inmemory-emulator" }
            PseudonymizerServiceSingleton.init(env = null, bq = Mockito.mock(BigQuery::class.java))
        }
    }
}