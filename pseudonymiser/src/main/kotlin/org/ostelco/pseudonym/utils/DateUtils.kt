package org.ostelco.pseudonym.utils

import org.ostelco.pseudonym.resources.DateBounds
import java.util.*

class WeeklyBounds: DateBounds {
    private val timeZone = TimeZone.getTimeZone("UTC")

    override fun getBounds(timestamp: Long): Pair<Long, Long> {
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
}
