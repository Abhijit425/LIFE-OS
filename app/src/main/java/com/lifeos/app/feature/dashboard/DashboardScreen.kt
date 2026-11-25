package com.lifeos.app.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.AiOrb
import com.lifeos.app.core.designsystem.component.ErrorState
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LoadingState
import com.lifeos.app.core.designsystem.component.StatBlock
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.designsystem.theme.WidthClass
import com.lifeos.app.core.format.IndianFormat
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.dayPartFor
import com.lifeos.app.domain.model.DashboardSnapshot
import com.lifeos.app.domain.model.WidgetType
import com.lifeos.app.navigation.screenContentPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenModule: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showCustomize by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.refresh(userInitiated = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val s = state) {
                DashboardUiState.Loading -> Column(Modifier.padding(screenContentPadding())) {
                    GlassCard { LoadingState() }
                }
                is DashboardUiState.Error -> Column(Modifier.padding(screenContentPadding())) {
                    GlassCard { ErrorState(s.message, onRetry = { viewModel.refresh(userInitiated = true) }) }
                }
                is DashboardUiState.Ready -> DashboardContent(
                    state = s,
                    onOpenModule = onOpenModule,
                    onOpenSettings = onOpenSettings,
                    onCustomize = { showCustomize = true },
                    onNotifications = { showNotifications = true },
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
    }

    val ready = state as? DashboardUiState.Ready
    if (showCustomize && ready != null) {
        CustomizeDashboardSheet(
            widgets = ready.snapshot.widgets,
            onDismiss = { showCustomize = false },
            onToggle = { index, visible -> viewModel.setVisible(ready.snapshot.widgets, index, visible) },
            onMove = { index, delta -> viewModel.move(ready.snapshot.widgets, index, delta) },
            onReset = viewModel::resetWidgets,
        )
    }
    if (showNotifications && ready != null) {
        RemindersSheet(
            reminders = ready.snapshot.reminders,
            today = ready.today,
            zone = ready.zone,
            onDismiss = { showNotifications = false },
            onOpenCalendar = {
                showNotifications = false
                onOpenModule("calendar")
            },
        )
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Ready,
    onOpenModule: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCustomize: () -> Unit,
    onNotifications: () -> Unit,
) {
    val layout = LifeTheme.layout
    val snapshot = state.snapshot
    val visibleWidgets = snapshot.widgets.filter { it.isVisible }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(layout.dashboardColumns),
        contentPadding = screenContentPadding(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalItemSpacing = Spacing.md,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header", span = StaggeredGridItemSpan.FullLine) {
            DashboardHeader(state, onOpenSettings, onCustomize, onNotifications, onOpenAssistant = { onOpenModule("assistant") })
        }
        item(key = "hero", span = StaggeredGridItemSpan.FullLine) {
            HeroCard(state, onOpenAssistant = { onOpenModule("assistant") })
        }
        if (visibleWidgets.isEmpty()) {
            item(key = "none", span = StaggeredGridItemSpan.FullLine) {
                GlassCard(onClick = onCustomize, onClickLabel = "Customize dashboard") {
                    Text("All widgets are hidden", style = MaterialTheme.typography.titleMedium, color = LifeTheme.colors.textPrimary)
                    Text("Tap to choose what appears on your dashboard.", style = MaterialTheme.typography.bodyMedium, color = LifeTheme.colors.textSecondary)
                }
            }
        }
        visibleWidgets.forEachIndexed { index, widget ->
            item(key = widget.type.name) {
                AppearIn(index) {
                    DashboardWidgetCard(widget.type, state, onOpenModule)
                }
            }
        }
    }
}

/** Gentle staggered entrance for widgets; skipped under reduced motion. */
@Composable
private fun AppearIn(index: Int, content: @Composable () -> Unit) {
    if (LifeTheme.reducedMotion) {
        content()
        return
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(380, delayMillis = 40 * index)) +
            slideInVertically(tween(380, delayMillis = 40 * index)) { it / 8 },
    ) { content() }
}

