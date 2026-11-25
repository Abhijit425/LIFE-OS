package com.lifeos.app.feature.settings

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.lifeos.app.data.repository.BackupManager
import com.lifeos.app.data.repository.DeviceCapacity
import com.lifeos.app.data.repository.ExportManager
import com.lifeos.app.data.repository.ModelInfo
import com.lifeos.app.data.repository.ModelManager
import com.lifeos.app.domain.model.MotionPreference
import com.lifeos.app.domain.model.ThemeMode
import com.lifeos.app.domain.repository.AppPreferences
import com.lifeos.app.domain.repository.PreferencesRepository
import com.lifeos.app.domain.repository.ProfileRepository
import com.lifeos.app.feature.common.LifeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val profile: ProfileRepository,
    private val backup: BackupManager,
    private val export: ExportManager,
    private val models: ModelManager,
) : LifeViewModel() {

    val preferences: StateFlow<AppPreferences> = prefs.preferences.stateWhileSubscribed(AppPreferences())
    val displayName: StateFlow<String?> = profile.displayName.stateWhileSubscribed(null)
    val busy = MutableStateFlow(false)
    val model = MutableStateFlow(models.installed())
    val capacity: DeviceCapacity = models.capacity()
    val runtimeAvailable = models.runtimeAvailable

    /** Set once a restore has replaced the database; the UI then restarts the app. */
    val restartRequired = MutableStateFlow(false)

    fun setTheme(mode: ThemeMode) = act { prefs.setThemeMode(mode) }
    fun setMotion(pref: MotionPreference) = act { prefs.setMotion(pref) }
    fun setAppLock(enabled: Boolean) = act(if (enabled) "App lock on. You'll unlock with your screen lock or biometrics." else "App lock off.") { prefs.setAppLock(enabled) }

    fun saveName(name: String) {
        viewModelScope.launch { say(if (profile.setDisplayName(name)) "Name saved." else "Please enter a name of 1–40 characters.") }
    }

    private fun busyAct(success: String, block: suspend () -> Unit) {
        busy.value = true
        act(success) {
            try { block() } finally { busy.value = false }
        }
    }

    fun createBackup(uri: Uri, password: String) = busyAct("Encrypted backup saved. Keep your password safe — it can't be recovered.") {
        backup.createBackup(uri, password.toCharArray())
    }

    fun restore(uri: Uri, password: String) = busyAct("Backup restored. Restarting…") {
        backup.restoreBackup(uri, password.toCharArray())
        restartRequired.value = true
    }

    fun exportJson(uri: Uri) = busyAct("All records exported as JSON.") { export.exportJson(uri) }
    fun exportTransactions(uri: Uri) = busyAct("Transactions exported as CSV.") { export.exportTransactionsCsv(uri) }
    fun exportTasks(uri: Uri) = busyAct("Tasks exported as CSV.") { export.exportTasksCsv(uri) }

    fun importModel(uri: Uri) = busyAct("Model imported.") {
        val info: ModelInfo = models.import(uri)
        model.value = info
    }

    fun deleteModel() = act("Model removed.") {
        models.delete()
        model.value = null
    }
}
