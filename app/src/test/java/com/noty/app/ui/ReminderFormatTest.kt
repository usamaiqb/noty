package com.noty.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderFormatTest {

    private val zone = ZoneId.of("Asia/Karachi")
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private val now = at("2026-11-26T23:30")

    @Test
    fun sameDate_isToday_evenMinutesBeforeMidnight() {
        assertEquals(0L, daysFromToday(at("2026-11-26T23:59"), now, zone))
    }

    @Test
    fun justPastMidnight_isTomorrow_notToday() {
        assertEquals(1L, daysFromToday(at("2026-11-27T00:05"), now, zone))
    }

    @Test
    fun countsCalendarDays_notTwentyFourHourBlocks() {
        assertEquals(2L, daysFromToday(at("2026-11-28T00:01"), now, zone))
    }

    @Test
    fun earlierDate_isNegative() {
        assertEquals(-1L, daysFromToday(at("2026-11-25T12:00"), now, zone))
    }
}
