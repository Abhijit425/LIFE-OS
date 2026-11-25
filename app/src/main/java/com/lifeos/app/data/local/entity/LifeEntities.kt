package com.lifeos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeos.app.domain.model.DocumentCategory
import com.lifeos.app.domain.model.DocumentStorage
import com.lifeos.app.domain.model.GoalType
import com.lifeos.app.domain.model.HabitFrequency
import com.lifeos.app.domain.model.HealthMetric
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.ReminderType
import java.time.Instant
import java.time.LocalDate

// ---------- Calendar & reminders ----------

@Entity(
    tableName = "calendar_events",
    foreignKeys = [ForeignKey(entity = FamilyMemberEntity::class, parentColumns = ["id"], childColumns = ["familyMemberId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("startAt"), Index("familyMemberId")],
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val startAt: Instant,
    val endAt: Instant,
    val allDay: Boolean = false,
    val location: String? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val familyMemberId: Long? = null,
    val createdAt: Instant,
)

@Entity(
    tableName = "document_folders",
    foreignKeys = [ForeignKey(entity = DocumentFolderEntity::class, parentColumns = ["id"], childColumns = ["parentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("parentId")],
)
data class DocumentFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val createdAt: Instant,
)

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(entity = DocumentFolderEntity::class, parentColumns = ["id"], childColumns = ["folderId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = FamilyMemberEntity::class, parentColumns = ["id"], childColumns = ["familyMemberId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("folderId"), Index("familyMemberId"), Index("category"), Index("addedAt"), Index("displayName")],
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    /** content:// SAF URI for REFERENCE, or app-private relative path for copied files. */
    val location: String,
    val storage: DocumentStorage,
    val mimeType: String?,
    val sizeBytes: Long?,
    val category: DocumentCategory = DocumentCategory.OTHER,
    val description: String? = null,
    /** Comma-separated, lower-case tags. */
    val tags: String = "",
    val folderId: Long? = null,
    val familyMemberId: Long? = null,
    val expiresOn: LocalDate? = null,
    val addedAt: Instant,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CalendarEventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DocumentEntity::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FamilyMemberEntity::class, parentColumns = ["id"], childColumns = ["familyMemberId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("triggerAt"), Index("taskId"), Index("eventId"), Index("documentId"), Index("familyMemberId")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String? = null,
    val type: ReminderType = ReminderType.GENERAL,
    val triggerAt: Instant,
    val recurrence: Recurrence = Recurrence.NONE,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val taskId: Long? = null,
    val eventId: Long? = null,
    val documentId: Long? = null,
    val familyMemberId: Long? = null,
    val createdAt: Instant,
)

// ---------- Health (personal organisation only — never diagnosis or dosing advice) ----------

@Entity(
    tableName = "health_records",
    foreignKeys = [ForeignKey(entity = FamilyMemberEntity::class, parentColumns = ["id"], childColumns = ["familyMemberId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["metric", "recordedAt"]), Index("familyMemberId")],
)
data class HealthRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metric: HealthMetric,
    val value: Double,
    val note: String? = null,
    val recordedAt: Instant,
    /** null = the user themself. */
    val familyMemberId: Long? = null,
)

@Entity(
    tableName = "medication_reminders",
    foreignKeys = [ForeignKey(entity = FamilyMemberEntity::class, parentColumns = ["id"], childColumns = ["familyMemberId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("familyMemberId")],
)
data class MedicationReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineName: String,
    /** Exactly as the user typed it from their prescription. The app never alters doses. */
    val doseLabel: String,
    /** Comma-separated 24h local times, e.g. "08:00,20:00". */
    val times: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
    val notes: String? = null,
    val familyMemberId: Long? = null,
)

@Entity(
    tableName = "appointments",
    foreignKeys = [ForeignKey(entity = FamilyMemberEntity::class, parentColumns = ["id"], childColumns = ["familyMemberId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("at"), Index("familyMemberId")],
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val withWhom: String? = null,
    val location: String? = null,
    val at: Instant,
    val notes: String? = null,
    val familyMemberId: Long? = null,
)

// ---------- Notes ----------

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("taskId"), Index("projectId"), Index(value = ["isArchived", "isPinned", "updatedAt"])],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Rich text stored as lightweight Markdown; checklists as "- [ ] item" lines. */
    val content: String,
    val isChecklist: Boolean = false,
    val category: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val taskId: Long? = null,
    val projectId: Long? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tag"],
    foreignKeys = [ForeignKey(entity = NoteEntity::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tag")],
)
data class NoteTagEntity(
    val noteId: Long,
    val tag: String,
)

// ---------- Goals & habits ----------

@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(entity = SavingsGoalEntity::class, parentColumns = ["id"], childColumns = ["savingsGoalId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("savingsGoalId"), Index("isCompleted")],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: GoalType,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    /** e.g. "books", "₹", "km". */
    val unit: String,
    val targetDate: LocalDate? = null,
    val savingsGoalId: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Instant,
)

@Entity(
    tableName = "habits",
    foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("goalId")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val goalId: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: Instant,
)

@Entity(
    tableName = "habit_entries",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class HabitEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
)
