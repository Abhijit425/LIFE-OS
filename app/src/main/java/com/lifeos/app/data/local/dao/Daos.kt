package com.lifeos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.lifeos.app.data.local.entity.AccountEntity
import com.lifeos.app.data.local.entity.AiConversationEntity
import com.lifeos.app.data.local.entity.AiMessageEntity
import com.lifeos.app.data.local.entity.AppointmentEntity
import com.lifeos.app.data.local.entity.BudgetEntity
import com.lifeos.app.data.local.entity.CalendarEventEntity
import com.lifeos.app.data.local.entity.DashboardWidgetEntity
import com.lifeos.app.data.local.entity.DocumentEntity
import com.lifeos.app.data.local.entity.DocumentFolderEntity
import com.lifeos.app.data.local.entity.FamilyMemberEntity
import com.lifeos.app.data.local.entity.GoalEntity
import com.lifeos.app.data.local.entity.HabitEntity
import com.lifeos.app.data.local.entity.HabitEntryEntity
import com.lifeos.app.data.local.entity.HealthRecordEntity
import com.lifeos.app.data.local.entity.MedicationReminderEntity
import com.lifeos.app.data.local.entity.MilestoneEntity
import com.lifeos.app.data.local.entity.NoteEntity
import com.lifeos.app.data.local.entity.NoteTagEntity
import com.lifeos.app.data.local.entity.ProjectEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.data.local.entity.SavingsGoalEntity
import com.lifeos.app.data.local.entity.SubTaskEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.data.local.entity.TransactionEntity
import com.lifeos.app.data.local.entity.UserProfileEntity
import com.lifeos.app.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/* Enum columns are stored by name, so queries compare against literal names such as 'DONE'. */

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}

/** Abstract class (not interface) so the @Transaction method has a concrete body Room can wrap. */
@Dao
abstract class DashboardWidgetDao {
    @Query("SELECT * FROM dashboard_widgets ORDER BY position ASC")
    abstract fun observeAll(): Flow<List<DashboardWidgetEntity>>

    @Query("SELECT * FROM dashboard_widgets ORDER BY position ASC")
    abstract suspend fun getAll(): List<DashboardWidgetEntity>

    @Upsert
    abstract suspend fun upsertAll(widgets: List<DashboardWidgetEntity>)

    @Query("DELETE FROM dashboard_widgets")
    abstract suspend fun clear()

    @Transaction
    open suspend fun replaceAll(widgets: List<DashboardWidgetEntity>) {
        clear()
        upsertAll(widgets)
    }
}

// ---------------------------------------------------------------- Tasks & projects

data class ProjectProgress(val projectId: Long, val total: Int, val done: Int)

@Dao
interface TaskDao {
    @Insert suspend fun insert(task: TaskEntity): Long
    @Update suspend fun update(task: TaskEntity)
    @Delete suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY CASE status WHEN 'DONE' THEN 1 ELSE 0 END, dueAt IS NULL, dueAt ASC, updatedAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY CASE status WHEN 'DONE' THEN 1 ELSE 0 END, dueAt IS NULL, dueAt ASC")
    fun observeByProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY dueAt IS NULL, dueAt ASC LIMIT :limit")
    suspend fun getOpen(limit: Int): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status != 'DONE' AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun findOpenByTitle(query: String): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status != 'DONE' AND dueAt >= :start AND dueAt < :end ORDER BY dueAt ASC")
    fun observeOpenDueBetween(start: Instant, end: Instant): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'DONE' AND completedAt >= :start AND completedAt < :end")
    fun observeCompletedBetween(start: Instant, end: Instant): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'DONE' AND completedAt >= :start AND completedAt < :end")
    suspend fun countCompletedBetween(start: Instant, end: Instant): Int

