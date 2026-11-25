package com.lifeos.app.feature.common

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeSecondaryButton
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.notify.AlarmScheduler
import com.lifeos.app.core.notify.Notifier
import com.lifeos.app.data.repository.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderReadiness(val notifications: Boolean, val exactAlarms: Boolean, val batteryUnrestricted: Boolean)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val notifier: Notifier,
    private val scheduler: AlarmScheduler,
    private val engine: ReminderEngine,
) : ViewModel() {
    val state = MutableStateFlow(read())
    private fun read() = ReminderReadiness(notifier.notificationsEnabled(), scheduler.canScheduleExact(), notifier.isIgnoringBatteryOptimizations())

    fun refresh() {
        val before = state.value
        val now = read()
        state.value = now
        // Newly granted exact alarms → re-arm everything precisely.
        if (!before.exactAlarms && now.exactAlarms) viewModelScope.launch { engine.rescheduleAll() }
    }
}

/**
 * Explains and fixes what can stop reminders arriving: notification permission (Android 13+),
 * exact alarms (Android 12+) and battery optimisation. Hidden when everything is fine.
 */
@Composable
fun ReminderReadinessCard(modifier: Modifier = Modifier, alwaysShow: Boolean = false, vm: PermissionsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    if (!alwaysShow && s.notifications && s.exactAlarms) return
    val colors = LifeTheme.colors
    GlassCard(modifier, strong = true) {
        CardHeader(Icons.Outlined.NotificationsActive, "Reminder delivery")
        Spacer(Modifier.height(Spacing.xs))
        Line(if (s.notifications) "✓ Notifications are allowed." else "✗ Notifications are off — reminders will be saved but you won't be alerted.", s.notifications)
        if (!s.notifications) {
            LifeSecondaryButton("Allow notifications", onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            })
            Text("If Android doesn't show a prompt, enable notifications in App info → Notifications.", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        }
        Spacer(Modifier.height(Spacing.xs))
        Line(if (s.exactAlarms) "✓ Exact alarms allowed — reminders arrive on time." else "! Exact alarms are off — Android may deliver reminders a few minutes late.", s.exactAlarms)
        if (!s.exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LifeSecondaryButton("Allow alarms & reminders", onClick = {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            })
        }
        Spacer(Modifier.height(Spacing.xs))
        Line(
            if (s.batteryUnrestricted) "✓ Battery optimisation is off for LIFE OS." else "Some phones (incl. many Vivo/Oppo/Xiaomi models) stop background alarms to save battery. If reminders go missing, set LIFE OS's battery usage to Unrestricted.",
            s.batteryUnrestricted,
        )
        if (!s.batteryUnrestricted) {
            LifeSecondaryButton("Open battery settings", onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) })
        }
    }
}

@Composable
private fun Line(text: String, ok: Boolean) {
    val c = LifeTheme.colors
    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (ok) c.textSecondary else c.textPrimary)
}
