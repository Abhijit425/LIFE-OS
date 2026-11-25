package com.lifeos.app.data.repository

import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.core.time.dayRange
import com.lifeos.app.core.time.monthRange
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.dao.CategoryTotal
import com.lifeos.app.domain.logic.Badge
import com.lifeos.app.domain.logic.BadgeRules
import com.lifeos.app.domain.logic.StreakMath
import com.lifeos.app.domain.model.TaskStatus
import com.lifeos.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

data class DayCount(val date: LocalDate, val count: Int)

data class Insights(
    val tasksPerDay: List<DayCount>,
    val spendingThisMonth: Long,
    val spendingLastMonth: Long,
    val categories: List<CategoryTotal>,
    val habitWeekRate: Float?,
    val goalAverage: Float?,
    val goalsCompleted: Int,
)

/** Analytics computed from stored records only. */
@Singleton
class InsightsRepository @Inject constructor(
    private val db: LifeOsDatabase,
    private val time: TimeProvider,
) {
    private val farFuture: Instant = Instant.ofEpochMilli(Long.MAX_VALUE / 4)

    fun observeInsights(): Flow<Insights> {
        val zone = time.zone()
        val today = time.now().atZone(zone).toLocalDate()
        val weekStart = today.minusDays(6)
        val month = YearMonth.from(today)
        val m = month.monthRange(zone)
        val last = month.minusMonths(1).monthRange(zone)
        val tx = db.transactionDao()
        val part1 = combine(
            db.taskDao().observeCompletionTimesSince(weekStart.dayRange(zone).start),
            tx.observeTotal(TransactionType.EXPENSE.name, m.start, m.end),
            tx.observeTotal(TransactionType.EXPENSE.name, last.start, last.end),
            tx.observeCategoryTotals(TransactionType.EXPENSE.name, m.start, m.end),
        ) { times, thisMonth, lastMonth, cats ->
            val days = (0L..6L).map { weekStart.plusDays(it) }
            val counts = times.filterNotNull().groupingBy { it.atZone(zone).toLocalDate() }.eachCount()
            Triple(days.map { DayCount(it, counts[it] ?: 0) }, thisMonth to lastMonth, cats)
        }
        val part2 = combine(db.habitDao().observeActive(), db.habitDao().observeAllEntries(), db.goalDao().observeAll()) { habits, entries, goals ->
            val habitIds = habits.map { it.id }.toSet()
            val weekEntries = entries.count { it.habitId in habitIds && !it.date.isBefore(weekStart) && !it.date.isAfter(today) }
            val rate = if (habits.isEmpty()) null else weekEntries.toFloat() / (habits.size * 7)
            val active = goals.filter { !it.isCompleted && it.targetValue > 0 }
            val avg = if (active.isEmpty()) null else active.map { (it.currentValue / it.targetValue).coerceIn(0.0, 1.0) }.average().toFloat()
            Triple(rate, avg, goals.count { it.isCompleted })
        }
        return combine(part1, part2) { a, b ->
            Insights(a.first, a.second.first, a.second.second, a.third, b.first, b.second, b.third)
        }
    }

    fun observeBadges(): Flow<List<Badge>> = combine(
        db.taskDao().observeAll(),
        db.goalDao().observeAll(),
        db.habitDao().observeAllEntries(),
        combine(db.noteDao().observe(false), db.noteDao().observe(true)) { a, b -> a.size + b.size },
        combine(db.transactionDao().observeBetween(Instant.EPOCH, farFuture), db.documentDao().observeAll()) { t, d -> t.size to d.size },
    ) { tasks, goals, entries, notes, txDocs ->
        val longest = entries.groupBy { it.habitId }.values.maxOfOrNull { list -> StreakMath.longestStreak(list.map { it.date }.toSet()) } ?: 0
        BadgeRules.evaluate(
            completedTasks = tasks.count { it.status == TaskStatus.DONE },
            completedGoals = goals.count { it.isCompleted },
            longestHabitStreak = longest,
            notesCount = notes,
            transactionsCount = txDocs.first,
            documentsCount = txDocs.second,
        )
    }
}