    @Query("SELECT completedAt FROM tasks WHERE status = 'DONE' AND completedAt >= :start")
    fun observeCompletionTimesSince(start: Instant): Flow<List<Instant?>>

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'DONE' AND dueAt IS NOT NULL AND dueAt < :before")
    fun observeOverdueCount(before: Instant): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'DONE'")
    fun observeOpenCount(): Flow<Int>

    @Query(
        """SELECT projectId, COUNT(*) AS total, SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) AS done
           FROM tasks WHERE projectId IS NOT NULL GROUP BY projectId"""
    )
    fun observeProjectProgress(): Flow<List<ProjectProgress>>
}

@Dao
interface SubTaskDao {
    @Insert suspend fun insert(subTask: SubTaskEntity): Long
    @Update suspend fun update(subTask: SubTaskEntity)
    @Delete suspend fun delete(subTask: SubTaskEntity)

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    fun observeForTask(taskId: Long): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM subtasks")
    suspend fun getAll(): List<SubTaskEntity>
}

@Dao
interface ProjectDao {
    @Insert suspend fun insert(project: ProjectEntity): Long
    @Update suspend fun update(project: ProjectEntity)
    @Delete suspend fun delete(project: ProjectEntity)

    @Query("SELECT * FROM projects ORDER BY CASE status WHEN 'COMPLETED' THEN 1 ELSE 0 END, deadline IS NULL, deadline ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>
}

@Dao
interface MilestoneDao {
    @Insert suspend fun insert(milestone: MilestoneEntity): Long
    @Update suspend fun update(milestone: MilestoneEntity)
    @Delete suspend fun delete(milestone: MilestoneEntity)

    @Query("SELECT * FROM milestones WHERE projectId = :projectId ORDER BY dueDate IS NULL, dueDate ASC, id ASC")
    fun observeForProject(projectId: Long): Flow<List<MilestoneEntity>>
}

// ---------------------------------------------------------------- Finance

/** Amount + timestamp projection for chart bucketing. */
data class AmountAt(val amountPaise: Long, val occurredAt: Instant)

data class CategoryTotal(val category: TransactionCategory, val totalPaise: Long)

data class AccountFlow(val accountId: Long, val incomePaise: Long, val expensePaise: Long)

@Dao
interface TransactionDao {
    @Insert suspend fun insert(transaction: TransactionEntity): Long
    @Update suspend fun update(transaction: TransactionEntity)
    @Delete suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE occurredAt >= :start AND occurredAt < :end ORDER BY occurredAt DESC")
    fun observeBetween(start: Instant, end: Instant): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE recurrence != 'NONE'")
    suspend fun getRecurringHeads(): List<TransactionEntity>

    /** [type] is a TransactionType name. */
    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE type = :type AND occurredAt >= :start AND occurredAt < :end")
    fun observeTotal(type: String, start: Instant, end: Instant): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE type = :type AND occurredAt >= :start AND occurredAt < :end")
    suspend fun total(type: String, start: Instant, end: Instant): Long

    @Query("SELECT amountPaise, occurredAt FROM transactions WHERE type = :type AND occurredAt >= :start ORDER BY occurredAt ASC")
    fun observeAmountsSince(type: String, start: Instant): Flow<List<AmountAt>>

    @Query(
        """SELECT category, SUM(amountPaise) AS totalPaise FROM transactions
           WHERE type = :type AND occurredAt >= :start AND occurredAt < :end
           GROUP BY category ORDER BY totalPaise DESC"""
    )
    fun observeCategoryTotals(type: String, start: Instant, end: Instant): Flow<List<CategoryTotal>>

    @Query(
        """SELECT category, SUM(amountPaise) AS totalPaise FROM transactions
           WHERE type = :type AND occurredAt >= :start AND occurredAt < :end
           GROUP BY category ORDER BY totalPaise DESC"""
    )
    suspend fun categoryTotals(type: String, start: Instant, end: Instant): List<CategoryTotal>

