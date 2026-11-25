package com.lifeos.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.component.AnimatedBarChart
import com.lifeos.app.core.designsystem.component.BarDatum
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.GlowProgressBar
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.ProgressRing
import com.lifeos.app.core.designsystem.component.StatBlock
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.IndianFormat
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.domain.model.WidgetType

// Each widget shows stored records only. Empty states route to the module that creates the data.

@Composable
internal fun ScheduleWidget(state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    val colors = LifeTheme.colors
    val items = state.snapshot.schedule
    GlassCard(onClick = { onOpenModule("calendar") }, onClickLabel = "Open calendar") {
        CardHeader(Icons.Outlined.CalendarToday, WidgetType.TODAY_SCHEDULE.title)
        Spacer(Modifier.height(Spacing.md))
        if (items.isEmpty()) {
            EmptyState(
                Icons.Outlined.CalendarToday, "Nothing scheduled today",
                "Events and tasks due today will appear here.",
            )
        } else {
            items.take(5).forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(width = 4.dp, height = 32.dp).clip(Radii.pill)
                            .background(if (item.isTask) colors.accentPurple else colors.accentCyan),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                item.allDay -> "All day"
                                item.isTask -> "Task · due ${TimeFormat.time(item.startAt, state.zone)}"
                                else -> "${TimeFormat.time(item.startAt, state.zone)} – ${TimeFormat.time(item.endAt, state.zone)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            if (items.size > 5) {
                Text("+${items.size - 5} more", style = MaterialTheme.typography.labelMedium, color = colors.accentCyan)
            }
        }
    }
}

@Composable
internal fun FinanceWidget(state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    val colors = LifeTheme.colors
    val f = state.snapshot.finance
    val hasData = f.incomePaise > 0 || f.expensePaise > 0 || f.lastSixMonthsExpense.any { it.paise > 0 }
    GlassCard(onClick = { onOpenModule("finance") }, onClickLabel = "Open finance") {
        CardHeader(Icons.Outlined.AccountBalanceWallet, WidgetType.FINANCE_OVERVIEW.title)
        Spacer(Modifier.height(Spacing.md))
        if (!hasData) {
            EmptyState(
                Icons.Outlined.AccountBalanceWallet, "No transactions yet",
                "Record income and expenses in Finance to see your month here.",
                actionLabel = "Open Finance", onAction = { onOpenModule("finance") },
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatBlock("Income", IndianFormat.rupeesCompact(f.incomePaise), Modifier.weight(1f), colors.success)
                StatBlock("Spent", IndianFormat.rupeesCompact(f.expensePaise), Modifier.weight(1f))
                StatBlock(
                    if (f.remainingBudgetPaise != null) "Budget left" else "Net",
                    IndianFormat.rupeesCompact(f.remainingBudgetPaise ?: f.netPaise),
                    Modifier.weight(1f),
                    if ((f.remainingBudgetPaise ?: f.netPaise) < 0) colors.danger else colors.textPrimary,
                )
            }
            if (f.budgetPaise != null && f.budgetPaise > 0) {
                Spacer(Modifier.height(Spacing.sm))
                GlowProgressBar(
                    progress = (f.expensePaise.toFloat() / f.budgetPaise.toFloat()),
                    color = if (f.expensePaise > f.budgetPaise) colors.danger else null,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text("Spending · last 6 months", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            Spacer(Modifier.height(Spacing.xs))
            AnimatedBarChart(
                data = f.lastSixMonthsExpense.map { BarDatum(TimeFormat.monthShort(it.month), it.paise.toFloat()) },
                height = 96.dp,
                accessibilitySummary = "Monthly spending: " + f.lastSixMonthsExpense.joinToString { "${TimeFormat.monthShort(it.month)} ${IndianFormat.rupees(it.paise, false)}" },
            )
        }
    }
}

@Composable
internal fun TaskWidget(state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    val colors = LifeTheme.colors
    val t = state.snapshot.tasks
    val totalToday = t.completedToday + t.dueTodayOpen
    GlassCard(onClick = { onOpenModule("tasks") }, onClickLabel = "Open tasks") {
        CardHeader(Icons.Outlined.CheckCircle, WidgetType.TASK_COMPLETION.title)
        Spacer(Modifier.height(Spacing.md))
        if (totalToday == 0 && t.openTotal == 0 && t.overdue == 0) {
            EmptyState(
                Icons.Outlined.CheckCircle, "No tasks yet",
                "Create tasks with due dates to track today's completion.",
                actionLabel = "Open Tasks", onAction = { onOpenModule("tasks") },
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = if (totalToday == 0) 0f else t.completedToday.toFloat() / totalToday,
                    label = if (totalToday == 0) "—" else "${t.completedToday}/$totalToday",
                    subLabel = "today",
                )
                Spacer(Modifier.width(Spacing.lg))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    LifeChip("${t.openTotal} open", colors.accentCyan)
                    if (t.overdue > 0) LifeChip("${t.overdue} overdue", colors.danger)
                    state.snapshot.productivityScore?.let { LifeChip("Productivity $it", colors.accentPurple) }
                }
            }
        }
    }
}

@Composable
internal fun RemindersWidget(state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    val colors = LifeTheme.colors
    val reminders = state.snapshot.reminders
    GlassCard(onClick = { onOpenModule("calendar") }, onClickLabel = "Open reminders") {
        CardHeader(Icons.Outlined.NotificationsActive, WidgetType.UPCOMING_REMINDERS.title)
        Spacer(Modifier.height(Spacing.md))
        if (reminders.isEmpty()) {
            EmptyState(Icons.Outlined.NotificationsActive, "No upcoming reminders", "Bill, birthday and task reminders will be listed here.")
        } else {
            reminders.take(3).forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(r.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(TimeFormat.relative(r.triggerAt, state.today, state.zone), style = MaterialTheme.typography.bodySmall, color = colors.accentCyan)
                }
            }
        }
    }
}

