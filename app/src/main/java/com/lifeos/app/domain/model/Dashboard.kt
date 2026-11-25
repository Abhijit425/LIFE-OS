package com.lifeos.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

data class DashboardWidget(val type: WidgetType, val position: Int, val isVisible: Boolean)

data class ScheduleItem(val title: String, val startAt: Instant, val endAt: Instant, val allDay: Boolean, val isTask: Boolean)

data class ReminderItem(val id: Long, val title: String, val type: ReminderType, val triggerAt: Instant)

data class GoalItem(val id: Long, val title: String, val progress: Float, val currentValue: Double, val targetValue: Double, val unit: String)

data class NoteItem(val id: Long, val title: String, val preview: String, val isPinned: Boolean)

data class DocumentItem(val id: Long, val name: String, val category: DocumentCategory, val addedAt: Instant)

data class MonthlyAmount(val month: YearMonth, val paise: Long)

data class FinanceSummary(
    val incomePaise: Long,
    val expensePaise: Long,
    /** null when the user has not set any budget for this month. */
    val budgetPaise: Long?,
    val lastSixMonthsExpense: List<MonthlyAmount>,
) {
    val netPaise: Long get() = incomePaise - expensePaise
    val remainingBudgetPaise: Long? get() = budgetPaise?.let { it - expensePaise }
}

data class TaskSummary(
    val dueTodayOpen: Int,
    val completedToday: Int,
    val overdue: Int,
    val openTotal: Int,
)

data class FamilySummary(
    val memberCount: Int,
    val nextBirthdayName: String?,
    val nextBirthdayDate: LocalDate?,
    val daysUntilBirthday: Long?,
)

/** Everything the home screen shows — every value is derived from stored records. */
data class DashboardSnapshot(
    val displayName: String?,
    val widgets: List<DashboardWidget>,
    val schedule: List<ScheduleItem>,
    val upcomingEventsCount: Int,
    val finance: FinanceSummary,
    val tasks: TaskSummary,
    val reminders: List<ReminderItem>,
    val goals: List<GoalItem>,
    val family: FamilySummary,
    val notes: List<NoteItem>,
    val documents: List<DocumentItem>,
    /** null when there's nothing to score yet (no tasks due/completed today). */
    val productivityScore: Int?,
) {
    /** Average progress across active goals, or null if there are none. */
    val averageGoalProgress: Float?
        get() = if (goals.isEmpty()) null else goals.map { it.progress }.average().toFloat()
}
