package com.lifeos.app.feature.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.ChoiceChips
import com.lifeos.app.core.designsystem.component.DateField
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.FieldLabel
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ProgressRing
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SegmentedTabs
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.designsystem.theme.WidthClass
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.entity.GoalEntity
import com.lifeos.app.data.local.entity.HabitEntity
import com.lifeos.app.data.repository.GoalRepository
import com.lifeos.app.data.repository.InsightsRepository
import com.lifeos.app.domain.logic.GoalMilestones
import com.lifeos.app.domain.logic.StreakMath
import com.lifeos.app.domain.model.GoalType
import com.lifeos.app.domain.model.HabitFrequency
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.screenContentPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repo: GoalRepository,
    insights: InsightsRepository,
    private val time: TimeProvider,
) : LifeViewModel() {
    val goals = repo.observeGoals().stateWhileSubscribed(emptyList())
    val habits = repo.observeHabits().stateWhileSubscribed(emptyList())
    val entries = repo.observeEntries().stateWhileSubscribed(emptyList())
    val badges = insights.observeBadges().stateWhileSubscribed(emptyList())
    fun today() = repo.today()
    fun now() = time.now()

    fun saveGoal(g: GoalEntity) = act(if (g.targetValue > 0 && g.currentValue >= g.targetValue) "🎉 Goal reached!" else "Goal saved.") { repo.saveGoal(g) }
    fun deleteGoal(g: GoalEntity) = act("Goal deleted.") { repo.deleteGoal(g) }
    fun saveHabit(h: HabitEntity) = act("Habit saved.") { repo.saveHabit(h) }
    fun deleteHabit(h: HabitEntity) = act("Habit deleted.") { repo.deleteHabit(h) }
    fun toggle(h: HabitEntity, done: Boolean) = act { repo.toggle(h.id, today(), done) }
}

private enum class GoalsTab(val label: String) { GOALS("Goals"), HABITS("Habits"), BADGES("Badges") }

