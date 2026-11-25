package com.lifeos.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.entity.CalendarEventEntity
import com.lifeos.app.data.local.entity.DashboardWidgetEntity
import com.lifeos.app.data.local.entity.FamilyMemberEntity
import com.lifeos.app.data.local.entity.GoalEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.data.local.entity.TransactionEntity
import com.lifeos.app.data.repository.DashboardRepositoryImpl
import com.lifeos.app.domain.model.GoalType
import com.lifeos.app.domain.model.TaskStatus
import com.lifeos.app.domain.model.TransactionCategory
import com.lifeos.app.domain.model.TransactionType
import com.lifeos.app.domain.model.WidgetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: LifeOsDatabase
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 25)
    private val noon = today.atTime(12, 0).atZone(zone).toInstant()

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LifeOsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun tearDown() = db.close()

    @Test fun widgetsResetAndPersist() = runTest {
        val repo = DashboardRepositoryImpl(db)
        repo.ensureWidgets()
        val rows = db.dashboardWidgetDao().getAll()
        assertEquals(WidgetType.entries.map { it.name }, rows.map { it.widgetKey })

        db.dashboardWidgetDao().replaceAll(listOf(DashboardWidgetEntity("QUICK_NOTES", 0, false)))
        repo.ensureWidgets() // must restore missing widgets, keeping the user's choice
        val after = db.dashboardWidgetDao().getAll()
        assertEquals(WidgetType.entries.size, after.size)
        assertEquals("QUICK_NOTES", after.first().widgetKey)
        assertEquals(false, after.first().isVisible)
    }

    @Test fun deletingFamilyMemberNullsTransactionLink() = runTest {
        val memberId = db.familyMemberDao().insert(FamilyMemberEntity(name = "Ma", relation = "Mother", createdAt = noon))
        db.transactionDao().insert(
            TransactionEntity(type = TransactionType.EXPENSE, amountPaise = 500_00, category = TransactionCategory.MEDICAL,
                occurredAt = noon, familyMemberId = memberId, createdAt = noon),
        )
        val member = db.familyMemberDao().observeAll().first().single()
        db.familyMemberDao().delete(member)
        // Expense survives (never silently delete money records); only the link is cleared.
        val total = db.transactionDao().observeTotal(TransactionType.EXPENSE.name, noon.minusSeconds(1), noon.plusSeconds(1)).first()
        assertEquals(500_00L, total)
    }

    @Test fun deletingTaskCascadesToItsReminders() = runTest {
        val taskId = db.taskDao().insert(TaskEntity(title = "Pay bill", dueAt = noon, createdAt = noon, updatedAt = noon))
        db.reminderDao().insert(ReminderEntity(title = "Pay bill", triggerAt = noon.plusSeconds(60), taskId = taskId, createdAt = noon))
        db.taskDao().delete(db.taskDao().getById(taskId)!!)
        assertTrue(db.reminderDao().observeUpcoming(noon, 10).first().isEmpty())
    }

    @Test(expected = SQLiteConstraintException::class)
    fun foreignKeysAreEnforced() = runTest {
        db.reminderDao().insert(ReminderEntity(title = "Orphan", triggerAt = noon, taskId = 999, createdAt = noon))
    }

    @Test fun snapshotReflectsStoredRecords() = runTest {
        val start = today.atStartOfDay(zone).toInstant()
        db.taskDao().insert(TaskEntity(title = "Open today", dueAt = noon, createdAt = start, updatedAt = start))
        db.taskDao().insert(
            TaskEntity(title = "Done today", status = TaskStatus.DONE, completedAt = noon, dueAt = noon, createdAt = start, updatedAt = noon),
        )
        db.taskDao().insert(TaskEntity(title = "Overdue", dueAt = start.minus(Duration.ofDays(2)), createdAt = start, updatedAt = start))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.EXPENSE, amountPaise = 1_250_00, category = TransactionCategory.FOOD, occurredAt = noon, createdAt = noon))
        db.transactionDao().insert(TransactionEntity(type = TransactionType.INCOME, amountPaise = 60_000_00, category = TransactionCategory.SALARY, occurredAt = noon, createdAt = noon))
        db.calendarEventDao().insert(CalendarEventEntity(title = "Standup", startAt = noon, endAt = noon.plusSeconds(900), createdAt = start))
        db.goalDao().insert(GoalEntity(title = "Read 12 books", type = GoalType.READING, targetValue = 12.0, currentValue = 3.0, unit = "books", createdAt = start))

        val snap = DashboardRepositoryImpl(db).observeSnapshot(today, start, zone).first()
        assertEquals(1, snap.tasks.dueTodayOpen)
        assertEquals(1, snap.tasks.completedToday)
        assertEquals(1, snap.tasks.overdue)
        assertEquals(33, snap.productivityScore)
        assertEquals(1_250_00L, snap.finance.expensePaise)
        assertEquals(60_000_00L, snap.finance.incomePaise)
        assertNull(snap.finance.budgetPaise)
        assertEquals(2, snap.schedule.size) // the event + the open task due today
        assertEquals(0.25f, snap.goals.single().progress, 0.001f)
    }
}
