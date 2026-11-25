package com.lifeos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeos.app.domain.model.ProjectStatus
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.TaskPriority
import com.lifeos.app.domain.model.TaskStatus
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "projects", indices = [Index("status")])
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val startDate: LocalDate? = null,
    val deadline: LocalDate? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val createdAt: Instant,
)

@Entity(
    tableName = "milestones",
    foreignKeys = [ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val dueDate: LocalDate? = null,
    val isDone: Boolean = false,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = FamilyMemberEntity::class, parentColumns = ["id"], childColumns = ["familyMemberId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("status"), Index("dueAt"), Index("projectId"), Index("familyMemberId"), Index("completedAt")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val dueAt: Instant? = null,
    val category: String? = null,
    val projectId: Long? = null,
    val familyMemberId: Long? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("taskId")],
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false,
    val position: Int = 0,
)
