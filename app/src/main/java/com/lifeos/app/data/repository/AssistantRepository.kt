package com.lifeos.app.data.repository

import com.lifeos.app.core.format.IndianFormat
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.core.time.dayRange
import com.lifeos.app.core.time.monthKey
import com.lifeos.app.core.time.monthRange
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.entity.AiConversationEntity
import com.lifeos.app.data.local.entity.AiMessageEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.data.local.entity.SavingsGoalEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.data.local.entity.TransactionEntity
import com.lifeos.app.domain.assistant.Command
import com.lifeos.app.domain.assistant.CommandParser
import com.lifeos.app.domain.assistant.Period
import com.lifeos.app.domain.model.MessageRole
import com.lifeos.app.domain.model.ReminderType
import com.lifeos.app.domain.model.TaskStatus
import com.lifeos.app.domain.model.TransactionType
import com.lifeos.app.core.time.InstantRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** An action that needs the user's explicit "yes" before it runs. */
data class PendingAction(val description: String, val taskId: Long)

data class AssistantReply(val text: String, val status: String?, val pending: PendingAction? = null)

/**
 * LIFE AI basic mode: parses a request with [CommandParser] and runs it against local data.
 * It only reports an action as done after the database write has returned.
 */
@Singleton
class AssistantRepository @Inject constructor(
    private val db: LifeOsDatabase,
    private val finance: FinanceRepository,
    private val tasks: TaskRepository,
    private val notes: NoteRepository,
    private val reminders: ReminderEngine,
    private val calendar: CalendarRepository,
    private val health: HealthRepository,
    private val time: TimeProvider,
) {
    fun observeConversations(): Flow<List<AiConversationEntity>> = db.aiDao().observeConversations()
    fun observeMessages(conversationId: Long): Flow<List<AiMessageEntity>> = db.aiDao().observeMessages(conversationId)

    suspend fun newConversation(): Long {
        val now = time.now()
        return db.aiDao().insertConversation(AiConversationEntity(title = "New conversation", createdAt = now, updatedAt = now))
    }

    suspend fun deleteConversation(c: AiConversationEntity) = db.aiDao().deleteConversation(c)

    suspend fun record(conversationId: Long, role: MessageRole, text: String, status: String? = null) {
        val now = time.now()
        db.aiDao().insertMessage(AiMessageEntity(conversationId = conversationId, role = role, text = text, actionStatus = status, createdAt = now))
        db.aiDao().getConversation(conversationId)?.let { c ->
            val title = if (c.title == "New conversation" && role == MessageRole.USER) text.take(48) else c.title
            db.aiDao().updateConversation(c.copy(title = title, updatedAt = now))
        }
    }

    suspend fun handle(input: String): AssistantReply {
        val zone = time.zone()
        val now = time.now()
        val today = now.atZone(zone).toLocalDate()
        return try {
            execute(CommandParser.parse(input, today))
        } catch (e: Exception) {
            AssistantReply("I couldn't complete that: ${e.message ?: "unexpected error"}. Nothing was changed.", "FAILED")
        }
    }

    /** Runs a confirmed destructive action. */
    suspend fun confirm(action: PendingAction): AssistantReply {
        val task = db.taskDao().getById(action.taskId) ?: return AssistantReply("That task no longer exists, so nothing was deleted.", "FAILED")
        tasks.deleteTask(task)
        return AssistantReply("Deleted the task “${task.title}”.", "EXECUTED")
    }

    private fun range(period: Period): InstantRange {
        val zone = time.zone()
        val today = time.now().atZone(zone).toLocalDate()
        return when (period) {
            Period.TODAY -> today.dayRange(zone)
            Period.THIS_WEEK -> InstantRange(today.with(DayOfWeek.MONDAY).dayRange(zone).start, today.dayRange(zone).end)
            Period.THIS_MONTH -> YearMonth.from(today).monthRange(zone)
            Period.LAST_MONTH -> YearMonth.from(today).minusMonths(1).monthRange(zone)
        }
    }

    private suspend fun execute(cmd: Command): AssistantReply {
        val zone = time.zone()
        val now = time.now()
        val today = now.atZone(zone).toLocalDate()
        return when (cmd) {
            is Command.SpendingQuery -> {
                val r = range(cmd.period)
                val spent = finance.totalFor(TransactionType.EXPENSE, r.start, r.end)
                val top = finance.categoryTotals(TransactionType.EXPENSE, r.start, r.end).firstOrNull()
                if (spent == 0L) AssistantReply("You haven't recorded any expenses ${cmd.period.label}.", null)
                else AssistantReply(
                    "You spent ${IndianFormat.rupees(spent)} ${cmd.period.label}." +
                        (top?.let { " The biggest category was ${it.category.label} (${IndianFormat.rupees(it.totalPaise, false)})." } ?: ""),
                    null,
                )
            }
            is Command.IncomeQuery -> {
                val r = range(cmd.period)
                val inc = finance.totalFor(TransactionType.INCOME, r.start, r.end)
                AssistantReply(if (inc == 0L) "No income recorded ${cmd.period.label}." else "You recorded ${IndianFormat.rupees(inc)} of income ${cmd.period.label}.", null)
            }
            is Command.ExpensesByCategory -> {
                val r = range(cmd.period)
                val totals = finance.categoryTotals(TransactionType.EXPENSE, r.start, r.end)
                if (totals.isEmpty()) AssistantReply("No expenses recorded ${cmd.period.label}.", null)
                else {
                    val sum = totals.sumOf { it.totalPaise }.coerceAtLeast(1)
                    AssistantReply(
                        "Expenses ${cmd.period.label} by category:\n" + totals.joinToString("\n") {
                            "• ${it.category.label}: ${IndianFormat.rupees(it.totalPaise, false)} (${Math.round(it.totalPaise * 100.0 / sum)}%)"
                        },
                        null,
                    )
                }
            }
            is Command.BudgetStatus -> {
                val month = YearMonth.from(today)
                val budget = db.budgetDao().observeTotalLimit(month.monthKey()).first()
                val r = month.monthRange(zone)
                val spent = finance.totalFor(TransactionType.EXPENSE, r.start, r.end)
                if (budget == 0L) AssistantReply("You haven't set a budget for this month yet. Add one in Finance → Budgets.", null)
                else AssistantReply("Budget this month: ${IndianFormat.rupees(budget, false)}. Spent: ${IndianFormat.rupees(spent, false)}. Remaining: ${IndianFormat.rupees(budget - spent, false)}.", null)
            }
            is Command.AddExpense, is Command.AddIncome -> {
                val isExpense = cmd is Command.AddExpense
                val amount = if (cmd is Command.AddExpense) cmd.amountPaise else (cmd as Command.AddIncome).amountPaise
                val category = if (cmd is Command.AddExpense) cmd.category else com.lifeos.app.domain.model.TransactionCategory.SALARY
                val note = if (cmd is Command.AddExpense) cmd.note else (cmd as Command.AddIncome).note
                finance.saveTransaction(
                    TransactionEntity(
                        type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                        amountPaise = amount, category = category, note = note, occurredAt = now, createdAt = now,
                    ),
                )
                AssistantReply(
                    "Recorded ${if (isExpense) "an expense" else "income"} of ${IndianFormat.rupees(amount)}" +
                        (if (isExpense) " under ${category.label}" else "") + (note?.let { " — $it" } ?: "") + ".",
                    "EXECUTED",
                )
            }
            is Command.CreateReminder -> {
                val t = cmd.time ?: LocalTime.of(9, 0)
                var at = cmd.date.atTime(t).atZone(zone).toInstant()
                if (!at.isAfter(now) && cmd.time == null && cmd.date == today) at = now.plusSeconds(3600)
                if (!at.isAfter(now)) return AssistantReply("That time has already passed, so I didn't create the reminder. Try a later time.", "FAILED")
                reminders.save(ReminderEntity(title = cmd.title, type = guessReminderType(cmd.title), triggerAt = at, createdAt = now))
                val warn = if (!reminders.exactAlarmsAllowed) " (Exact alarms are off, so Android may deliver it a few minutes late — you can allow them in Settings.)" else ""
                AssistantReply("Reminder set: “${cmd.title}” — ${TimeFormat.relative(at, today, zone)}.$warn", "EXECUTED")
            }
            Command.ShowPendingTasks -> {
                val open = tasks.getOpen(10)
                if (open.isEmpty()) AssistantReply("You have no pending tasks. 🎉", null)
                else AssistantReply(
                    "Your pending tasks:\n" + open.joinToString("\n") { t ->
                        "• ${t.title}" + (t.dueAt?.let { " — due ${TimeFormat.relative(it, today, zone)}" } ?: "") + " [${t.priority.label}]"
                    },
                    null,
                )
            }
            is Command.AddTask -> {
                val due = cmd.due?.let { d -> d.atTime(cmd.time ?: LocalTime.of(18, 0)).atZone(zone).toInstant() }
                tasks.saveTask(TaskEntity(title = cmd.title, dueAt = due, createdAt = now, updatedAt = now), remindAt = null)
                AssistantReply("Added the task “${cmd.title}”" + (due?.let { ", due ${TimeFormat.relative(it, today, zone)}" } ?: "") + ".", "EXECUTED")
            }
            is Command.CompleteTask -> {
                val matches = tasks.findOpen(cmd.query)
                when {
                    matches.isEmpty() -> AssistantReply("I couldn't find an open task matching “${cmd.query}”.", "FAILED")
                    matches.size > 1 -> AssistantReply("Several tasks match: " + matches.take(5).joinToString { "“${it.title}”" } + ". Please be more specific.", null)
                    else -> {
                        tasks.setStatus(matches.first(), TaskStatus.DONE)
                        AssistantReply("Marked “${matches.first().title}” as done.", "EXECUTED")
                    }
                }
            }
            is Command.DeleteTask -> {
                val matches = tasks.findOpen(cmd.query)
                when {
                    matches.isEmpty() -> AssistantReply("I couldn't find an open task matching “${cmd.query}”. Nothing was deleted.", null)
                    matches.size > 1 -> AssistantReply("Several tasks match: " + matches.take(5).joinToString { "“${it.title}”" } + ". Please be more specific.", null)
                    else -> AssistantReply(
                        "Delete the task “${matches.first().title}”? This can't be undone.",
                        "NEEDS_CONFIRMATION",
                        PendingAction("Delete “${matches.first().title}”", matches.first().id),
                    )
                }
            }
            is Command.CreateSavingsGoal -> {
                finance.saveSavingsGoal(SavingsGoalEntity(title = cmd.title, targetPaise = cmd.amountPaise, createdAt = now))
                AssistantReply("Created the savings goal “${cmd.title}” for ${IndianFormat.rupees(cmd.amountPaise, false)}. Track it in Finance → Savings.", "EXECUTED")
            }
            is Command.AppointmentsQuery -> {
                val r = cmd.date.dayRange(zone)
                val list = health.appointmentsBetween(r.start, r.end)
                val label = if (cmd.date == today) "today" else if (cmd.date == today.plusDays(1)) "tomorrow" else "on ${TimeFormat.shortDate(cmd.date)}"
                if (list.isEmpty()) AssistantReply("You have no appointments $label.", null)
                else AssistantReply("Appointments $label:\n" + list.joinToString("\n") { "• ${TimeFormat.time(it.at, zone)} — ${it.title}" + (it.withWhom?.let { w -> " with $w" } ?: "") }, null)
            }
            is Command.ScheduleQuery -> {
                val r = cmd.date.dayRange(zone)
                val events = calendar.eventsBetween(r.start, r.end)
                val appts = health.appointmentsBetween(r.start, r.end)
                val items = events.map { it.startAt to it.title } + appts.map { it.at to "Appointment: ${it.title}" }
                if (items.isEmpty()) AssistantReply("Nothing is scheduled ${if (cmd.date == today) "today" else "on ${TimeFormat.shortDate(cmd.date)}"}.", null)
                else AssistantReply(items.sortedBy { it.first }.joinToString("\n") { "• ${TimeFormat.time(it.first, zone)} — ${it.second}" }, null)
            }
            Command.SummarizeWeek -> summarizeWeek()
            is Command.AddNote -> {
                notes.quickAdd(cmd.text)
                AssistantReply("Saved a note: “${cmd.text.take(80)}”.", "EXECUTED")
            }
            Command.Help -> AssistantReply(HELP, null)
            is Command.Unknown -> AssistantReply(
                "I didn't understand that. I'm running in basic mode, which understands specific commands rather than free conversation. Try:\n$EXAMPLES",
                null,
            )
        }
    }

    private suspend fun summarizeWeek(): AssistantReply {
        val zone = time.zone()
        val today = time.now().atZone(zone).toLocalDate()
        val start = today.minusDays(6).dayRange(zone).start
        val end = today.dayRange(zone).end
        val completed = db.taskDao().countCompletedBetween(start, end)
        val spent = finance.totalFor(TransactionType.EXPENSE, start, end)
        val income = finance.totalFor(TransactionType.INCOME, start, end)
        val top = finance.categoryTotals(TransactionType.EXPENSE, start, end).firstOrNull()
        val open = db.taskDao().getOpen(100).size
        val upcoming = calendar.eventsBetween(end, end.plusSeconds(7 * 86_400L)).size
        val habitsDone = db.habitDao().getAllEntries().count { !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today) }
        val lines = buildList {
            add("Your last 7 days:")
            add("• Tasks completed: $completed (open now: $open)")
            add("• Spent: ${IndianFormat.rupees(spent, false)}" + (top?.let { ", mostly on ${it.category.label}" } ?: ""))
            if (income > 0) add("• Income: ${IndianFormat.rupees(income, false)}")
            add("• Habit check-ins: $habitsDone")
            add("• Events in the next 7 days: $upcoming")
        }
        return AssistantReply(lines.joinToString("\n"), null)
    }

    private fun guessReminderType(title: String): ReminderType {
        val t = title.lowercase()
        return when {
            listOf("bill", "pay", "emi", "rent", "recharge").any { t.contains(it) } -> ReminderType.BILL
            listOf("birthday").any { t.contains(it) } -> ReminderType.BIRTHDAY
            listOf("medicine", "tablet", "pill").any { t.contains(it) } -> ReminderType.MEDICINE
            listOf("doctor", "appointment").any { t.contains(it) } -> ReminderType.APPOINTMENT
            else -> ReminderType.GENERAL
        }
    }

    companion object {
        val EXAMPLES = listOf(
            "How much did I spend this month?",
            "Show my expenses by category",
            "I spent 450 on lunch",
            "Remind me to pay electricity bill tomorrow at 10am",
            "Show my pending tasks",
            "Add a task to complete my project by Friday",
            "Create a savings goal of ₹50,000 for a laptop",
            "What appointments do I have tomorrow?",
            "Summarize my week",
            "Note: buy gifts for Diwali",
        ).joinToString("\n") { "• $it" }

        val HELP = "LIFE AI basic mode works offline and acts on your own data. I can answer questions about spending, " +
            "income, budgets, tasks, appointments and your schedule, and I can add expenses, income, tasks, notes, reminders " +
            "and savings goals. I always ask before deleting anything.\n\nExamples:\n$EXAMPLES"
    }
}
