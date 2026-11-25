package com.lifeos.app.data.repository

import com.lifeos.app.core.notify.AlarmKind
import com.lifeos.app.core.notify.AlarmScheduler
import com.lifeos.app.core.notify.Channels
import com.lifeos.app.core.notify.Notifier
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.entity.AppointmentEntity
import com.lifeos.app.data.local.entity.MedicationReminderEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.domain.logic.RecurrenceMath
import com.lifeos.app.domain.logic.TimeListParser
import com.lifeos.app.domain.model.Recurrence
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns every scheduled alarm: user reminders, medication times and appointment alerts.
 * The database is the source of truth; [rescheduleAll] rebuilds alarms from it after reboot,
 * app update, time change or time-zone change.
 */
@Singleton
class ReminderEngine @Inject constructor(
    private val db: LifeOsDatabase,
    private val scheduler: AlarmScheduler,
    private val notifier: Notifier,
    private val time: TimeProvider,
) {
    private val dao get() = db.reminderDao()

    fun observeAll(): Flow<List<ReminderEntity>> = dao.observeAll()

    val exactAlarmsAllowed: Boolean get() = scheduler.canScheduleExact()

    /** Inserts or updates and (re)schedules. Returns the id. */
    suspend fun save(reminder: ReminderEntity): Long {
        val id = if (reminder.id == 0L) dao.insert(reminder) else reminder.id.also { dao.update(reminder) }
        val saved = reminder.copy(id = id)
        scheduleReminder(saved)
        return id
    }

    suspend fun delete(reminder: ReminderEntity) {
        scheduler.cancel(AlarmKind.REMINDER, reminder.id)
        dao.delete(reminder)
    }

    suspend fun setCompleted(reminder: ReminderEntity, completed: Boolean) {
        val updated = reminder.copy(isCompleted = completed)
        dao.update(updated)
        if (completed) scheduler.cancel(AlarmKind.REMINDER, reminder.id) else scheduleReminder(updated)
    }

    suspend fun setEnabled(reminder: ReminderEntity, enabled: Boolean) {
        val updated = reminder.copy(isEnabled = enabled)
        dao.update(updated)
        if (enabled) scheduleReminder(updated) else scheduler.cancel(AlarmKind.REMINDER, reminder.id)
    }

    /** Cancels alarms before rows are removed by a cascade (e.g. deleting a task). */
    fun cancelReminderAlarms(reminders: List<ReminderEntity>) {
        reminders.forEach { scheduler.cancel(AlarmKind.REMINDER, it.id) }
    }

    private suspend fun scheduleReminder(r: ReminderEntity) {
        scheduler.cancel(AlarmKind.REMINDER, r.id)
        if (!r.isEnabled || r.isCompleted) return
        val now = time.now()
        val next = RecurrenceMath.nextAfter(r.triggerAt, r.recurrence, now, time.zone()) ?: return
        if (next != r.triggerAt) dao.update(r.copy(triggerAt = next))
        scheduler.schedule(AlarmKind.REMINDER, r.id, 0, next)
    }

    // ---------------- Medication

    fun scheduleMedication(med: MedicationReminderEntity) {
        cancelMedication(med)
        if (!med.isActive) return
        val times = TimeListParser.parse(med.times) ?: return
        val zone = time.zone()
        val now = time.now()
        val today = now.atZone(zone).toLocalDate()
        times.forEachIndexed { slot, t ->
            var date = if (today.isBefore(med.startDate)) med.startDate else today
            var at = date.atTime(t).atZone(zone).toInstant()
            if (!at.isAfter(now)) {
                date = date.plusDays(1)
                at = date.atTime(t).atZone(zone).toInstant()
            }
            val end = med.endDate
            if (end == null || !date.isAfter(end)) scheduler.schedule(AlarmKind.MEDICATION, med.id, slot, at)
        }
    }

    fun cancelMedication(med: MedicationReminderEntity) {
        for (slot in 0 until 12) scheduler.cancel(AlarmKind.MEDICATION, med.id, slot)
    }

    // ---------------- Appointments (alert one hour before)

    fun scheduleAppointment(a: AppointmentEntity) {
        scheduler.cancel(AlarmKind.APPOINTMENT, a.id)
        val alertAt = a.at.minus(Duration.ofHours(1))
        if (alertAt.isAfter(time.now())) scheduler.schedule(AlarmKind.APPOINTMENT, a.id, 0, alertAt)
    }

    fun cancelAppointment(a: AppointmentEntity) = scheduler.cancel(AlarmKind.APPOINTMENT, a.id)

    // ---------------- Recovery & firing

    suspend fun rescheduleAll() {
        dao.getActive().forEach { scheduleReminder(it) }
        db.healthDao().getAllMedications().forEach { scheduleMedication(it) }
        db.healthDao().getAllAppointments().forEach { scheduleAppointment(it) }
    }

    /** Called by AlarmReceiver. Posts the notification and arms the next occurrence. */
    suspend fun onAlarm(kind: AlarmKind, id: Long, slot: Int) {
        when (kind) {
            AlarmKind.REMINDER -> {
                val r = dao.getById(id) ?: return
                if (!r.isEnabled || r.isCompleted) return
                notifier.show(Channels.REMINDERS, ("r$id").hashCode(), r.title, r.message ?: r.type.name.lowercase().replaceFirstChar { it.uppercase() } + " reminder")
                if (r.recurrence != Recurrence.NONE) {
                    val next = RecurrenceMath.nextAfter(r.triggerAt, r.recurrence, time.now(), time.zone())
                    if (next != null) {
                        dao.update(r.copy(triggerAt = next))
                        scheduler.schedule(AlarmKind.REMINDER, id, 0, next)
                    }
                }
            }
            AlarmKind.MEDICATION -> {
                val med = db.healthDao().getMedication(id) ?: return
                if (!med.isActive) return
                val today: LocalDate = time.now().atZone(time.zone()).toLocalDate()
                val ended = med.endDate?.let { today.isAfter(it) } ?: false
                if (!ended) {
                    notifier.show(
                        Channels.MEDICINE, ("m$id-$slot").hashCode(),
                        "Medicine: ${med.medicineName}",
                        "${med.doseLabel} — as written in your prescription.",
                    )
                }
                scheduleMedication(med)
            }
            AlarmKind.APPOINTMENT -> {
                val a = db.healthDao().getAppointment(id) ?: return
                notifier.show(Channels.APPOINTMENTS, ("a$id").hashCode(), "Appointment in 1 hour", listOfNotNull(a.title, a.withWhom, a.location).joinToString(" · "))
            }
        }
    }

    fun nowInstant(): Instant = time.now()
}
