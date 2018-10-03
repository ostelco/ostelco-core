package org.ostelco.bqmetrics

import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import org.mockito.Mockito.`when`
import kotlin.test.assertNotEquals

/**
 * Class for testing the SQL expander.
 */
class MetricBuildersTest {

    @Test
    fun testSQLNoVars() {
        val testEnvVars = mock(EnvironmentVars::class.java)
        `when`(testEnvVars.getVar("DATASET_PROJECT")).thenReturn("pantel-2decb")
        `when`(testEnvVars.getVar("DATASET_MODIFIER")).thenReturn("_dev")
        val sql = """
        SELECT count(distinct user_pseudo_id) AS count FROM `pantel-2decb.analytics_160712959.events_*`
        WHERE event_name = "first_open"
        """
        val metric: SummaryMetricBuilder = SummaryMetricBuilder(
                metricName = "metric1",
                help = "none",
                sql = sql,
                resultColumn = "result1",
                env = testEnvVars
        )
        assertEquals(metric.expandSql(), sql.trimIndent())
    }
    @Test
    fun testSQL2Vars() {
        val testEnvVars = mock(EnvironmentVars::class.java)
        `when`(testEnvVars.getVar("DATASET_PROJECT")).thenReturn("pantel-2decb")
        `when`(testEnvVars.getVar("DATASET_MODIFIER")).thenReturn("_dev")
        val sql = """
        SELECT count(distinct user_pseudo_id) AS count FROM `${'$'}{DATASET_PROJECT}.analytics_160712959${'$'}{DATASET_MODIFIER}.events_*`
        WHERE event_name = "first_open"
        """
        val sqlResult = """
        SELECT count(distinct user_pseudo_id) AS count FROM `pantel-2decb.analytics_160712959_dev.events_*`
        WHERE event_name = "first_open"
        """
        val metric: SummaryMetricBuilder = SummaryMetricBuilder(
                metricName = "metric1",
                help = "none",
                sql = sql,
                resultColumn = "result1",
                env = testEnvVars
        )
        assertEquals(metric.expandSql(), sqlResult.trimIndent())
    }

    @Test
    fun testSQLUnknownVar() {
        val testEnvVars = mock(EnvironmentVars::class.java)
        `when`(testEnvVars.getVar("DATASET_PROJECT")).thenReturn("pantel-2decb")
        `when`(testEnvVars.getVar("DATASET_MODIFIER")).thenReturn(null)
        val sql = """
        SELECT count(distinct user_pseudo_id) AS count FROM `${'$'}{DATASET_PROJECT}.analytics_160712959${'$'}{DATASET_MODIFIER}.events_*`
        WHERE event_name = "first_open"
        """
        val sqlResult = """
        SELECT count(distinct user_pseudo_id) AS count FROM `pantel-2decb.analytics_160712959.events_*`
        WHERE event_name = "first_open"
        """
        val metric: SummaryMetricBuilder = SummaryMetricBuilder(
                metricName = "metric1",
                help = "none",
                sql = sql,
                resultColumn = "result1",
                env = testEnvVars
        )
        assertEquals(metric.expandSql(), sqlResult.trimIndent())
    }

    @Test
    fun testMangleBadSQL() {
        val testEnvVars = mock(EnvironmentVars::class.java)
        `when`(testEnvVars.getVar("DATASET_PROJECT")).thenReturn("pantel-2decb")
        `when`(testEnvVars.getVar("DATASET_MODIFIER")).thenReturn("; DELETE * from abc;")
        val sql = """
        SELECT count(distinct user_pseudo_id) AS count FROM `${'$'}{DATASET_PROJECT}.analytics_160712959${'$'}{DATASET_MODIFIER}.events_*`
        WHERE event_name = "first_open"
        """
        val sqlResult = """
        SELECT count(distinct user_pseudo_id) AS count FROM `pantel-2decb.analytics_160712959; DELETE * from abc;.events_*`
        WHERE event_name = "first_open"
        """
        val metric: SummaryMetricBuilder = SummaryMetricBuilder(
                metricName = "metric1",
                help = "none",
                sql = sql,
                resultColumn = "result1",
                env = testEnvVars
        )
        println(metric.expandSql())
        assertNotEquals(metric.expandSql(), sqlResult.trimIndent())
    }
}
