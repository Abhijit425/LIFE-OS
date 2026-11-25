package com.lifeos.app.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.AnimatedBarChart
import com.lifeos.app.core.designsystem.component.BarDatum
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.DonutChart
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LoadingState
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.Slice
import com.lifeos.app.core.designsystem.component.StatBlock
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.IndianFormat
import com.lifeos.app.data.repository.Insights
import com.lifeos.app.data.repository.InsightsRepository
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.screenContentPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(repo: InsightsRepository) : LifeViewModel() {
    val insights = repo.observeInsights().stateWhileSubscribed<Insights?>(null)
}

/** Weekly report and productivity analytics, computed from stored records only. */
@Composable
fun InsightsScreen(onBack: () -> Unit, vm: InsightsViewModel = hiltViewModel()) {
    val data by vm.insights.collectAsStateWithLifecycle()
    val colors = LifeTheme.colors
    MessageBox(vm) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = screenContentPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item { ScreenHeader("Insights", "Your weekly report, from your own records", onBack = onBack) }
            val d = data
            if (d == null) {
                item { GlassCard { LoadingState() } }
                return@LazyColumn
            }
            item {
                val tasksWeek = d.tasksPerDay.sumOf { it.count }
                val change = if (d.spendingLastMonth > 0) ((d.spendingThisMonth - d.spendingLastMonth) * 100 / d.spendingLastMonth) else null
                GlassCard(strong = true) {
                    CardHeader(Icons.Outlined.Insights, "Weekly report")
                    Spacer(Modifier.height(Spacing.sm))
                    Row(Modifier.fillMaxWidth()) {
                        StatBlock("Tasks done (7d)", tasksWeek.toString(), Modifier.weight(1f))
                        StatBlock("Habit consistency", d.habitWeekRate?.let { "${Math.round(it * 100)}%" } ?: "—", Modifier.weight(1f))
                        StatBlock("Goal progress", d.goalAverage?.let { "${Math.round(it * 100)}%" } ?: "—", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    val lines = buildList {
                        add(if (tasksWeek == 0) "No tasks completed in the last 7 days." else "You completed $tasksWeek task${if (tasksWeek == 1) "" else "s"} in the last 7 days; your best day was ${d.tasksPerDay.maxBy { it.count }.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}.")
                        if (d.spendingThisMonth > 0) add("Spending this month: ${IndianFormat.rupees(d.spendingThisMonth, false)}" +
                            (change?.let { if (it >= 0) " — $it% more than all of last month so far." else " — ${-it}% less than last month." } ?: "."))
                        if (d.goalsCompleted > 0) add("Goals completed so far: ${d.goalsCompleted}.")
                    }
                    lines.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary) }
                }
            }
            item {
                GlassCard {
                    CardHeader(Icons.Outlined.CheckCircle, "Tasks completed per day")
                    Spacer(Modifier.height(Spacing.sm))
                    AnimatedBarChart(
                        d.tasksPerDay.map { BarDatum(it.date.dayOfWeek.name.take(2), it.count.toFloat()) },
                        accessibilitySummary = d.tasksPerDay.joinToString { "${it.date.dayOfWeek.name.lowercase()}: ${it.count}" },
                    )
                }
            }
            item {
                GlassCard {
                    CardHeader(Icons.Outlined.PieChart, "Where your money went this month")
                    Spacer(Modifier.height(Spacing.sm))
                    if (d.categories.isEmpty()) EmptyState(Icons.Outlined.PieChart, "No expenses yet", "Record expenses in Finance to see the breakdown.")
                    else DonutChart(d.categories.map { Slice(it.category.label, it.totalPaise.toFloat(), IndianFormat.rupeesCompact(it.totalPaise)) },
                        "this month", IndianFormat.rupeesCompact(d.spendingThisMonth))
                }
            }
        }
    }
}
