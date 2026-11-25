package com.lifeos.app.feature.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.ChoiceChips
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.GlowProgressBar
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.ListRow
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SearchField
import com.lifeos.app.core.designsystem.component.SegmentedTabs
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.designsystem.theme.WidthClass
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.data.local.entity.ProjectEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.domain.model.TaskPriority
import com.lifeos.app.domain.model.TaskStatus
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.screenContentPadding

private enum class TasksTab(val label: String) { LIST("List"), BOARD("Board"), PROJECTS("Projects") }
private enum class StatusFilter(val label: String) { OPEN("Open"), ALL("All"), DONE("Completed") }

@Composable
fun TasksScreen(onOpenProject: (Long) -> Unit, vm: TasksViewModel = hiltViewModel()) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TasksTab.LIST) }
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(StatusFilter.OPEN) }
    var priorityFilter by rememberSaveable { mutableStateOf<TaskPriority?>(null) }
    var editing by remember { mutableStateOf<Pair<Boolean, TaskEntity?>?>(null) }
    var newProject by remember { mutableStateOf(false) }
    val projectNames = projects.associate { it.id to it.title }
    val colors = LifeTheme.colors

    MessageBox(vm) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = screenContentPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item {
                val open = tasks.count { it.status != TaskStatus.DONE }
                ScreenHeader("Tasks", "$open open · ${tasks.size - open} completed") {
                    if (tab == TasksTab.PROJECTS) LifePrimaryButton("Project", onClick = { newProject = true }, icon = Icons.Outlined.Add)
                    else LifePrimaryButton("Task", onClick = { editing = true to null }, icon = Icons.Outlined.Add)
                }
                SegmentedTabs(TasksTab.entries, tab, { it.label }, { tab = it })
            }
            when (tab) {
                TasksTab.LIST -> {
                    item {
                        SearchField(query, { query = it }, "Search tasks")
                        Spacer(Modifier.height(Spacing.xs))
                        ChoiceChips(StatusFilter.entries, statusFilter, { it.label }, { statusFilter = it })
                        Spacer(Modifier.height(Spacing.xs))
                        ChoiceChips(listOf<TaskPriority?>(null) + TaskPriority.entries, priorityFilter, { it?.label ?: "Any priority" }, { priorityFilter = it })
                    }
                    val filtered = tasks.filter { t ->
                        when (statusFilter) {
                            StatusFilter.OPEN -> t.status != TaskStatus.DONE
                            StatusFilter.DONE -> t.status == TaskStatus.DONE
                            StatusFilter.ALL -> true
                        } && (priorityFilter == null || t.priority == priorityFilter) &&
                            (query.isBlank() || t.title.contains(query, true) || t.description.orEmpty().contains(query, true) || t.category.orEmpty().contains(query, true))
                    }.sortedWith(compareBy<TaskEntity> { it.status == TaskStatus.DONE }.thenByDescending { it.priority.weight }.thenBy { it.dueAt ?: java.time.Instant.MAX })
                    item {
                        GlassCard {
                            if (filtered.isEmpty()) {
                                EmptyState(Icons.Outlined.CheckCircle, if (tasks.isEmpty()) "No tasks yet" else "Nothing matches",
                                    if (tasks.isEmpty()) "Tap Task to add your first one." else "Try a different filter.")
                            }
                            filtered.forEach { t -> TaskRow(t, vm, t.projectId?.let { projectNames[it] }) { editing = true to t } }
                        }
                    }
                }
                TasksTab.BOARD -> item { KanbanBoard(tasks, vm) { editing = true to it } }
                TasksTab.PROJECTS -> {
                    if (projects.isEmpty()) item {
                        GlassCard { EmptyState(Icons.Outlined.Folder, "No projects", "Group tasks into projects with milestones and deadlines.") }
                    }
                    items(projects, key = { it.id }) { p ->
                        val pr = progress.firstOrNull { it.projectId == p.id }
                        val fraction = if (pr == null || pr.total == 0) 0f else pr.done.toFloat() / pr.total
                        GlassCard(onClick = { onOpenProject(p.id) }, onClickLabel = "Open ${p.title}") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                LifeChip(p.status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, colors.accentPurple)
                            }
                            Text(
                                listOfNotNull(
                                    pr?.let { "${it.done}/${it.total} tasks done" } ?: "No tasks yet",
                                    p.deadline?.let { "Deadline ${TimeFormat.shortDate(it)} ${it.year}" },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            GlowProgressBar(fraction)
                        }
                    }
                }
            }
        }
    }

    editing?.let { (_, task) -> TaskSheet(task, vm, projects, null, onDismiss = { editing = null }) }
    if (newProject) ProjectSheet(null, vm::now, onSave = vm::saveProject, onDismiss = { newProject = false })
}

/** Kanban: three columns side by side on tablets, horizontally scrollable on phones. Arrow buttons move cards (accessible). */
@Composable
private fun KanbanBoard(tasks: List<TaskEntity>, vm: TaskActionsViewModel, onOpen: (TaskEntity) -> Unit) {
    val wide = LifeTheme.layout.widthClass != WidthClass.Compact
    val columns = TaskStatus.entries
    val rowModifier = if (wide) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())
    Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        columns.forEach { status ->
            val colTasks = tasks.filter { it.status == status }.sortedByDescending { it.priority.weight }
            GlassCard(modifier = if (wide) Modifier.weight(1f) else Modifier.width(280.dp)) {
                Text("${statusLabel(status)} · ${colTasks.size}", style = MaterialTheme.typography.titleMedium, color = LifeTheme.colors.textPrimary)
                Spacer(Modifier.height(Spacing.sm))
                if (colTasks.isEmpty()) Text("Empty", style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textTertiary)
                colTasks.take(50).forEach { t -> KanbanCard(t, vm, onOpen) }
            }
        }
    }
}

@Composable
private fun KanbanCard(t: TaskEntity, vm: TaskActionsViewModel, onOpen: (TaskEntity) -> Unit) {
    val colors = LifeTheme.colors
    val idx = TaskStatus.entries.indexOf(t.status)
    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), strong = true, contentPadding = Spacing.sm, onClick = { onOpen(t) }, onClickLabel = "Edit ${t.title}") {
        Text(t.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LifeChip(t.priority.label, priorityColor(t.priority))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.setStatus(t, TaskStatus.entries[idx - 1]) }, enabled = idx > 0) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Move ${t.title} left", tint = if (idx > 0) colors.textSecondary else colors.textTertiary)
            }
            IconButton(onClick = { vm.setStatus(t, TaskStatus.entries[idx + 1]) }, enabled = idx < TaskStatus.entries.lastIndex) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Move ${t.title} right", tint = if (idx < TaskStatus.entries.lastIndex) colors.textSecondary else colors.textTertiary)
            }
        }
    }
}

