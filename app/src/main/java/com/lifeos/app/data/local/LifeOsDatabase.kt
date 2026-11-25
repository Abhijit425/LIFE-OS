package com.lifeos.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifeos.app.data.local.dao.BudgetDao
import com.lifeos.app.data.local.dao.CalendarEventDao
import com.lifeos.app.data.local.dao.DashboardWidgetDao
import com.lifeos.app.data.local.dao.DocumentDao
import com.lifeos.app.data.local.dao.FamilyMemberDao
import com.lifeos.app.data.local.dao.GoalDao
import com.lifeos.app.data.local.dao.NoteDao
import com.lifeos.app.data.local.dao.ReminderDao
import com.lifeos.app.data.local.dao.TaskDao
import com.lifeos.app.data.local.dao.TransactionDao
import com.lifeos.app.data.local.dao.UserProfileDao
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

/**
 * Schema v1 defines every table LIFE OS needs, so later phases add DAOs and screens rather than
 * tables. Any future schema change bumps [version] and ships a Migration in [Migrations]; the
 * exported JSON in app/schemas lets migration tests verify each step.
 */
@Database(
    version = 1,
    exportSchema = true,
    entities = [
        UserProfileEntity::class, FamilyMemberEntity::class, DashboardWidgetEntity::class,
        AccountEntity::class, TransactionEntity::class, BudgetEntity::class, SavingsGoalEntity::class,
        ProjectEntity::class, MilestoneEntity::class, TaskEntity::class, SubTaskEntity::class,
        CalendarEventEntity::class, ReminderEntity::class,
        HealthRecordEntity::class, MedicationReminderEntity::class, AppointmentEntity::class,
        DocumentFolderEntity::class, DocumentEntity::class,
        NoteEntity::class, NoteTagEntity::class,
        GoalEntity::class, HabitEntity::class, HabitEntryEntity::class,
        AiConversationEntity::class, AiMessageEntity::class,
    ],
)
@TypeConverters(Converters::class)
abstract class LifeOsDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun dashboardWidgetDao(): DashboardWidgetDao
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun reminderDao(): ReminderDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun goalDao(): GoalDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun noteDao(): NoteDao
    abstract fun documentDao(): DocumentDao

    companion object {
        const val NAME = "lifeos.db"
    }
}

/** Registry of schema migrations. Empty at v1; each future version appends here. */
object Migrations {
    val ALL: Array<androidx.room.migration.Migration> = emptyArray()
}
