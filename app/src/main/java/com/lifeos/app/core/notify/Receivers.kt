package com.lifeos.app.core.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifeos.app.data.repository.FinanceRepository
import com.lifeos.app.data.repository.ReminderEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Receivers and workers get dependencies through an entry point (no constructor injection). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackgroundEntryPoint {
    fun reminderEngine(): ReminderEngine
    fun financeRepository(): FinanceRepository
}

private fun Context.entry(): BackgroundEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, BackgroundEntryPoint::class.java)

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_ALARM) return
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND)?.let { k -> AlarmKind.entries.firstOrNull { it.name == k } } ?: return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L).takeIf { it > 0 } ?: return
        val slot = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT, 0)
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.entry().reminderEngine().onAlarm(kind, id, slot)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Alarms are wiped on reboot and can drift after clock/zone changes, so we rebuild them
 * from the database whenever any of these system events happen.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.entry().reminderEngine().rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}

/** Daily maintenance: posts due recurring transactions and re-arms alarms as a safety net. */
class MaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val e = applicationContext.entry()
        return runCatching {
            e.financeRepository().processRecurring()
            e.reminderEngine().rescheduleAll()
        }.fold({ Result.success() }, { Result.retry() })
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("lifeos-maintenance", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
