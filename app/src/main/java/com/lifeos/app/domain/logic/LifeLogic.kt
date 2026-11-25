package com.lifeos.app.domain.logic

import com.lifeos.app.domain.model.Recurrence
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Recurrence arithmetic in local wall-clock time, so "9 AM daily" stays 9 AM across DST/zone changes. */
object RecurrenceMath {

    fun next(from: Instant, recurrence: Recurrence, zone: ZoneId): Instant? {
        val z = from.atZone(zone)
        val n: ZonedDateTime = when (recurrence) {
            Recurrence.NONE -> return null
            Recurrence.DAILY -> z.plusDays(1)
            Recurrence.WEEKLY -> z.plusWeeks(1)
            Recurrence.MONTHLY -> z.plusMonths(1)
            Recurrence.YEARLY -> z.plusYears(1)
        }
        return n.toInstant()
    }

    /** First occurrence strictly after [now], starting from [start]. Null for one-off past events. */
    fun nextAfter(start: Instant, recurrence: Recurrence, now: Instant, zone: ZoneId): Instant? {
        if (start.isAfter(now)) return start
        if (recurrence == Recurrence.NONE) return null
        var t = start
        var guard = 0
        while (!t.isAfter(now) && guard < 100_000) {
            t = next(t, recurrence, zone) ?: return null
            guard++
        }
        return t
    }
}

/** Parses user-typed rupee amounts into paise. Accepts "1,250.50", "₹ 500", "50k", "1.5 lakh", "2 crore". */
object MoneyParser {
    private val pattern = Regex(
        """(?i)(?:₹|rs\.?|inr)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(k|thousand|l|lakh|lakhs|lac|cr|crore|crores)?\b"""
    )

    fun parsePaise(text: String): Long? {
        val m = pattern.find(text.trim()) ?: return null
        val number = m.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
        val multiplier = when (m.groupValues[2].lowercase()) {
            "k", "thousand" -> 1_000L
            "l", "lakh", "lakhs", "lac" -> 1_00_000L
            "cr", "crore", "crores" -> 1_00_00_000L
            else -> 1L
        }
        val paise = number.multiply(java.math.BigDecimal(multiplier * 100)).setScale(0, java.math.RoundingMode.HALF_UP)
        val value = paise.toLong()
        return if (value <= 0 || value > 1_000_000_000_000_00L) null else value
    }

    /** Strict form-field parse: the whole string must be an amount. */
    fun parseField(text: String): Long? {
        val t = text.trim()
        if (t.isEmpty()) return null
        if (!Regex("""^(?:₹\s*)?[0-9][0-9,]*(?:\.[0-9]{1,2})?$""").matches(t)) return null
        return parsePaise(t)
    }

    fun toFieldText(paise: Long): String {
        val r = paise / 100
        val p = paise % 100
        return if (p == 0L) r.toString() else "$r.${p.toString().padStart(2, '0')}"
    }
}

/** Habit streaks. A streak survives until the end of today even if today isn't ticked yet. */
object StreakMath {
    fun currentStreak(dates: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in dates) today else today.minusDays(1)
        var count = 0
        while (day in dates) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun longestStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (ChronoUnit.DAYS.between(sorted[i - 1], sorted[i]) == 1L) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /** Count of completions in the Monday-started week containing [today]. */
    fun completionsThisWeek(dates: Set<LocalDate>, today: LocalDate): Int {
        val monday = today.with(DayOfWeek.MONDAY)
        return dates.count { !it.isBefore(monday) && !it.isAfter(monday.plusDays(6)) }
    }
}

data class Badge(val id: String, val title: String, val description: String, val earned: Boolean)

/** Achievement badges, computed from stored records only. */
object BadgeRules {
    fun evaluate(
        completedTasks: Int,
        completedGoals: Int,
        longestHabitStreak: Int,
        notesCount: Int,
        transactionsCount: Int,
        documentsCount: Int,
    ): List<Badge> = listOf(
        Badge("first_task", "First Step", "Complete your first task", completedTasks >= 1),
        Badge("tasks_50", "Finisher", "Complete 50 tasks", completedTasks >= 50),
        Badge("goal_1", "Goal Getter", "Complete a goal", completedGoals >= 1),
        Badge("streak_7", "One Week Strong", "Reach a 7-day habit streak", longestHabitStreak >= 7),
        Badge("streak_30", "Unstoppable", "Reach a 30-day habit streak", longestHabitStreak >= 30),
        Badge("notes_10", "Thinker", "Write 10 notes", notesCount >= 10),
        Badge("money_25", "Money Minded", "Record 25 transactions", transactionsCount >= 25),
        Badge("vault_5", "Organised", "Store 5 documents", documentsCount >= 5),
    )
}

/** Automatic goal milestones at 25/50/75/100%. */
object GoalMilestones {
    val thresholds = listOf(0.25f, 0.5f, 0.75f, 1f)
    fun reached(progress: Float): List<Boolean> = thresholds.map { progress >= it - 1e-6f }
}

/** Parses "08:00, 20:30" style medication times. Invalid entries are rejected. */
object TimeListParser {
    fun parse(text: String): List<LocalTime>? {
        val parts = text.split(',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.size > 12) return null
        val times = parts.map { p ->
            val m = Regex("""^([01]?\d|2[0-3])[:.]([0-5]\d)$""").matchEntire(p) ?: return null
            LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        return times.distinct().sorted()
    }

    fun format(times: List<LocalTime>): String =
        times.joinToString(",") { "%02d:%02d".format(it.hour, it.minute) }
}
