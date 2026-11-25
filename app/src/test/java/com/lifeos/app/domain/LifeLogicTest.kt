package com.lifeos.app.domain

import com.lifeos.app.domain.logic.BadgeRules
import com.lifeos.app.domain.logic.GoalMilestones
import com.lifeos.app.domain.logic.MoneyParser
import com.lifeos.app.domain.logic.RecurrenceMath
import com.lifeos.app.domain.logic.StreakMath
import com.lifeos.app.domain.logic.TimeListParser
import com.lifeos.app.domain.model.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class LifeLogicTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")

    @Test fun recurrenceKeepsWallClockAcrossDst() {
        val ny = ZoneId.of("America/New_York")
        val start = LocalDateTime.of(2026, 3, 7, 9, 0).atZone(ny).toInstant()
        val next = RecurrenceMath.next(start, Recurrence.DAILY, ny)!!
        assertEquals(LocalTime.of(9, 0), next.atZone(ny).toLocalTime())
        assertNull(RecurrenceMath.next(start, Recurrence.NONE, ny))
    }

    @Test fun monthlyRecurrenceClampsMonthEnd() {
        val jan31 = LocalDateTime.of(2027, 1, 31, 10, 0).atZone(kolkata).toInstant()
        val feb = RecurrenceMath.next(jan31, Recurrence.MONTHLY, kolkata)!!
        assertEquals(LocalDate.of(2027, 2, 28), feb.atZone(kolkata).toLocalDate())
    }

    @Test fun nextAfterSkipsPastOccurrences() {
        val start = LocalDateTime.of(2026, 11, 1, 8, 0).atZone(kolkata).toInstant()
        val now = LocalDateTime.of(2026, 11, 25, 12, 0).atZone(kolkata).toInstant()
        val n = RecurrenceMath.nextAfter(start, Recurrence.WEEKLY, now, kolkata)!!
        assertEquals(LocalDate.of(2026, 11, 29), n.atZone(kolkata).toLocalDate())
        assertNull(RecurrenceMath.nextAfter(start, Recurrence.NONE, now, kolkata))
    }

    @Test fun moneyParsing() {
        assertEquals(125_050L, MoneyParser.parseField("1,250.50"))
        assertEquals(50_000_00L, MoneyParser.parseField("₹50,000"))
        assertNull(MoneyParser.parseField("12abc"))
        assertNull(MoneyParser.parseField("0"))
        assertNull(MoneyParser.parseField(""))
        assertEquals(1_50_000_00L, MoneyParser.parsePaise("1.5 lakh"))
        assertEquals(2_00_00_000_00L, MoneyParser.parsePaise("2 crore"))
        assertEquals("1250.5".let { "1250.50" }, MoneyParser.toFieldText(125_050))
        assertEquals("500", MoneyParser.toFieldText(500_00))
    }

    @Test fun streaks() {
        val today = LocalDate.of(2026, 11, 25)
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(10))
        assertEquals(3, StreakMath.currentStreak(dates, today))
        assertEquals(4, StreakMath.currentStreak(dates + today, today))
        assertEquals(0, StreakMath.currentStreak(setOf(today.minusDays(2)), today))
        assertEquals(4, StreakMath.longestStreak(dates + today))
        assertEquals(3, StreakMath.completionsThisWeek(dates + today, today)) // Mon 23 – Wed 25
    }

    @Test fun badgesAndMilestones() {
        val b = BadgeRules.evaluate(1, 0, 7, 0, 30, 0)
        assertEquals(listOf("first_task", "streak_7", "money_25"), b.filter { it.earned }.map { it.id })
        assertEquals(listOf(true, true, false, false), GoalMilestones.reached(0.5f))
    }

    @Test fun medicationTimes() {
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 30)), TimeListParser.parse("20:30, 8:00"))
        assertNull(TimeListParser.parse("25:00"))
        assertNull(TimeListParser.parse(""))
        assertEquals("08:00,20:30", TimeListParser.format(listOf(LocalTime.of(8, 0), LocalTime.of(20, 30))))
    }
}