    @Query(
        """SELECT accountId,
             SUM(CASE WHEN type = 'INCOME' THEN amountPaise ELSE 0 END) AS incomePaise,
             SUM(CASE WHEN type = 'EXPENSE' THEN amountPaise ELSE 0 END) AS expensePaise
           FROM transactions WHERE accountId IS NOT NULL GROUP BY accountId"""
    )
    fun observeAccountFlows(): Flow<List<AccountFlow>>
}

@Dao
interface AccountDao {
    @Insert suspend fun insert(account: AccountEntity): Long
    @Update suspend fun update(account: AccountEntity)
    @Delete suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY type, name COLLATE NOCASE")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>
}

@Dao
interface BudgetDao {
    /** REPLACE so a second budget for the same category+month overwrites the first (unique index). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Delete suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey ORDER BY category")
    fun observeForMonth(monthKey: Int): Flow<List<BudgetEntity>>

    @Query("SELECT COALESCE(SUM(limitPaise), 0) FROM budgets WHERE monthKey = :monthKey")
    fun observeTotalLimit(monthKey: Int): Flow<Long>

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>
}

@Dao
interface SavingsGoalDao {
    @Insert suspend fun insert(goal: SavingsGoalEntity): Long
    @Update suspend fun update(goal: SavingsGoalEntity)
    @Delete suspend fun delete(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goals ORDER BY targetDate IS NULL, targetDate ASC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals")
    suspend fun getAll(): List<SavingsGoalEntity>
}

// ---------------------------------------------------------------- Calendar & reminders

@Dao
interface CalendarEventDao {
    @Insert suspend fun insert(event: CalendarEventEntity): Long
    @Update suspend fun update(event: CalendarEventEntity)
    @Delete suspend fun delete(event: CalendarEventEntity)

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getById(id: Long): CalendarEventEntity?

    /** Events overlapping [start, end). */
    @Query("SELECT * FROM calendar_events WHERE startAt < :end AND endAt > :start ORDER BY startAt ASC")
    fun observeOverlapping(start: Instant, end: Instant): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE startAt < :end AND endAt > :start ORDER BY startAt ASC")
    suspend fun getOverlapping(start: Instant, end: Instant): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events")
    suspend fun getAll(): List<CalendarEventEntity>
}

@Dao
interface ReminderDao {
    @Insert suspend fun insert(reminder: ReminderEntity): Long
    @Update suspend fun update(reminder: ReminderEntity)
    @Delete suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, triggerAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0 AND triggerAt >= :from ORDER BY triggerAt ASC LIMIT :limit")
    fun observeUpcoming(from: Instant, limit: Int): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0")
    suspend fun getActive(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun getForTask(taskId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE familyMemberId = :memberId AND type = 'BIRTHDAY'")
    suspend fun getBirthdayReminders(memberId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE documentId = :documentId")
    suspend fun getForDocument(documentId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>
}

// ---------------------------------------------------------------- Family & health

@Dao
interface FamilyMemberDao {
    @Insert suspend fun insert(member: FamilyMemberEntity): Long
    @Update suspend fun update(member: FamilyMemberEntity)
    @Delete suspend fun delete(member: FamilyMemberEntity)

    @Query("SELECT * FROM family_members ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getById(id: Long): FamilyMemberEntity?

    @Query("SELECT * FROM family_members")
    suspend fun getAll(): List<FamilyMemberEntity>
}

@Dao
interface HealthDao {
    @Insert suspend fun insertRecord(record: HealthRecordEntity): Long
    @Delete suspend fun deleteRecord(record: HealthRecordEntity)

    @Query("SELECT * FROM health_records WHERE recordedAt >= :start ORDER BY recordedAt DESC")
    fun observeRecordsSince(start: Instant): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_records")
    suspend fun getAllRecords(): List<HealthRecordEntity>

    @Insert suspend fun insertMedication(med: MedicationReminderEntity): Long
    @Update suspend fun updateMedication(med: MedicationReminderEntity)
    @Delete suspend fun deleteMedication(med: MedicationReminderEntity)

    @Query("SELECT * FROM medication_reminders ORDER BY isActive DESC, medicineName COLLATE NOCASE")
    fun observeMedications(): Flow<List<MedicationReminderEntity>>

    @Query("SELECT * FROM medication_reminders WHERE id = :id")
    suspend fun getMedication(id: Long): MedicationReminderEntity?

    @Query("SELECT * FROM medication_reminders")
    suspend fun getAllMedications(): List<MedicationReminderEntity>

    @Insert suspend fun insertAppointment(appointment: AppointmentEntity): Long
    @Update suspend fun updateAppointment(appointment: AppointmentEntity)
    @Delete suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments ORDER BY at DESC")
    fun observeAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointment(id: Long): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE at >= :start AND at < :end ORDER BY at ASC")
    suspend fun getAppointmentsBetween(start: Instant, end: Instant): List<AppointmentEntity>

    @Query("SELECT * FROM appointments")
    suspend fun getAllAppointments(): List<AppointmentEntity>
}

// ---------------------------------------------------------------- Notes

@Dao
interface NoteDao {
    @Insert suspend fun insert(note: NoteEntity): Long
    @Update suspend fun update(note: NoteEntity)
    @Delete suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE isArchived = :archived ORDER BY isPinned DESC, updatedAt DESC")
    fun observe(archived: Boolean): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<NoteEntity>
}

@Dao
interface NoteTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<NoteTagEntity>)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteForNote(noteId: Long)

    @Query("SELECT * FROM note_tags")
    fun observeAll(): Flow<List<NoteTagEntity>>

    @Query("SELECT * FROM note_tags WHERE noteId = :noteId")
    suspend fun getForNote(noteId: Long): List<NoteTagEntity>
}

// ---------------------------------------------------------------- Documents

@Dao
interface DocumentDao {
    @Insert suspend fun insert(document: DocumentEntity): Long
    @Update suspend fun update(document: DocumentEntity)
    @Delete suspend fun delete(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY addedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents")
    suspend fun getAll(): List<DocumentEntity>
}

@Dao
interface DocumentFolderDao {
    @Insert suspend fun insert(folder: DocumentFolderEntity): Long
    @Update suspend fun update(folder: DocumentFolderEntity)
    @Delete suspend fun delete(folder: DocumentFolderEntity)

    @Query("SELECT * FROM document_folders ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<DocumentFolderEntity>>
}

// ---------------------------------------------------------------- Goals & habits

@Dao
interface GoalDao {
    @Insert suspend fun insert(goal: GoalEntity): Long
    @Update suspend fun update(goal: GoalEntity)
    @Delete suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE isCompleted = 0 ORDER BY targetDate IS NULL, targetDate ASC, createdAt DESC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, targetDate IS NULL, targetDate ASC, createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>
}

@Dao
interface HabitDao {
    @Insert suspend fun insert(habit: HabitEntity): Long
    @Update suspend fun update(habit: HabitEntity)
    @Delete suspend fun delete(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_entries")
    fun observeAllEntries(): Flow<List<HabitEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: HabitEntryEntity): Long

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND date = :date")
    suspend fun deleteEntry(habitId: Long, date: LocalDate)

    @Query("SELECT * FROM habits")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habit_entries")
    suspend fun getAllEntries(): List<HabitEntryEntity>
}

// ---------------------------------------------------------------- Assistant

@Dao
interface AiDao {
    @Insert suspend fun insertConversation(c: AiConversationEntity): Long
    @Update suspend fun updateConversation(c: AiConversationEntity)
    @Delete suspend fun deleteConversation(c: AiConversationEntity)

    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    suspend fun getConversation(id: Long): AiConversationEntity?

    @Insert suspend fun insertMessage(m: AiMessageEntity): Long

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<AiMessageEntity>>
}
