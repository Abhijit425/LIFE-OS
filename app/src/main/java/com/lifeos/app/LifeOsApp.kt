package com.lifeos.app

import android.app.Application
import com.lifeos.app.core.notify.BackgroundEntryPoint
import com.lifeos.app.core.notify.Channels
import com.lifeos.app.core.notify.MaintenanceWorker
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LifeOsApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Channels.create(this)
        MaintenanceWorker.enqueue(this)
        // Safety net: re-arm alarms and post due recurring transactions on every launch.
        val entry = EntryPointAccessors.fromApplication(this, BackgroundEntryPoint::class.java)
        appScope.launch {
            runCatching { entry.financeRepository().processRecurring() }
            runCatching { entry.reminderEngine().rescheduleAll() }
        }
    }
}
