package com.lifeos.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.dao.ProjectProgress
import com.lifeos.app.data.local.entity.MilestoneEntity
import com.lifeos.app.data.local.entity.ProjectEntity
import com.lifeos.app.data.local.entity.SubTaskEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.domain.model.TaskStatus
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.navigation.ProjectRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Shared task actions used by the task list, board and project screens. */
abstract class TaskActionsViewModel(protected val repo: TaskRepository, private val time: TimeProvider) : LifeViewModel() {
    fun now(): Instant = time.now()
    fun zone(): ZoneId = time.zone()
    fun subtasks(taskId: Long): Flow<List<SubTaskEntity>> = repo.observeSubtasks(taskId)
    suspend fun reminderFor(taskId: Long): Instant? = repo.reminderFor(taskId)

    fun saveTask(t: TaskEntity, remindAt: Instant?) = act(if (t.id == 0L) "Task added." else "Task saved.") { repo.saveTask(t, remindAt) }
    fun setStatus(t: TaskEntity, s: TaskStatus) = act(if (s == TaskStatus.DONE) "Completed “${t.title}”." else null) { repo.setStatus(t, s) }
    fun deleteTask(t: TaskEntity) = act("Task deleted.") { repo.deleteTask(t) }
    fun addSubtask(taskId: Long, title: String, position: Int) = act { repo.addSubtask(taskId, title, position) }
    fun toggleSubtask(s: SubTaskEntity) = act { repo.updateSubtask(s.copy(isDone = !s.isDone)) }
    fun deleteSubtask(s: SubTaskEntity) = act { repo.deleteSubtask(s) }
}

@HiltViewModel
class TasksViewModel @Inject constructor(repo: TaskRepository, time: TimeProvider) : TaskActionsViewModel(repo, time) {
    val tasks: StateFlow<List<TaskEntity>> = repo.observeTasks().stateWhileSubscribed(emptyList())
    val projects: StateFlow<List<ProjectEntity>> = repo.observeProjects().stateWhileSubscribed(emptyList())
    val progress: StateFlow<List<ProjectProgress>> = repo.observeProjectProgress().stateWhileSubscribed(emptyList())

    fun saveProject(p: ProjectEntity) = act("Project saved.") { repo.saveProject(p) }
}

@HiltViewModel
class ProjectViewModel @Inject constructor(
    savedState: SavedStateHandle,
    repo: TaskRepository,
    time: TimeProvider,
) : TaskActionsViewModel(repo, time) {
    val projectId: Long = savedState.toRoute<ProjectRoute>().id
    val project: StateFlow<ProjectEntity?> = repo.observeProject(projectId).stateWhileSubscribed(null)
    val tasks: StateFlow<List<TaskEntity>> = repo.observeProjectTasks(projectId).stateWhileSubscribed(emptyList())
    val milestones: StateFlow<List<MilestoneEntity>> = repo.observeMilestones(projectId).stateWhileSubscribed(emptyList())
    val projects: StateFlow<List<ProjectEntity>> = repo.observeProjects().stateWhileSubscribed(emptyList())

    fun saveProject(p: ProjectEntity) = act("Project saved.") { repo.saveProject(p) }
    fun deleteProject(p: ProjectEntity, onDone: () -> Unit) = act("Project deleted. Its tasks were kept.") {
        repo.deleteProject(p)
        onDone()
    }
    fun addMilestone(title: String, due: java.time.LocalDate?) = act { if (title.isNotBlank()) repo.saveMilestone(MilestoneEntity(projectId = projectId, title = title.trim(), dueDate = due)) }
    fun toggleMilestone(m: MilestoneEntity) = act { repo.saveMilestone(m.copy(isDone = !m.isDone)) }
    fun deleteMilestone(m: MilestoneEntity) = act("Milestone removed.") { repo.deleteMilestone(m) }
}
