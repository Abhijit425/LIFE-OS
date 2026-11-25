package com.lifeos.app.feature.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.ChoiceChips
import com.lifeos.app.core.designsystem.component.DateField
import com.lifeos.app.core.designsystem.component.FieldLabel
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ListRow
import com.lifeos.app.core.designsystem.component.SwitchRow
import com.lifeos.app.core.designsystem.component.TimeField
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.data.local.entity.ProjectEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.domain.model.ProjectStatus
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.TaskPriority
import com.lifeos.app.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun priorityColor(p: TaskPriority): Color {
    val c = LifeTheme.colors
    return when (p) {
        TaskPriority.CRITICAL -> c.danger
        TaskPriority.HIGH -> c.warning
        TaskPriority.MEDIUM -> c.accentCyan
        TaskPriority.LOW -> c.textSecondary
    }
}

@Composable
fun TaskRow(task: TaskEntity, vm: TaskActionsViewModel, projectName: String?, onOpen: () -> Unit) {
    val colors = LifeTheme.colors
    val zone = vm.zone()
    val today = vm.now().atZone(zone).toLocalDate()
    val overdue = task.status != TaskStatus.DONE && task.dueAt?.isBefore(vm.now()) == true
    ListRow(
        title = task.title,
        subtitle = listOfNotNull(
            task.dueAt?.let { (if (overdue) "Overdue · " else "Due ") + TimeFormat.relative(it, today, zone) },
            projectName,
            task.category,
            if (task.recurrence != Recurrence.NONE) "Repeats ${task.recurrence.name.lowercase()}" else null,
        ).joinToString(" · "),
        strikeThrough = task.status == TaskStatus.DONE,
        leading = {
            IconButton(onClick = { vm.setStatus(task, if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE) }) {
                Icon(
                    if (task.status == TaskStatus.DONE) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (task.status == TaskStatus.DONE) "Mark ${task.title} not done" else "Mark ${task.title} done",
                    tint = if (task.status == TaskStatus.DONE) colors.success else priorityColor(task.priority),
                )
            }
        },
        trailing = { LifeChip(task.priority.label, if (overdue) colors.danger else priorityColor(task.priority)) },
        onClick = onOpen,
    )
}

