package com.lifeos.app.core

import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.DayPart
import com.lifeos.app.core.time.dayPartFor
import com.lifeos.app.core.time.dayRange
import com.lifeos.app.core.time.monthKey
import com.lifeos.app.core.time.monthRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class TimeTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")

    @Test fun greetingBoundaries() {
        assertEquals(DayPart.NIGHT, dayPartFor(4))
        assertEquals(DayPart.MORNING, dayPartFor(5))
        assertEquals(DayPart.MORNING, dayPartFor(11))
        assertEquals(DayPart.AFTERNOON, dayPartFor(12))
        assertEquals(DayPart.AFTERNOON, dayPartFor(16))
        assertEquals(DayPart.EVENING, dayPartFor(17))
        assertEquals(DayPart.EVENING, dayPartFor(21))
        assertEquals(DayPart.NIGHT, dayPartFor(22))
        assertEquals("Good morning", DayPart.MORNING.greeting)
    }

    @Test fun dayRangeIsLocalMidnightToMidnight() {
        val r = LocalDate.of(2026, 11, 25).dayRange(kolkata)
        assertEquals(Instant.parse("2026-11-24T18:30:00Z"), r.start)
        assertEquals(Duration.ofHours(24), Duration.between(r.start, r.end))
    }

    @Test fun dayRangeHandlesDstShortDay() {
        // US spring-forward day has 23 hours.
        val r = LocalDate.of(2026, 3, 8).dayRange(ZoneId.of("America/New_York"))
        assertEquals(Duration.ofHours(23), Duration.between(r.start, r.end))
    }

    @Test fun monthRangeAndKey() {
        val nov = YearMonth.of(2026, 11)
        val r = nov.monthRange(kolkata)
        assertEquals(Instant.parse("2026-10-31T18:30:00Z"), r.start)
        assertEquals(Instant.parse("2026-11-30T18:30:00Z"), r.end)
        assertEquals(202611, nov.monthKey())
    }

    @Test fun relativeLabels() {
        val today = LocalDate.of(2026, 11, 25)
        val at = LocalDate.of(2026, 11, 26).atTime(9, 0).atZone(kolkata).toInstant()
        assertEquals("Tomorrow 9:00 AM", TimeFormat.relative(at, today, kolkata))
        val later = LocalDate.of(2026, 11, 28).atTime(17, 30).atZone(kolkata).toInstant()
        assertEquals("Sat, 28 Nov 5:30 PM", TimeFormat.relative(later, today, kolkata))
    }
}
