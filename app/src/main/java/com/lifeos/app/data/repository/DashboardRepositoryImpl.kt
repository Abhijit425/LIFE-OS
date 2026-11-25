package com.lifeos.app.data.repository

import com.lifeos.app.core.time.dayRange
import com.lifeos.app.core.time.monthKey
import com.lifeos.app.core.time.monthRange
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.entity.DashboardWidgetEntity
import com.lifeos.app.domain.model.DashboardSnapshot
import com.lifeos.app.domain.model.DashboardWidget
import com.lifeos.app.domain.model.DocumentItem
import com.lifeos.app.domain.model.FinanceSummary
import com.lifeos.app.domain.model.GoalItem
import com.lifeos.app.domain.model.NoteItem
import com.lifeos.app.domain.model.ReminderItem
import com.lifeos.app.domain.model.ScheduleItem
import com.lifeos.app.domain.model.TaskSummary
import com.lifeos.app.domain.model.TransactionType
import com.lifeos.app.domain.repository.DashboardRepository
import com.lifeos.app.domain.usecase.DashboardMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val db: LifeOsDatabase,
) : DashboardRepository {

    override fun observeSnapshot(today: LocalDate, now: Instant, zone: ZoneId): Flow<DashboardSnapshot> {
        val day = today.dayRange(zone)
        val month = YearMonth.from(today)
        val monthRange = month.monthRange(zone)
        val sixMonthsStart = month.minusMonths(5).monthRange(zone).start
        val weekEnd = now.plus(Duration.ofDays(7))

        // ---- Group 1: profile, widgets, schedule, reminders ----
        val header = combine(
            db.userProfileDao().observe(),
            db.dashboardWidgetDao().observeAll(),
            db.calendarEventDao().observeOverlapping(day.start, day.end),
            db.calendarEventDao().observeOverlapping(now, weekEnd),
            db.reminderDao().observeUpcoming(now, 5),
        ) { profile, widgets, todayEvents, weekEvents, reminders ->
            HeaderPart(
                name = profile?.displayName?.takeIf(String::isNotBlank),
                widgets = DashboardMath.normalizeWidgets(widgets.map { it.widgetKey to it.isVisible }),
                events = todayEvents.map { ScheduleItem(it.title, it.startAt, it.endAt, it.allDay, isTask = false) },
                upcomingEvents = weekEvents.size,
                reminders = reminders.map { ReminderItem(it.id, it.title, it.type, it.triggerAt) },
            )
        }

        // ---- Group 2: finance ----
        val tx = db.transactionDao()
        val finance = combine(
            tx.observeTotal(TransactionType.INCOME.name, monthRange.start, monthRange.end),
            tx.observeTotal(TransactionType.EXPENSE.name, monthRange.start, monthRange.end),
            db.budgetDao().observeTotalLimit(month.monthKey()),
            tx.observeAmountsSince(TransactionType.EXPENSE.name, sixMonthsStart),
        ) { income, expense, budget, amounts ->
            FinanceSummary(
                incomePaise = income,
                expensePaise = expense,
                budgetPaise = budget.takeIf { it > 0 },
                lastSixMonthsExpense = DashboardMath.bucketByMonth(amounts.map { it.amountPaise to it.occurredAt }, month, zone),
            )
        }

        // ---- Group 3: tasks ----
        val tasks = db.taskDao()
        val taskPart = combine(
            tasks.observeOpenDueBetween(day.start, day.end),
            tasks.observeCompletedBetween(day.start, day.end),
            tasks.observeOverdueCount(day.start),
            tasks.observeOpenCount(),
        ) { dueToday, completed, overdue, open ->
            TaskPart(
                items = dueToday.mapNotNull { t ->
                    t.dueAt?.let { ScheduleItem(t.title, it, it, allDay = false, isTask = true) }
                },
                summary = TaskSummary(dueTodayOpen = dueToday.size, completedToday = completed, overdue = overdue, openTotal = open),
            )
        }

        // ---- Group 4: goals, family, notes, documents ----
        val lifePart = combine(
            db.goalDao().observeActive(),
            db.familyMemberDao().observeAll(),
            db.noteDao().observeRecent(3),
            db.documentDao().observeRecent(3),
        ) { goals, family, notes, docs ->
            LifePart(
                goals = goals.map {
                    GoalItem(it.id, it.title, DashboardMath.goalProgress(it.currentValue, it.targetValue), it.currentValue, it.targetValue, it.unit)
                },
                family = DashboardMath.familySummary(family.map { it.name to it.birthDate }, today),
                notes = notes.map { n ->
                    NoteItem(n.id, n.title.ifBlank { "Untitled" }, n.content.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().take(90), n.isPinned)
                },
                documents = docs.map { DocumentItem(it.id, it.displayName, it.category, it.addedAt) },
            )
        }

        return combine(header, finance, taskPart, lifePart) { h, f, t, l ->
            DashboardSnapshot(
                displayName = h.name,
                widgets = h.widgets,
                schedule = (h.events + t.items).sortedBy { it.startAt },
                upcomingEventsCount = h.upcomingEvents,
                finance = f,
                tasks = t.summary,
                reminders = h.reminders,
                goals = l.goals,
                family = l.family,
                notes = l.notes,
                documents = l.documents,
                productivityScore = DashboardMath.productivityScore(
                    completedToday = t.summary.completedToday,
                    openDueToday = t.summary.dueTodayOpen,
                    overdue = t.summary.overdue,
                ),
            )
        }
    }

    override suspend fun ensureWidgets() {
        val stored = db.dashboardWidgetDao().getAll()
        val normalized = DashboardMath.normalizeWidgets(stored.map { it.widgetKey to it.isVisible })
        if (stored.size != normalized.size || stored.map { it.widgetKey } != normalized.map { it.type.name }) {
            db.dashboardWidgetDao().replaceAll(normalized.map { it.toEntity() })
        }
    }

    override suspend fun saveWidgets(widgets: List<DashboardWidget>) {
        db.dashboardWidgetDao().replaceAll(widgets.mapIndexed { i, w -> w.copy(position = i).toEntity() })
    }

    override suspend fun resetWidgets() {
        db.dashboardWidgetDao().replaceAll(DashboardMath.defaultWidgets().map { it.toEntity() })
    }

    private fun DashboardWidget.toEntity() = DashboardWidgetEntity(type.name, position, isVisible)

    private data class HeaderPart(
        val name: String?,
        val widgets: List<DashboardWidget>,
        val events: List<ScheduleItem>,
        val upcomingEvents: Int,
        val reminders: List<ReminderItem>,
    )

    private data class TaskPart(val items: List<ScheduleItem>, val summary: TaskSummary)

    private data class LifePart(
        val goals: List<GoalItem>,
        val family: com.lifeos.app.domain.model.FamilySummary,
        val notes: List<NoteItem>,
        val documents: List<DocumentItem>,
    )
}