@Composable
internal fun GoalsWidget(state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    val colors = LifeTheme.colors
    val goals = state.snapshot.goals
    GlassCard(onClick = { onOpenModule("goals") }, onClickLabel = "Open goals") {
        CardHeader(Icons.Outlined.EmojiEvents, WidgetType.ACTIVE_GOALS.title)
        Spacer(Modifier.height(Spacing.md))
        if (goals.isEmpty()) {
            EmptyState(Icons.Outlined.EmojiEvents, "No active goals", "Savings, learning and fitness goals will track progress here.")
        } else {
            goals.take(3).forEach { g ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(g.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("${Math.round(g.progress * 100)}%", style = MaterialTheme.typography.labelLarge, color = colors.accentCyan)
                    }
                    Spacer(Modifier.height(6.dp))
                    GlowProgressBar(g.progress)
                }
            }
        }
    }
}

@Composable
internal fun FamilyWidget(state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    val colors = LifeTheme.colors
    val f = state.snapshot.family
    GlassCard(onClick = { onOpenModule("family") }, onClickLabel = "Open family") {
        CardHeader(Icons.Outlined.FamilyRestroom, WidgetType.FAMILY_OVERVIEW.title)
        Spacer(Modifier.height(Spacing.md))
        if (f.memberCount == 0) {
            EmptyState(Icons.Outlined.FamilyRestroom, "No family members added", "Profiles, birthdays and emergency contacts stay private on this device.")
        } else {
            StatBlock("Members", f.memberCount.toString())
            if (f.nextBirthdayName != null && f.nextBirthdayDate != null) {
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Cake, contentDescription = null, tint = colors.accentPurple, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    val whenText = when (f.daysUntilBirthday) {
                        0L -> "today"
                        1L -> "tomorrow"
                        else -> "on ${TimeFormat.shortDate(f.nextBirthdayDate)}"
                    }
                    Text("${f.nextBirthdayName}'s birthday $whenText", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
internal fun NotesWidget(state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    val colors = LifeTheme.colors
    val notes = state.snapshot.notes
    GlassCard(onClick = { onOpenModule("notes") }, onClickLabel = "Open notes") {
        CardHeader(Icons.Outlined.StickyNote2, WidgetType.QUICK_NOTES.title)
        Spacer(Modifier.height(Spacing.md))
        if (notes.isEmpty()) {
            EmptyState(Icons.Outlined.StickyNote2, "No notes yet", "Pinned and recent notes will show here.")
        } else {
            notes.forEach { n ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (n.isPinned) {
                            Icon(Icons.Outlined.PushPin, contentDescription = "Pinned", tint = colors.accentPurple, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(n.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (n.preview.isNotBlank()) {
                        Text(n.preview, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
internal fun DocumentsWidget(state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    val colors = LifeTheme.colors
    val docs = state.snapshot.documents
    GlassCard(onClick = { onOpenModule("documents") }, onClickLabel = "Open documents") {
        CardHeader(Icons.Outlined.Description, WidgetType.RECENT_DOCUMENTS.title)
        Spacer(Modifier.height(Spacing.md))
        if (docs.isEmpty()) {
            EmptyState(Icons.Outlined.Description, "Vault is empty", "Imported PDFs, images and files are stored on this device only.")
        } else {
            docs.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = colors.accentCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(d.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    LifeChip(d.category.label, colors.accentPurple)
                }
            }
        }
    }
}
