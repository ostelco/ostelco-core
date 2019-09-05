package org.ostelco.sim.es2plus

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/*
// XXX Write an unit test for this, then use it instead of the static
//     string (currentTimestamp) below
//  ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[T,D,Z]{1}$
fun getDatetime( time: ZonedDateTime) =
        DateTimeFormatter.ofPattern("YYYY-MM-DD'T'hh:mm:ss'Z'").format(time)

fun getNowAsDatetime(): String = getDatetime(ZonedDateTime.now())

 */


class Es2PlusClientTest {
    @Test
    fun testDownloadOrder() {
        val localTime = LocalDateTime.parse("2011-12-03T10:15:30")
        val time = ZonedDateTime.ofLocal(localTime, ZoneId.of("Z"), ZoneOffset.MIN)
        val timeString = ES2PlusClient.getDatetime(time)
        assertEquals("2011-12-03T10:15:30Z", timeString)
    }
}