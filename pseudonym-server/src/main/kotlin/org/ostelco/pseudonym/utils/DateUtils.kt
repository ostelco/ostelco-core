package org.ostelco.pseudonym.utils

import org.ostelco.pseudonym.resources.Bounds
import org.ostelco.pseudonym.resources.DateBounds
import java.util.*
/**
 * Implements DateBounds interface which provides boundary timestamps
 * for a week.
 */
class WeeklyBounds: DateBounds {
    private val timeZone = TimeZone.getTimeZone("UTC")
    /**
     * Returns the boundaries for the week of the given timestamp.
     */
    fun getBounds(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone)
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

    override fun getBoundsNKeyPrefix(msisdn: String, timestamp: Long): Pair<Bounds, String> {
        val bounds = getBounds(timestamp.toLong())
        val keyPrefix = "${msisdn}-${bounds.first}"
        return Pair(Bounds(bounds.first, bounds.second), keyPrefix)
    }
}
