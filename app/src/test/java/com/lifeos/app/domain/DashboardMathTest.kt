package com.lifeos.app.domain

import com.lifeos.app.domain.model.WidgetType
import com.lifeos.app.domain.usecase.DashboardMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DashboardMathTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test fun productivityIsNullWithoutData() {
        assertNull(DashboardMath.productivityScore(0, 0, 0))
    }

    @Test fun productivityCountsOverdueAgainstScore() {
        assertEquals(100, DashboardMath.productivityScore(4, 0, 0))
        assertEquals(50, DashboardMath.productivityScore(2, 2, 0))
        assertEquals(40, DashboardMath.productivityScore(2, 2, 1))
        assertEquals(0, DashboardMath.productivityScore(0, 3, 0))
        assertEquals(33, DashboardMath.productivityScore(1, 2, 0))
    }

    @Test fun bucketsByLocalMonthAndZeroFills() {
        val nov = YearMonth.of(2026, 11)
        // 1 Nov 00:15 IST is still 31 Oct in UTC — must count as November locally.
        val earlyNov = LocalDate.of(2026, 11, 1).atTime(0, 15).atZone(zone).toInstant()
        val sep = LocalDate.of(2026, 9, 10).atStartOfDay(zone).toInstant()
        val buckets = DashboardMath.bucketByMonth(
            listOf(100L to earlyNov, 250L to earlyNov, 70L to sep),
            nov, zone,
        )
        assertEquals(6, buckets.size)
        assertEquals(YearMonth.of(2026, 6), buckets.first().month)
        assertEquals(nov, buckets.last().month)
        assertEquals(350L, buckets.last().paise)
        assertEquals(70L, buckets.first { it.month == YearMonth.of(2026, 9) }.paise)
        assertEquals(0L, buckets.first { it.month == YearMonth.of(2026, 10) }.paise)
    }

    @Test fun goalProgressClamps() {
        assertEquals(0.5f, DashboardMath.goalProgress(50.0, 100.0), 0.0001f)
        assertEquals(1f, DashboardMath.goalProgress(150.0, 100.0), 0.0001f)
        assertEquals(0f, DashboardMath.goalProgress(10.0, 0.0), 0.0001f)
    }

    @Test fun nextBirthdayRollsToNextYear() {
        val today = LocalDate.of(2026, 11, 25)
        assertEquals(LocalDate.of(2026, 11, 25), DashboardMath.nextBirthday(LocalDate.of(1990, 11, 25), today))
        assertEquals(LocalDate.of(2027, 1, 3), DashboardMath.nextBirthday(LocalDate.of(1990, 1, 3), today))
        assertEquals(LocalDate.of(2027, 2, 28), DashboardMath.nextBirthday(LocalDate.of(2000, 2, 29), today))
        assertEquals(LocalDate.of(2028, 2, 29), DashboardMath.nextBirthday(LocalDate.of(2000, 2, 29), LocalDate.of(2027, 3, 1)))
    }

    @Test fun familySummaryPicksSoonestBirthday() {
        val today = LocalDate.of(2026, 11, 25)
        val s = DashboardMath.familySummary(
            listOf("Ma" to LocalDate.of(1960, 12, 5), "Baba" to LocalDate.of(1958, 11, 28), "No date" to null),
            today,
        )
        assertEquals(3, s.memberCount)
        assertEquals("Baba", s.nextBirthdayName)
        assertEquals(3L, s.daysUntilBirthday)
    }

    @Test fun normalizeDropsUnknownAndAppendsNew() {
        val stored = listOf("QUICK_NOTES" to false, "RETIRED_WIDGET" to true, "FINANCE_OVERVIEW" to true, "QUICK_NOTES" to true)
        val result = DashboardMath.normalizeWidgets(stored)
        assertEquals(WidgetType.entries.size, result.size)
        assertEquals(WidgetType.QUICK_NOTES, result[0].type)
        assertEquals(false, result[0].isVisible)
        assertEquals(WidgetType.FINANCE_OVERVIEW, result[1].type)
        assertEquals((0 until result.size).toList(), result.map { it.position })
        assertTrue(result.drop(2).all { it.isVisible })
    }

    @Test fun moveReordersAndClamps() {
        val d = DashboardMath.defaultWidgets()
        val moved = DashboardMath.move(d, 0, 1)
        assertEquals(WidgetType.FINANCE_OVERVIEW, moved[0].type)
        assertEquals(WidgetType.TODAY_SCHEDULE, moved[1].type)
        assertEquals((0 until d.size).toList(), moved.map { it.position })
        assertEquals(d, DashboardMath.move(d, 0, -1))
        assertEquals(WidgetType.TODAY_SCHEDULE, DashboardMath.move(d, 0, 99).last().type)
    }
}