@Composable
fun GoalsScreen(onBack: () -> Unit, vm: GoalsViewModel = hiltViewModel()) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val habits by vm.habits.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(GoalsTab.GOALS) }
    var goalEdit by remember { mutableStateOf<Pair<Boolean, GoalEntity?>?>(null) }
    var progressFor by remember { mutableStateOf<GoalEntity?>(null) }
    var habitEdit by remember { mutableStateOf<Pair<Boolean, HabitEntity?>?>(null) }
    val colors = LifeTheme.colors
    val today = vm.today()
    val datesByHabit = entries.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }
    val columns = when (LifeTheme.layout.widthClass) { WidthClass.Compact -> 1; WidthClass.Medium -> 2; WidthClass.Expanded -> 3 }

    MessageBox(vm) {
        LazyVerticalGrid(
            GridCells.Fixed(if (tab == GoalsTab.BADGES) columns + 1 else columns), contentPadding = screenContentPadding(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    ScreenHeader("Goals", "Targets, habits and achievements", onBack = onBack) {
                        when (tab) {
                            GoalsTab.GOALS -> LifePrimaryButton("Goal", onClick = { goalEdit = true to null }, icon = Icons.Outlined.Add)
                            GoalsTab.HABITS -> LifePrimaryButton("Habit", onClick = { habitEdit = true to null }, icon = Icons.Outlined.Add)
                            GoalsTab.BADGES -> {}
                        }
                    }
                    SegmentedTabs(GoalsTab.entries, tab, { it.label }, { tab = it })
                }
            }
            when (tab) {
                GoalsTab.GOALS -> {
                    if (goals.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                        GlassCard { EmptyState(Icons.Outlined.EmojiEvents, "No goals yet", "Examples: save ₹1,00,000 · read 12 books · learn Flutter.") }
                    }
                    items(goals, key = { it.id }) { g ->
                        val p = if (g.targetValue > 0) (g.currentValue / g.targetValue).toFloat().coerceIn(0f, 1f) else 0f
                        GlassCard(onClick = { progressFor = g }, onClickLabel = "Update progress for ${g.title}") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProgressRing(p, size = 84.dp, strokeWidth = 8.dp, label = "${Math.round(p * 100)}%")
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text(g.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text("${fmt(g.currentValue)} / ${fmt(g.targetValue)} ${g.unit}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    g.targetDate?.let { Text("By ${TimeFormat.shortDate(it)} ${it.year}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
                                    if (g.isCompleted) LifeChip("Completed", colors.success)
                                }
                                TextButton(onClick = { goalEdit = true to g }) { Text("Edit", color = colors.accentCyan) }
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GoalMilestones.reached(p).forEachIndexed { i, ok ->
                                    LifeChip("${(GoalMilestones.thresholds[i] * 100).toInt()}%", if (ok) colors.success else colors.textTertiary)
                                }
                            }
                        }
                    }
                }
                GoalsTab.HABITS -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val weekDone = habits.sumOf { StreakMath.completionsThisWeek(datesByHabit[it.id].orEmpty(), today) }
                        GlassCard(strong = true) {
                            CardHeader(Icons.Outlined.LocalFireDepartment, "This week")
                            Text(
                                if (habits.isEmpty()) "Add a habit to start a streak."
                                else "$weekDone check-ins across ${habits.size} habit${if (habits.size == 1) "" else "s"} since Monday.",
                                style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary,
                            )
                        }
                    }
                    items(habits, key = { it.id }) { h ->
                        val dates = datesByHabit[h.id].orEmpty()
                        val doneToday = today in dates
                        GlassCard(onClick = { habitEdit = true to h }, onClickLabel = "Edit ${h.title}") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.toggle(h, !doneToday) }) {
                                    Icon(if (doneToday) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        if (doneToday) "Undo today for ${h.title}" else "Mark ${h.title} done today", tint = if (doneToday) colors.success else colors.textSecondary)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(h.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text("🔥 ${StreakMath.currentStreak(dates, today)}-day streak · best ${StreakMath.longestStreak(dates)}",
                                        style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (6 downTo 0).forEach { i ->
                                    val d = today.minusDays(i.toLong())
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.size(18.dp).clip(CircleShape).background(if (d in dates) colors.accentCyan else colors.glassFillStrong))
                                        Text(d.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
                GoalsTab.BADGES -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("${badges.count { it.earned }} of ${badges.size} earned — based on your real activity.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    items(badges, key = { it.id }) { b ->
                        GlassCard(Modifier.alpha(if (b.earned) 1f else 0.5f), strong = b.earned) {
                            Icon(Icons.Outlined.WorkspacePremium, null, tint = if (b.earned) colors.warning else colors.textTertiary, modifier = Modifier.size(36.dp))
                            Text(b.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(b.description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Text(if (b.earned) "Earned" else "Locked", style = MaterialTheme.typography.labelSmall, color = if (b.earned) colors.success else colors.textTertiary)
                        }
                    }
                }
            }
        }
    }

    goalEdit?.let { (_, g) -> GoalSheet(g, vm) { goalEdit = null } }
    progressFor?.let { g -> ProgressSheet(g, vm) { progressFor = null } }
    habitEdit?.let { (_, h) -> HabitSheet(h, vm) { habitEdit = null } }
}

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v)

