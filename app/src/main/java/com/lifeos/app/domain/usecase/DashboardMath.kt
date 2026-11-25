package com.lifeos.app.domain.usecase

import com.lifeos.app.domain.model.DashboardWidget
import com.lifeos.app.domain.model.FamilySummary
import com.lifeos.app.domain.model.MonthlyAmount
import com.lifeos.app.domain.model.WidgetType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Pure functions behind the dashboard, kept separate so they're unit-testable. */
object DashboardMath {

    /**
     * Productivity score for today, 0–100.
     *
     * score = completedToday / (completedToday + openDueToday + overdue) × 100
     *
     * Overdue tasks count against the score so it can't be gamed by never setting due dates
     * on hard work. Returns null when the denominator is zero — we never invent a score.
     */
    fun productivityScore(completedToday: Int, openDueToday: Int, overdue: Int): Int? {
        val total = completedToday + openDueToday + overdue
        if (total <= 0) return null
        return Math.round(completedToday * 100f / total)
    }

    /** Buckets expense amounts into the last [months] calendar months (oldest first, zero-filled). */
    fun bucketByMonth(
        amounts: List<Pair<Long, Instant>>,
        currentMonth: YearMonth,
        zone: ZoneId,
        months: Int = 6,
    ): List<MonthlyAmount> {
        val window = (months - 1 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
        val sums = HashMap<YearMonth, Long>()
        for ((paise, at) in amounts) {
            val ym = YearMonth.from(at.atZone(zone))
            sums[ym] = (sums[ym] ?: 0L) + paise
        }
        return window.map { MonthlyAmount(it, sums[it] ?: 0L) }
    }

    fun goalProgress(current: Double, target: Double): Float =
        if (target <= 0.0) 0f else (current / target).toFloat().coerceIn(0f, 1f)

    /** Next birthday on or after [today]; Feb 29 birthdays fall on Feb 28 in non-leap years. */
    fun nextBirthday(birthDate: LocalDate, today: LocalDate): LocalDate {
        fun inYear(year: Int): LocalDate =
            if (birthDate.monthValue == 2 && birthDate.dayOfMonth == 29 && !java.time.Year.isLeap(year.toLong())) {
                LocalDate.of(year, 2, 28)
            } else {
                birthDate.withYear(year)
            }
        val thisYear = inYear(today.year)
        return if (thisYear.isBefore(today)) inYear(today.year + 1) else thisYear
    }

    fun familySummary(members: List<Pair<String, LocalDate?>>, today: LocalDate): FamilySummary {
        val next = members
            .mapNotNull { (name, bd) -> bd?.let { name to nextBirthday(it, today) } }
            .minByOrNull { it.second }
        return FamilySummary(
            memberCount = members.size,
            nextBirthdayName = next?.first,
            nextBirthdayDate = next?.second,
            daysUntilBirthday = next?.let { ChronoUnit.DAYS.between(today, it.second) },
        )
    }

    /**
     * Merges stored widget rows with the full widget catalogue: unknown stored keys are dropped,
     * new widget types are appended (visible), and positions are re-normalised to 0..n-1.
     */
    fun normalizeWidgets(stored: List<Pair<String, Boolean>>): List<DashboardWidget> {
        val known = stored.mapNotNull { (key, visible) ->
            WidgetType.entries.firstOrNull { it.name == key }?.let { it to visible }
        }.distinctBy { it.first }
        val missing = WidgetType.entries.filter { t -> known.none { it.first == t } }.map { it to true }
        return (known + missing).mapIndexed { i, (type, visible) -> DashboardWidget(type, i, visible) }
    }

    /** Moves the widget at [from] by [delta] positions (clamped), returning renumbered list. */
    fun move(widgets: List<DashboardWidget>, from: Int, delta: Int): List<DashboardWidget> {
        if (from !in widgets.indices) return widgets
        val to = (from + delta).coerceIn(0, widgets.lastIndex)
        if (to == from) return widgets
        val list = widgets.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        return list.mapIndexed { i, w -> w.copy(position = i) }
    }

    fun defaultWidgets(): List<DashboardWidget> =
        WidgetType.entries.mapIndexed { i, t -> DashboardWidget(t, i, true) }
}
