package org.ostelco.pseudonym

import org.junit.Test
import org.ostelco.pseudonym.utils.WeeklyBounds
import kotlin.test.assertEquals

/**
 * Class for unit testing DateUtils.
 */
class DateUtilsTest {
    val dateBounds = WeeklyBounds()
    /**
     * Test the most common use case, find next start period
     */
    @Test
    fun testGetNextPeriodStart() {
        // GMT: Saturday, May 12, 2018 11:59:59.999 PM
        val timestamp =  1526169599999
        // GMT: Sunday, May 13, 2018 12:00:00 AM
        val expectedNextTimestamp =  1526169600000
        val nextTimestamp = dateBounds.getNextPeriodStart(timestamp)
        assertEquals(expectedNextTimestamp, nextTimestamp)
    }
    /**
     * Test what happens when input is in last week of the year
     */
    @Test
    fun testGetNextPeriodAtYearEnd() {
        // GMT: Monday, December 31, 2018 11:59:59 PM
        val timestamp =  1546300799000
        // GMT: Sunday, January 6, 2019 12:00:00 AM
        val expectedNextTimestamp =  1546732800000
        val nextTimestamp = dateBounds.getNextPeriodStart(timestamp)
        assertEquals(expectedNextTimestamp, nextTimestamp)
    }
}