/** Create/edit a task, including its reminder and (for saved tasks) subtasks. */
@Composable
fun TaskSheet(
    item: TaskEntity?,
    vm: TaskActionsViewModel,
    projects: List<ProjectEntity>,
    defaultProjectId: Long?,
    onDismiss: () -> Unit,
) {
    val zone = vm.zone()
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var description by remember { mutableStateOf(item?.description.orEmpty()) }
    var priority by remember { mutableStateOf(item?.priority ?: TaskPriority.MEDIUM) }
    var status by remember { mutableStateOf(item?.status ?: TaskStatus.TODO) }
    var dueDate by remember { mutableStateOf(item?.dueAt?.atZone(zone)?.toLocalDate()) }
    var dueTime by remember { mutableStateOf(item?.dueAt?.atZone(zone)?.toLocalTime()) }
    var category by remember { mutableStateOf(item?.category.orEmpty()) }
    var projectId by remember { mutableStateOf(item?.projectId ?: defaultProjectId) }
    var recurrence by remember { mutableStateOf(item?.recurrence ?: Recurrence.NONE) }
    var remind by remember { mutableStateOf(false) }
    var newSub by remember { mutableStateOf("") }
    LaunchedEffect(item?.id) { if (item != null) remind = vm.reminderFor(item.id) != null }

    val dueAt = dueDate?.atTime(dueTime ?: LocalTime.of(18, 0))?.atZone(zone)?.toInstant()
    LifeSheet(
        title = if (item == null) "New task" else "Edit task",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank(),
        onSave = {
            val now = vm.now()
            val base = item ?: TaskEntity(title = title, createdAt = now, updatedAt = now)
            vm.saveTask(
                base.copy(
                    title = title.trim(), description = description.trim().ifBlank { null }, priority = priority, status = status,
                    dueAt = dueAt, category = category.trim().ifBlank { null }, projectId = projectId, recurrence = recurrence,
                ),
                remindAt = if (remind && dueAt != null && dueAt.isAfter(now)) dueAt else null,
            )
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteTask(it); onDismiss() } },
        deleteMessage = "The task, its subtasks and its reminder will be deleted.",
    ) {
        LifeTextField(title, { title = it.take(120) }, "Title", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(description, { description = it.take(1000) }, "Description (optional)", Modifier.fillMaxWidth(), singleLine = false)
        FieldLabel("Priority")
        ChoiceChips(TaskPriority.entries, priority, { it.label }, { priority = it })
        FieldLabel("Status")
        ChoiceChips(TaskStatus.entries, status, { statusLabel(it) }, { status = it })
        Row {
            DateField("Due date", dueDate, { dueDate = it }, Modifier.weight(1f))
            Spacer(Modifier.size(Spacing.sm))
            TimeField("Time", dueTime, { dueTime = it }, Modifier.weight(1f))
        }
        SwitchRow("Remind me at the due time", remind && dueDate != null, { remind = it }, enabled = dueDate != null,
            description = if (dueDate == null) "Set a due date first" else null)
        FieldLabel("Repeats")
        ChoiceChips(Recurrence.entries, recurrence, { if (it == Recurrence.NONE) "Never" else it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, { recurrence = it })
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(category, { category = it.take(30) }, "Category (e.g. Work, Home)", Modifier.fillMaxWidth())
        if (projects.isNotEmpty()) {
            FieldLabel("Project")
            ChoiceChips(listOf<ProjectEntity?>(null) + projects, projects.firstOrNull { it.id == projectId }, { it?.title ?: "None" }, { projectId = it?.id })
        }
        if (item != null) {
            val subsFlow = remember(item.id) { vm.subtasks(item.id) }
            val subs by subsFlow.collectAsStateWithLifecycle(emptyList())
            FieldLabel("Subtasks (${subs.count { it.isDone }}/${subs.size})")
            subs.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(s.isDone, { vm.toggleSubtask(s) }, colors = CheckboxDefaults.colors(checkedColor = LifeTheme.colors.accentPurple))
                    Text(s.title, style = MaterialTheme.typography.bodyLarge, color = LifeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteSubtask(s) }) { Icon(Icons.Outlined.Close, "Remove subtask ${s.title}", tint = LifeTheme.colors.textTertiary) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LifeTextField(newSub, { newSub = it.take(80) }, "Add a subtask", Modifier.weight(1f))
                IconButton(onClick = { vm.addSubtask(item.id, newSub, subs.size); newSub = "" }, enabled = newSub.isNotBlank()) {
                    Icon(Icons.Outlined.Add, "Add subtask", tint = LifeTheme.colors.accentCyan)
                }
            }
        } else {
            Text("Save the task to add subtasks.", style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textTertiary)
        }
    }
}

fun statusLabel(s: TaskStatus) = when (s) {
    TaskStatus.TODO -> "To Do"
    TaskStatus.IN_PROGRESS -> "In Progress"
    TaskStatus.DONE -> "Completed"
}

@Composable
fun ProjectSheet(item: ProjectEntity?, now: () -> java.time.Instant, onSave: (ProjectEntity) -> Unit, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null) {
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var description by remember { mutableStateOf(item?.description.orEmpty()) }
    var start by remember { mutableStateOf(item?.startDate) }
    var deadline by remember { mutableStateOf(item?.deadline) }
    var status by remember { mutableStateOf(item?.status ?: ProjectStatus.ACTIVE) }
    val s0 = start
    val d0 = deadline
    val valid = title.isNotBlank() && (s0 == null || d0 == null || !d0.isBefore(s0))
    LifeSheet(
        if (item == null) "New project" else "Edit project", onDismiss,
        saveEnabled = valid,
        onSave = {
            onSave((item ?: ProjectEntity(title = title, createdAt = now())).copy(
                title = title.trim(), description = description.trim().ifBlank { null }, startDate = start, deadline = deadline, status = status))
            onDismiss()
        },
        onDelete = onDelete,
        deleteMessage = "The project and its milestones will be deleted. Its tasks are kept.",
    ) {
        LifeTextField(title, { title = it.take(80) }, "Project title", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(description, { description = it.take(1000) }, "Description", Modifier.fillMaxWidth(), singleLine = false)
        Row {
            DateField("Start", start, { start = it }, Modifier.weight(1f))
            Spacer(Modifier.size(Spacing.sm))
            DateField("Deadline", deadline, { deadline = it }, Modifier.weight(1f))
        }
        if (!valid && title.isNotBlank()) Text("Deadline must be after the start date.", color = LifeTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
        FieldLabel("Status")
        ChoiceChips(ProjectStatus.entries, status, { it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() } }, { status = it })
    }
}