@Composable
private fun DashboardHeader(
    state: DashboardUiState.Ready,
    onOpenSettings: () -> Unit,
    onCustomize: () -> Unit,
    onNotifications: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    val colors = LifeTheme.colors
    val snapshot = state.snapshot
    val greeting = dayPartFor(state.now.atZone(state.zone).hour).greeting
    Row(Modifier.fillMaxWidth().padding(bottom = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                TimeFormat.longDate(state.today).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.accentCyan,
            )
            Text(
                if (snapshot.displayName != null) "$greeting, ${snapshot.displayName}" else greeting,
                style = if (LifeTheme.layout.widthClass == WidthClass.Compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (snapshot.displayName == null) {
                Text(
                    "Add your name in Settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accentPurple,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onOpenSettings).padding(vertical = 4.dp),
                )
            }
        }
        Box(
            Modifier.size(48.dp).clip(Radii.pill).clickable(role = Role.Button, onClickLabel = "Open LIFE AI", onClick = onOpenAssistant),
            contentAlignment = Alignment.Center,
        ) { AiOrb(size = 40.dp) }
        IconButton(onClick = onNotifications) {
            val count = snapshot.reminders.size
            BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Upcoming reminders: $count", tint = colors.textPrimary)
            }
        }
        IconButton(onClick = onCustomize) {
            Icon(Icons.Outlined.Tune, contentDescription = "Customize dashboard", tint = colors.textPrimary)
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(Radii.pill)
                .background(colors.accentBrush)
                .clickable(role = Role.Button, onClickLabel = "Open profile and settings", onClick = onOpenSettings)
                .semantics { contentDescription = "Profile and settings" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                snapshot.displayName?.firstOrNull()?.uppercase() ?: "•",
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

/** The main "AI card". Its sentence is assembled by rules from stored counts — not generated by an AI model. */
@Composable
private fun HeroCard(state: DashboardUiState.Ready, onOpenAssistant: () -> Unit) {
    val colors = LifeTheme.colors
    val s = state.snapshot
    val part = dayPartFor(state.now.atZone(state.zone).hour).greeting
    val compact = LifeTheme.layout.widthClass == WidthClass.Compact

    GlassCard(strong = true, contentPadding = Spacing.xl) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AiOrb(size = if (compact) 64.dp else 84.dp, modifier = Modifier.clickable(onClickLabel = "Open LIFE AI", onClick = onOpenAssistant))
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text("$part. Here is your life overview.", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(Spacing.xxs))
                Text(overviewSentence(s), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        val stats = listOf(
            "Today's tasks" to "${s.tasks.completedToday}/${s.tasks.completedToday + s.tasks.dueTodayOpen}",
            "Reminders" to s.reminders.size.toString(),
            "Spent this month" to IndianFormat.rupeesCompact(s.finance.expensePaise),
            "Goal progress" to (s.averageGoalProgress?.let { "${Math.round(it * 100)}%" } ?: "—"),
            "Events · 7 days" to s.upcomingEventsCount.toString(),
            "Productivity" to (s.productivityScore?.let { "$it" } ?: "—"),
        )
        val perRow = if (compact) 3 else 6
        stats.chunked(perRow).forEachIndexed { i, row ->
            if (i > 0) Spacer(Modifier.height(Spacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { (label, value) -> StatBlock(label, value, Modifier.weight(1f)) }
            }
        }
    }
}

/** Rule-based summary built only from real counts. */
internal fun overviewSentence(s: DashboardSnapshot): String {
    val parts = mutableListOf<String>()
    val due = s.tasks.dueTodayOpen
    if (due > 0) parts += "$due task${if (due == 1) "" else "s"} due today"
    if (s.tasks.overdue > 0) parts += "${s.tasks.overdue} overdue"
    if (s.schedule.any { !it.isTask }) {
        val n = s.schedule.count { !it.isTask }
        parts += "$n event${if (n == 1) "" else "s"} on your calendar"
    }
    if (s.finance.expensePaise > 0) parts += "${IndianFormat.rupees(s.finance.expensePaise, showPaise = false)} spent this month"
    return if (parts.isEmpty()) {
        "Nothing is recorded for today yet. As you add tasks, expenses and events, this overview fills in from your own data."
    } else {
        parts.joinToString(", ").replaceFirstChar { it.uppercase() } + "."
    }
}

@Composable
private fun DashboardWidgetCard(type: WidgetType, state: DashboardUiState.Ready, onOpenModule: (String) -> Unit) {
    when (type) {
        WidgetType.TODAY_SCHEDULE -> ScheduleWidget(state, onOpenModule)
        WidgetType.FINANCE_OVERVIEW -> FinanceWidget(state, onOpenModule)
        WidgetType.TASK_COMPLETION -> TaskWidget(state, onOpenModule)
        WidgetType.UPCOMING_REMINDERS -> RemindersWidget(state, onOpenModule)
        WidgetType.ACTIVE_GOALS -> GoalsWidget(state, onOpenModule)
        WidgetType.FAMILY_OVERVIEW -> FamilyWidget(state, onOpenModule)
        WidgetType.QUICK_NOTES -> NotesWidget(state, onOpenModule)
        WidgetType.RECENT_DOCUMENTS -> DocumentsWidget(state, onOpenModule)
    }
}