@Composable
private fun GoalSheet(item: GoalEntity?, vm: GoalsViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var type by remember { mutableStateOf(item?.type ?: GoalType.LEARNING) }
    var target by remember { mutableStateOf(item?.targetValue?.let { fmt(it) } ?: "") }
    var current by remember { mutableStateOf(item?.currentValue?.let { fmt(it) } ?: "0") }
    var unit by remember { mutableStateOf(item?.unit ?: "") }
    var date by remember { mutableStateOf(item?.targetDate) }
    val t = target.toDoubleOrNull()
    val c = current.toDoubleOrNull()
    val valid = title.isNotBlank() && t != null && t > 0 && c != null && c >= 0 && unit.isNotBlank()
    LifeSheet(if (item == null) "New goal" else "Edit goal", onDismiss, saveEnabled = valid,
        onSave = {
            vm.saveGoal((item ?: GoalEntity(title = title, type = type, targetValue = 1.0, unit = unit, createdAt = vm.now())).copy(
                title = title.trim(), type = type, targetValue = t ?: 1.0, currentValue = c ?: 0.0, unit = unit.trim(), targetDate = date,
                isCompleted = (c ?: 0.0) >= (t ?: 1.0)))
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteGoal(it); onDismiss() } },
    ) {
        if (item?.savingsGoalId != null) Text("This goal mirrors a Finance savings goal; changes sync both ways.", style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textTertiary)
        LifeTextField(title, { title = it.take(80) }, "Goal (e.g. Read 12 books)", Modifier.fillMaxWidth())
        FieldLabel("Type")
        ChoiceChips(GoalType.entries, type, { it.name.lowercase().replaceFirstChar { ch -> ch.uppercase() } }, { type = it; if (unit.isBlank()) unit = defaultUnit(it) })
        Row {
            LifeTextField(target, { target = it.take(12) }, "Target", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.width(Spacing.sm))
            LifeTextField(unit, { unit = it.take(15) }, "Unit", Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(current, { current = it.take(12) }, "Progress so far", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
        DateField("Target date", date, { date = it })
    }
}

private fun defaultUnit(t: GoalType) = when (t) {
    GoalType.SAVINGS -> "₹"
    GoalType.READING -> "books"
    GoalType.FITNESS -> "sessions"
    GoalType.LEARNING -> "lessons"
    GoalType.PROJECT -> "%"
    GoalType.HABIT -> "days"
    GoalType.OTHER -> "units"
}

@Composable
private fun ProgressSheet(g: GoalEntity, vm: GoalsViewModel, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(fmt(g.currentValue)) }
    val v = value.toDoubleOrNull()
    LifeSheet("Update progress", onDismiss, saveEnabled = v != null && v >= 0, onSave = {
        vm.saveGoal(g.copy(currentValue = v ?: g.currentValue, isCompleted = (v ?: 0.0) >= g.targetValue))
        onDismiss()
    }) {
        Text(g.title, style = MaterialTheme.typography.titleMedium, color = LifeTheme.colors.textPrimary)
        Text("Target: ${fmt(g.targetValue)} ${g.unit}", style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textSecondary)
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { value = fmt(((v ?: 0.0) - 1).coerceAtLeast(0.0)) }) { Text("−1", color = LifeTheme.colors.textPrimary) }
            LifeTextField(value, { value = it.take(12) }, "Current (${g.unit})", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            TextButton(onClick = { value = fmt((v ?: 0.0) + 1) }) { Text("+1", color = LifeTheme.colors.textPrimary) }
        }
    }
}

@Composable
private fun HabitSheet(item: HabitEntity?, vm: GoalsViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var frequency by remember { mutableStateOf(item?.frequency ?: HabitFrequency.DAILY) }
    LifeSheet(if (item == null) "New habit" else "Edit habit", onDismiss, saveEnabled = title.isNotBlank(),
        onSave = {
            vm.saveHabit((item ?: HabitEntity(title = title, createdAt = vm.now())).copy(title = title.trim(), frequency = frequency))
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteHabit(it); onDismiss() } },
        deleteMessage = "The habit and its check-in history will be deleted.",
    ) {
        LifeTextField(title, { title = it.take(60) }, "Habit (e.g. Exercise 30 min)", Modifier.fillMaxWidth())
        FieldLabel("Frequency")
        ChoiceChips(HabitFrequency.entries, frequency, { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, { frequency = it })
    }
}
