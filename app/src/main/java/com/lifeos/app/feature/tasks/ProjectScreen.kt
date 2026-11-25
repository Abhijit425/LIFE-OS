package com.lifeos.app.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.DateField
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSecondaryButton
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.LoadingState
import com.lifeos.app.core.designsystem.component.ProgressRing
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.domain.model.TaskStatus
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.screenContentPadding
import java.time.LocalDate

@Composable
fun ProjectScreen(onBack: () -> Unit, vm: ProjectViewModel = hiltViewModel()) {
    val project by vm.project.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val milestones by vm.milestones.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    var editProject by remember { mutableStateOf(false) }
    var taskEdit by remember { mutableStateOf<Pair<Boolean, TaskEntity?>?>(null) }
    var msTitle by remember { mutableStateOf("") }
    var msDate by remember { mutableStateOf<LocalDate?>(null) }
    val colors = LifeTheme.colors

    MessageBox(vm) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = screenContentPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            val p = project
            if (p == null) {
                item { GlassCard { LoadingState() } }
                return@LazyColumn
            }
            item {
                ScreenHeader(p.title, p.description, onBack = onBack) {
                    IconButton(onClick = { editProject = true }) { Icon(Icons.Outlined.Edit, "Edit project", tint = colors.textPrimary) }
                }
            }
            item {
                val done = tasks.count { it.status == TaskStatus.DONE }
                val fraction = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
                GlassCard(strong = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(fraction, label = "${Math.round(fraction * 100)}%", subLabel = "progress")
                        Spacer(Modifier.width(Spacing.lg))
                        androidx.compose.foundation.layout.Column {
                            Text("$done of ${tasks.size} tasks done", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            p.startDate?.let { Text("Started ${TimeFormat.shortDate(it)} ${it.year}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
                            p.deadline?.let { Text("Deadline ${TimeFormat.shortDate(it)} ${it.year}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
                            Text("Milestones: ${milestones.count { it.isDone }}/${milestones.size}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
            item {
                GlassCard {
                    CardHeader(Icons.Outlined.Flag, "Milestones")
                    milestones.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(m.isDone, { vm.toggleMilestone(m) }, colors = CheckboxDefaults.colors(checkedColor = colors.accentPurple))
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                Text(m.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                                m.dueDate?.let { Text("Due ${TimeFormat.shortDate(it)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
                            }
                            IconButton(onClick = { vm.deleteMilestone(m) }) { Icon(Icons.Outlined.Close, "Remove ${m.title}", tint = colors.textTertiary) }
                        }
                    }
                    LifeTextField(msTitle, { msTitle = it.take(80) }, "New milestone", Modifier.fillMaxWidth())
                    DateField("Milestone date", msDate, { msDate = it })
                    Spacer(Modifier.height(Spacing.sm))
                    LifeSecondaryButton("Add milestone", onClick = { vm.addMilestone(msTitle, msDate); msTitle = ""; msDate = null }, enabled = msTitle.isNotBlank())
                }
            }
            item {
                GlassCard {
                    CardHeader(Icons.Outlined.CheckCircle, "Tasks") {
                        LifePrimaryButton("Task", onClick = { taskEdit = true to null }, icon = Icons.Outlined.Add)
                    }
                    if (tasks.isEmpty()) EmptyState(Icons.Outlined.CheckCircle, "No tasks in this project", "Add tasks to track progress.")
                    tasks.forEach { t -> TaskRow(t, vm, null) { taskEdit = true to t } }
                }
            }
        }
    }

    project?.let { p ->
        if (editProject) ProjectSheet(p, vm::now, onSave = vm::saveProject, onDismiss = { editProject = false }, onDelete = { vm.deleteProject(p, onBack); editProject = false })
        taskEdit?.let { (_, t) -> TaskSheet(t, vm, projects, p.id, onDismiss = { taskEdit = null }) }
    }
}

