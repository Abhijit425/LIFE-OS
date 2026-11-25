package com.lifeos.app.data.repository

import androidx.room.withTransaction
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.entity.AppointmentEntity
import com.lifeos.app.data.local.entity.CalendarEventEntity
import com.lifeos.app.data.local.entity.FamilyMemberEntity
import com.lifeos.app.data.local.entity.GoalEntity
import com.lifeos.app.data.local.entity.HabitEntity
import com.lifeos.app.data.local.entity.HabitEntryEntity
import com.lifeos.app.data.local.entity.HealthRecordEntity
import com.lifeos.app.data.local.entity.MedicationReminderEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.ReminderType
import com.lifeos.app.domain.usecase.DashboardMath
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyRepository @Inject constructor(
    private val db: LifeOsDatabase,
    private val reminders: ReminderEngine,
    private val time: TimeProvider,
) {
    fun observeMembers(): Flow<List<FamilyMemberEntity>> = db.familyMemberDao().observeAll()

    /** Saves the member and keeps a yearly 9 AM birthday reminder in sync with their birth date. */
    suspend fun save(member: FamilyMemberEntity): Long {
        val id = if (member.id == 0L) db.familyMemberDao().insert(member) else member.id.also { db.familyMemberDao().update(member) }
        db.reminderDao().getBirthdayReminders(id).forEach { reminders.delete(it) }
        val bd = member.birthDate
        if (bd != null) {
            val zone = time.zone()
            val today = time.now().atZone(zone).toLocalDate()
            val next = DashboardMath.nextBirthday(bd, today)
            reminders.save(
                ReminderEntity(
                    title = "${member.name}'s birthday",
                    message = "Today is ${member.name}'s birthday 🎂",
                    type = ReminderType.BIRTHDAY,
                    triggerAt = next.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant(),
                    recurrence = Recurrence.YEARLY,
                    familyMemberId = id,
                    createdAt = time.now(),
                ),
            )
        }
        return id
    }

    /** Linked money records and tasks are kept and unlinked; the member's health data is removed. */
    suspend fun delete(member: FamilyMemberEntity) {
        reminders.cancelReminderAlarms(db.reminderDao().getAll().filter { it.familyMemberId == member.id })
        db.healthDao().getAllMedications().filter { it.familyMemberId == member.id }.forEach { reminders.cancelMedication(it) }
        db.healthDao().getAllAppointments().filter { it.familyMemberId == member.id }.forEach { reminders.cancelAppointment(it) }
        db.familyMemberDao().delete(member)
    }
}

@Singleton
class HealthRepository @Inject constructor(
    private val db: LifeOsDatabase,
    private val reminders: ReminderEngine,
) {
    private val dao get() = db.healthDao()

    fun observeRecordsSince(start: Instant): Flow<List<HealthRecordEntity>> = dao.observeRecordsSince(start)
    fun observeMedications(): Flow<List<MedicationReminderEntity>> = dao.observeMedications()
    fun observeAppointments(): Flow<List<AppointmentEntity>> = dao.observeAppointments()

    suspend fun addRecord(r: HealthRecordEntity) { dao.insertRecord(r) }
    suspend fun deleteRecord(r: HealthRecordEntity) = dao.deleteRecord(r)

    /** The dose text is stored exactly as entered; LIFE OS never suggests or changes doses. */
    suspend fun saveMedication(m: MedicationReminderEntity) {
        val id = if (m.id == 0L) dao.insertMedication(m) else m.id.also { dao.updateMedication(m) }
        reminders.scheduleMedication(m.copy(id = id))
    }

    suspend fun deleteMedication(m: MedicationReminderEntity) {
        reminders.cancelMedication(m)
        dao.deleteMedication(m)
    }

    suspend fun saveAppointment(a: AppointmentEntity) {
        val id = if (a.id == 0L) dao.insertAppointment(a) else a.id.also { dao.updateAppointment(a) }
        reminders.scheduleAppointment(a.copy(id = id))
    }

    suspend fun deleteAppointment(a: AppointmentEntity) {
        reminders.cancelAppointment(a)
        dao.deleteAppointment(a)
    }

    suspend fun appointmentsBetween(start: Instant, end: Instant) = dao.getAppointmentsBetween(start, end)
}

@Singleton
class CalendarRepository @Inject constructor(private val db: LifeOsDatabase) {
    fun observeRange(start: Instant, end: Instant): Flow<List<CalendarEventEntity>> = db.calendarEventDao().observeOverlapping(start, end)
    suspend fun eventsBetween(start: Instant, end: Instant) = db.calendarEventDao().getOverlapping(start, end)

    suspend fun save(e: CalendarEventEntity): Long {
        require(!e.endAt.isBefore(e.startAt)) { "End must be after start" }
        return if (e.id == 0L) db.calendarEventDao().insert(e) else e.id.also { db.calendarEventDao().update(e) }
    }

    suspend fun delete(e: CalendarEventEntity) = db.calendarEventDao().delete(e)
}

data class HabitWithEntries(val habit: HabitEntity, val dates: Set<LocalDate>)

@Singleton
class GoalRepository @Inject constructor(
    private val db: LifeOsDatabase,
    private val time: TimeProvider,
) {
    fun observeGoals(): Flow<List<GoalEntity>> = db.goalDao().observeAll()
    fun observeHabits(): Flow<List<HabitEntity>> = db.habitDao().observeActive()
    fun observeEntries(): Flow<List<HabitEntryEntity>> = db.habitDao().observeAllEntries()

    suspend fun saveGoal(g: GoalEntity): Long {
        val done = g.targetValue > 0 && g.currentValue >= g.targetValue
        val goal = g.copy(isCompleted = done || g.isCompleted)
        val id = if (goal.id == 0L) db.goalDao().insert(goal) else goal.id.also { db.goalDao().update(goal) }
        // Keep a mirrored savings goal in step.
        goal.savingsGoalId?.let { sid ->
            db.savingsGoalDao().getById(sid)?.let { s ->
                db.savingsGoalDao().update(s.copy(savedPaise = Math.round(goal.currentValue * 100), targetPaise = Math.round(goal.targetValue * 100), title = goal.title))
            }
        }
        return id
    }

    suspend fun deleteGoal(g: GoalEntity) = db.withTransaction {
        g.savingsGoalId?.let { sid -> db.savingsGoalDao().getById(sid)?.let { db.savingsGoalDao().delete(it) } }
        db.goalDao().delete(g)
    }

    suspend fun saveHabit(h: HabitEntity) {
        if (h.id == 0L) db.habitDao().insert(h) else db.habitDao().update(h)
    }

    suspend fun deleteHabit(h: HabitEntity) = db.habitDao().delete(h)

    suspend fun toggle(habitId: Long, date: LocalDate, done: Boolean) {
        if (done) db.habitDao().insertEntry(HabitEntryEntity(habitId = habitId, date = date)) else db.habitDao().deleteEntry(habitId, date)
    }

    fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()
}
