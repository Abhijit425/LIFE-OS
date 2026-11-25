package com.lifeos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.lifeos.app.data.local.entity.BudgetEntity
import com.lifeos.app.data.local.entity.CalendarEventEntity
import com.lifeos.app.data.local.entity.DashboardWidgetEntity
import com.lifeos.app.data.local.entity.DocumentEntity
import com.lifeos.app.data.local.entity.FamilyMemberEntity
import com.lifeos.app.data.local.entity.GoalEntity
import com.lifeos.app.data.local.entity.NoteEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.data.local.entity.TransactionEntity
import com.lifeos.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/*
 * Phase 1 DAOs: the read paths the dashboard needs plus basic writes (used by tests and by the
 * feature modules that arrive in Phase 2+). Enum columns are stored by name, so queries compare
 * against literal names such as 'DONE'.
 */

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMissing(widgets: List<DashboardWidgetEntity>)

    @Upsert
    abstract suspend fun upsertAll(widgets: List<DashboardWidgetEntity>)

    @Query("DELETE FROM dashboard_widgets")
    abstract suspend fun clear()

    /** Atomic reset so the dashboard is never observed half-written. */
    @Transaction
    open suspend fun replaceAll(widgets: List<DashboardWidgetEntity>) {
        clear()
        upsertAll(widgets)
    }
}

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    /** Open tasks due in [start, end). */
    @Query(
        """SELECT * FROM tasks WHERE status != 'DONE' AND dueAt >= :start AND dueAt < :end
           ORDER BY dueAt ASC"""
    )
    fun observeOpenDueBetween(start: Instant, end: Instant): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'DONE' AND completedAt >= :start AND completedAt < :end")
    fun observeCompletedBetween(start: Instant, end: Instant): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'DONE' AND dueAt IS NOT NULL AND dueAt < :before")
    fun observeOverdueCount(before: Instant): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'DONE'")
    fun observeOpenCount(): Flow<Int>
}

/** Amount + timestamp projection for chart bucketing. */
data class AmountAt(val amountPaise: Long, val occurredAt: Instant)

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    /** [type] is a TransactionType name. */
    @Query(
        """SELECT COALESCE(SUM(amountPaise), 0) FROM transactions
           WHERE type = :type AND occurredAt >= :start AND occurredAt < :end"""
    )
    fun observeTotal(type: String, start: Instant, end: Instant): Flow<Long>

    @Query(
        """SELECT amountPaise, occurredAt FROM transactions
           WHERE type = :type AND occurredAt >= :start ORDER BY occurredAt ASC"""
    )
    fun observeAmountsSince(type: String, start: Instant): Flow<List<AmountAt>>
}

@Dao
interface BudgetDao {
    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("SELECT COALESCE(SUM(limitPaise), 0) FROM budgets WHERE monthKey = :monthKey")
    fun observeTotalLimit(monthKey: Int): Flow<Long>
}

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query(
        """SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0 AND triggerAt >= :from
           ORDER BY triggerAt ASC LIMIT :limit"""
    )
    fun observeUpcoming(from: Instant, limit: Int): Flow<List<ReminderEntity>>
}

@Dao
interface CalendarEventDao {
    @Insert
    suspend fun insert(event: CalendarEventEntity): Long

    /** Events overlapping [start, end). */
    @Query("SELECT * FROM calendar_events WHERE startAt < :end AND endAt > :start ORDER BY startAt ASC")
    fun observeOverlapping(start: Instant, end: Instant): Flow<List<CalendarEventEntity>>
}

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Query("SELECT * FROM goals WHERE isCompleted = 0 ORDER BY targetDate IS NULL, targetDate ASC, createdAt DESC")
    fun observeActive(): Flow<List<GoalEntity>>
}

@Dao
interface FamilyMemberDao {
    @Insert
    suspend fun insert(member: FamilyMemberEntity): Long

    @Delete
    suspend fun delete(member: FamilyMemberEntity)

    @Query("SELECT * FROM family_members ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FamilyMemberEntity>>
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<NoteEntity>>
}

@Dao
interface DocumentDao {
    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Query("SELECT * FROM documents ORDER BY addedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DocumentEntity>>
}
