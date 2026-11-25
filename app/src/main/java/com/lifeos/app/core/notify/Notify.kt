package com.lifeos.app.core.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lifeos.app.MainActivity
import com.lifeos.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class AlarmKind { REMINDER, MEDICATION, APPOINTMENT }

object Channels {
    const val REMINDERS = "reminders"
    const val MEDICINE = "medicine"
    const val APPOINTMENTS = "appointments"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Tasks, bills, birthdays and your own reminders"
                },
                NotificationChannel(MEDICINE, "Medicine", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Medicine reminders you set from your prescriptions"
                },
                NotificationChannel(APPOINTMENTS, "Appointments", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Upcoming appointments"
                },
            ),
        )
    }
}

/**
 * Schedules alarms with AlarmManager. Exact alarms are used when the system allows them
 * (Android 12+ requires the "Alarms & reminders" permission); otherwise an inexact
 * allow-while-idle alarm is used, which Android may delay by several minutes in Doze.
 */
@Singleton
class AlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(kind: AlarmKind, id: Long, slot: Int, at: Instant) {
        val pi = pendingIntent(kind, id, slot, create = true) ?: return
        val millis = at.toEpochMilli()
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    fun cancel(kind: AlarmKind, id: Long, slot: Int = 0) {
        pendingIntent(kind, id, slot, create = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(kind: AlarmKind, id: Long, slot: Int, create: Boolean): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            // Unique data URI → a distinct PendingIntent per alarm.
            data = Uri.parse("lifeos://alarm/${kind.name}/$id/$slot")
            putExtra(EXTRA_KIND, kind.name)
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_SLOT, slot)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    companion object {
        const val ACTION_ALARM = "com.lifeos.app.ALARM"
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
        const val EXTRA_SLOT = "slot"
    }
}

@Singleton
class Notifier @Inject constructor(@ApplicationContext private val context: Context) {

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notificationsEnabled(): Boolean = hasPermission() && NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isIgnoringBatteryOptimizations(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun show(channel: String, notificationId: Int, title: String, text: String?) {
        if (!hasPermission()) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, n)
        } catch (_: SecurityException) {
            // Permission revoked between check and post — nothing else to do.
        }
    }
}
