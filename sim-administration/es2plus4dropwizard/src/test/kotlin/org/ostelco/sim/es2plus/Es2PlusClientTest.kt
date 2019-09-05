package org.ostelco.sim.es2plus

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime


class Es2PlusClientTest {
    @Test
    fun testDownloadOrder() {
        val localTime = LocalDateTime.parse("2011-12-03T10:15:30")
        val time = ZonedDateTime.ofLocal(localTime, ZoneId.of("Z"), ZoneOffset.MIN)
        val timeString = ES2PlusClient.getDatetime(time)
        assertEquals("2011-12-03T10:15:30Z", timeString)
    }
}