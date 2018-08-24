package org.ostelco.pseudonym.utils

import org.ostelco.pseudonym.service.Bounds
import org.ostelco.pseudonym.service.DateBounds
import java.util.*

/**
 * Implements DateBounds interface which provides boundary timestamps
 * for a week.
 */
class WeeklyBounds : DateBounds {
    private val timeZone = TimeZone.getTimeZone("UTC")
    private val locale  = java.util.Locale.UK
    /**
     * Returns the boundaries for the week of the given timestamp.
     */
    fun getBounds(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone, locale)
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.clear(Calendar.MINUTE)
        cal.clear(Calendar.SECOND)
        cal.clear(Calendar.MILLISECOND)

        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val start = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    override fun getNextPeriodStart(timestamp: Long): Long {
        val cal = Calendar.getInstance(timeZone, locale)
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.clear(Calendar.MINUTE)
        cal.clear(Calendar.SECOND)
        cal.clear(Calendar.MILLISECOND)

        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        return cal.timeInMillis
    }

    override fun getBoundsNKeyPrefix(msisdn: String, timestamp: Long): Pair<Bounds, String> {
        val bounds = getBounds(timestamp)
        val keyPrefix = "$msisdn-${bounds.first}"
        return Pair(Bounds(bounds.first, bounds.second), keyPrefix)
    }
}
