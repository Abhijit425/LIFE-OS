package com.lifeos.app.data.repository

import androidx.room.withTransaction
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.dao.ProjectProgress
import com.lifeos.app.data.local.entity.MilestoneEntity
import com.lifeos.app.data.local.entity.NoteEntity
import com.lifeos.app.data.local.entity.NoteTagEntity
import com.lifeos.app.data.local.entity.ProjectEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.data.local.entity.SubTaskEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.domain.logic.RecurrenceMath
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.ReminderType
import com.lifeos.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val db: LifeOsDatabase,
    private val reminders: ReminderEngine,
    private val time: TimeProvider,
) {
    private val dao get() = db.taskDao()

    fun observeTasks(): Flow<List<TaskEntity>> = dao.observeAll()
    fun observeProjectTasks(projectId: Long): Flow<List<TaskEntity>> = dao.observeByProject(projectId)
    fun observeProjects(): Flow<List<ProjectEntity>> = db.projectDao().observeAll()
    fun observeProject(id: Long): Flow<ProjectEntity?> = db.projectDao().observeById(id)
    fun observeProjectProgress(): Flow<List<ProjectProgress>> = dao.observeProjectProgress()
    fun observeMilestones(projectId: Long): Flow<List<MilestoneEntity>> = db.milestoneDao().observeForProject(projectId)
    fun observeSubtasks(taskId: Long): Flow<List<SubTaskEntity>> = db.subTaskDao().observeForTask(taskId)

    suspend fun reminderFor(taskId: Long): Instant? =
        db.reminderDao().getForTask(taskId).firstOrNull { !it.isCompleted }?.triggerAt

    /** Saves the task and keeps its (optional) reminder in sync. Returns the task id. */
    suspend fun saveTask(task: TaskEntity, remindAt: Instant?): Long {
        val now = time.now()
        val t = task.copy(
            title = task.title.trim(),
            updatedAt = now,
            completedAt = if (task.status == TaskStatus.DONE) task.completedAt ?: now else null,
        )
        val id = if (t.id == 0L) dao.insert(t) else t.id.also { dao.update(t) }
        val existing = db.reminderDao().getForTask(id)
        existing.forEach { reminders.delete(it) }
        if (remindAt != null && t.status != TaskStatus.DONE) {
            reminders.save(
                ReminderEntity(title = t.title, message = "Task reminder", type = ReminderType.TASK, triggerAt = remindAt, taskId = id, createdAt = now),
            )
        }
        return id
    }

    /**
     * Moves a task between Kanban columns. Completing a recurring task creates its next
     * occurrence so the series continues.
     */
    suspend fun setStatus(task: TaskEntity, status: TaskStatus) {
        val now = time.now()
        db.withTransaction {
            dao.update(task.copy(status = status, completedAt = if (status == TaskStatus.DONE) now else null, updatedAt = now))
            if (status == TaskStatus.DONE && task.status != TaskStatus.DONE && task.recurrence != Recurrence.NONE) {
                val base = task.dueAt ?: now
                val next = RecurrenceMath.nextAfter(base, task.recurrence, now, time.zone())
                    ?: RecurrenceMath.next(base, task.recurrence, time.zone())
                dao.insert(task.copy(id = 0, status = TaskStatus.TODO, dueAt = next, completedAt = null, createdAt = now, updatedAt = now))
            }
        }
        if (status == TaskStatus.DONE) {
            db.reminderDao().getForTask(task.id).forEach { reminders.setCompleted(it, true) }
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        reminders.cancelReminderAlarms(db.reminderDao().getForTask(task.id))
        dao.delete(task) // subtasks and reminders cascade
    }

    suspend fun getOpen(limit: Int) = dao.getOpen(limit)
    suspend fun findOpen(query: String) = dao.findOpenByTitle(query)

    suspend fun addSubtask(taskId: Long, title: String, position: Int) {
        if (title.isNotBlank()) db.subTaskDao().insert(SubTaskEntity(taskId = taskId, title = title.trim(), position = position))
    }
    suspend fun updateSubtask(s: SubTaskEntity) = db.subTaskDao().update(s)
    suspend fun deleteSubtask(s: SubTaskEntity) = db.subTaskDao().delete(s)

    suspend fun saveProject(p: ProjectEntity): Long =
        if (p.id == 0L) db.projectDao().insert(p) else p.id.also { db.projectDao().update(p) }

    /** Tasks in a deleted project are kept (unlinked); milestones go with it. */
    suspend fun deleteProject(p: ProjectEntity) = db.projectDao().delete(p)

    suspend fun saveMilestone(m: MilestoneEntity) {
        if (m.id == 0L) db.milestoneDao().insert(m) else db.milestoneDao().update(m)
    }
    suspend fun deleteMilestone(m: MilestoneEntity) = db.milestoneDao().delete(m)
}

@Singleton
class NoteRepository @Inject constructor(
    private val db: LifeOsDatabase,
    private val time: TimeProvider,
) {
    fun observe(archived: Boolean): Flow<List<NoteEntity>> = db.noteDao().observe(archived)
    fun observeTags(): Flow<List<NoteTagEntity>> = db.noteTagDao().observeAll()

    suspend fun get(id: Long): NoteEntity? = db.noteDao().getById(id)
    suspend fun tagsFor(id: Long): List<String> = db.noteTagDao().getForNote(id).map { it.tag }

    suspend fun save(note: NoteEntity, tags: List<String>): Long = db.withTransaction {
        val now = time.now()
        val n = note.copy(updatedAt = now)
        val id = if (n.id == 0L) db.noteDao().insert(n.copy(createdAt = now)) else n.id.also { db.noteDao().update(n) }
        db.noteTagDao().deleteForNote(id)
        val clean = tags.map { it.trim().lowercase().removePrefix("#") }.filter { it.isNotEmpty() }.distinct().take(20)
        db.noteTagDao().insertAll(clean.map { NoteTagEntity(id, it) })
        id
    }

    suspend fun quickAdd(text: String): Long {
        val now = time.now()
        val title = text.lineSequence().first().take(60)
        return db.noteDao().insert(NoteEntity(title = title, content = text, createdAt = now, updatedAt = now))
    }

    suspend fun setPinned(note: NoteEntity, pinned: Boolean) = db.noteDao().update(note.copy(isPinned = pinned, updatedAt = time.now()))
    suspend fun setArchived(note: NoteEntity, archived: Boolean) = db.noteDao().update(note.copy(isArchived = archived, isPinned = false, updatedAt = time.now()))
    suspend fun delete(note: NoteEntity) = db.noteDao().delete(note)
}
