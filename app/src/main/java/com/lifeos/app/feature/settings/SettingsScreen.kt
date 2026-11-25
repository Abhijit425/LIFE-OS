package com.lifeos.app.feature.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.BuildConfig
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSecondaryButton
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SwitchRow
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.designsystem.theme.WidthClass
import com.lifeos.app.data.repository.ProfileRepositoryImpl
import com.lifeos.app.domain.model.MotionPreference
import com.lifeos.app.domain.model.ThemeMode
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.feature.common.ReminderReadinessCard
import com.lifeos.app.feature.documents.formatSize
import com.lifeos.app.navigation.screenContentPadding
import java.time.LocalDate

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val storedName by vm.displayName.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val restart by vm.restartRequired.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(restart) { if (restart) restartApp(context) }
    val twoColumn = LifeTheme.layout.widthClass == WidthClass.Expanded

    MessageBox(vm) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenContentPadding())) {
            ScreenHeader("Settings", "LIFE OS keeps everything on this device. No account, no cloud, no ads.", onBack = onBack)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = Spacing.sm), color = LifeTheme.colors.accentCyan)
            val left: @Composable () -> Unit = {
                ProfileCard(storedName, vm::saveName)
                AppearanceCard(prefs.themeMode, vm::setTheme)
                MotionCard(prefs.motion, vm::setMotion)
                ReminderReadinessCard(alwaysShow = true)
                SecurityCard(prefs.appLock, vm)
            }
            val right: @Composable () -> Unit = {
                BackupCard(vm)
                ExportCard(vm)
                ModelCard(vm)
                RegionCard()
                AboutCard()
            }
            if (twoColumn) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) { left() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) { right() }
                }
            } else {
                Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) { left(); right() }
            }
        }
    }
}

fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    if (intent != null) context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}

@Composable
private fun ProfileCard(storedName: String?, onSave: (String) -> Unit) {
    var draft by rememberSaveable(storedName) { mutableStateOf(storedName.orEmpty()) }
    val tooLong = draft.trim().length > ProfileRepositoryImpl.MAX_NAME_LENGTH
    GlassCard {
        CardHeader(Icons.Outlined.Person, "Profile")
        Spacer(Modifier.height(Spacing.md))
        LifeTextField(draft, { draft = it.take(ProfileRepositoryImpl.MAX_NAME_LENGTH + 10) }, "Your name (used in greetings)", Modifier.fillMaxWidth(),
            isError = tooLong, supportingText = if (tooLong) "Maximum ${ProfileRepositoryImpl.MAX_NAME_LENGTH} characters" else null)
        Spacer(Modifier.height(Spacing.sm))
        LifePrimaryButton("Save name", onClick = { onSave(draft) }, enabled = draft.isNotBlank() && !tooLong && draft.trim() != storedName)
    }
}

@Composable
private fun AppearanceCard(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    GlassCard {
        CardHeader(Icons.Outlined.Palette, "Theme")
        Column(Modifier.selectableGroup()) {
            OptionRow("Dark (default)", current == ThemeMode.DARK) { onSelect(ThemeMode.DARK) }
            OptionRow("Light", current == ThemeMode.LIGHT) { onSelect(ThemeMode.LIGHT) }
            OptionRow("Follow system", current == ThemeMode.SYSTEM) { onSelect(ThemeMode.SYSTEM) }
        }
    }
}

@Composable
private fun MotionCard(current: MotionPreference, onSelect: (MotionPreference) -> Unit) {
    GlassCard {
        CardHeader(Icons.Outlined.Animation, "Motion")
        Column(Modifier.selectableGroup()) {
            OptionRow("Follow system animation setting", current == MotionPreference.FOLLOW_SYSTEM) { onSelect(MotionPreference.FOLLOW_SYSTEM) }
            OptionRow("Full animations", current == MotionPreference.FULL) { onSelect(MotionPreference.FULL) }
            OptionRow("Reduce motion", current == MotionPreference.REDUCED) { onSelect(MotionPreference.REDUCED) }
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LifeTheme.colors
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, null, colors = RadioButtonDefaults.colors(selectedColor = colors.accentCyan, unselectedColor = colors.textSecondary))
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
    }
}

@Composable
private fun SecurityCard(appLock: Boolean, vm: SettingsViewModel) {
    val context = LocalContext.current
    val auth = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val available = BiometricManager.from(context).canAuthenticate(auth) == BiometricManager.BIOMETRIC_SUCCESS
    GlassCard {
        CardHeader(Icons.Outlined.Fingerprint, "Privacy & security")
        SwitchRow(
            "App lock", appLock && available, { vm.setAppLock(it) }, enabled = available || appLock,
            description = if (available) "Require biometrics or your screen lock when opening LIFE OS" else "Set up a screen lock on this device to use app lock",
        )
        Text(
            "Encrypted documents use a key stored in Android Keystore. Backups are encrypted with your password. These measures reduce risk, but no app can promise absolute security.",
            style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textTertiary,
        )
    }
}

@Composable
private fun BackupCard(vm: SettingsViewModel) {
    var passwordFor by remember { mutableStateOf<String?>(null) } // "backup" or "restore"
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var backupPassword by remember { mutableStateOf("") }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) vm.createBackup(uri, backupPassword)
        backupPassword = ""
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { pendingRestore = uri; passwordFor = "restore" }
    }
    GlassCard {
        CardHeader(Icons.Outlined.Backup, "Encrypted backup & restore")
        Text("Saves your whole database to a file you choose (e.g. Downloads or a USB drive), encrypted with your password. Documents stored as references aren't included; private and encrypted copies aren't either — export them separately if needed.",
            style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textSecondary)
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            LifePrimaryButton("Back up", onClick = { password = ""; passwordFor = "backup" })
            LifeSecondaryButton("Restore", onClick = { open.launch(arrayOf("*/*")) })
        }
    }
    passwordFor?.let { mode ->
        var confirm by remember { mutableStateOf("") }
        val ok = password.length >= 8 && (mode == "restore" || password == confirm)
        LifeSheet(
            if (mode == "backup") "Backup password" else "Restore backup", { passwordFor = null; password = "" },
            saveLabel = if (mode == "backup") "Choose location" else "Restore",
            saveEnabled = ok,
            onSave = {
                if (mode == "backup") {
                    backupPassword = password
                    create.launch("lifeos-backup-${LocalDate.now()}.lifeos")
                } else {
                    pendingRestore?.let { vm.restore(it, password) }
                }
                password = ""
                passwordFor = null
            },
        ) {
            if (mode == "restore") Text("Restoring replaces ALL current data on this device with the backup. The app will restart.",
                style = MaterialTheme.typography.bodyMedium, color = LifeTheme.colors.danger)
            LifeTextField(password, { password = it.take(128) }, "Password (min 8 characters)", Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Password, visualTransformation = PasswordVisualTransformation())
            if (mode == "backup") {
                Spacer(Modifier.height(Spacing.sm))
                LifeTextField(confirm, { confirm = it.take(128) }, "Confirm password", Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Password, visualTransformation = PasswordVisualTransformation(),
                    isError = confirm.isNotEmpty() && confirm != password)
                Text("If you forget this password the backup cannot be opened.", style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textTertiary)
            }
        }
    }
}

@Composable
private fun ExportCard(vm: SettingsViewModel) {
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportJson) }
    val tx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::exportTransactions) }
    val tasks = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::exportTasks) }
    GlassCard {
        CardHeader(Icons.Outlined.FileDownload, "Export data")
        Text("Readable exports for spreadsheets or other apps. They are not encrypted — store them carefully.",
            style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textSecondary)
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            LifeSecondaryButton("All (JSON)", onClick = { json.launch("lifeos-export-${LocalDate.now()}.json") })
            LifeSecondaryButton("Money (CSV)", onClick = { tx.launch("lifeos-transactions-${LocalDate.now()}.csv") })
        }
        Spacer(Modifier.height(Spacing.xs))
        LifeSecondaryButton("Tasks (CSV)", onClick = { tasks.launch("lifeos-tasks-${LocalDate.now()}.csv") })
    }
}

@Composable
private fun ModelCard(vm: SettingsViewModel) {
    val model by vm.model.collectAsStateWithLifecycle()
    val cap = vm.capacity
    val colors = LifeTheme.colors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importModel) }
    GlassCard {
        CardHeader(Icons.Outlined.Memory, "Local AI model (optional)")
        Text("LIFE AI always works in basic mode: an offline rule-based command engine. It is not a generative AI.",
            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(Spacing.xs))
        Text("This device: ${formatSize(cap.totalRamBytes)} RAM · ${formatSize(cap.freeStorageBytes)} free storage",
            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.height(Spacing.xs))
        if (model != null) {
            Text("Imported: ${model!!.fileName} (${formatSize(model!!.sizeBytes)})", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        }
        Text(
            if (vm.runtimeAvailable) "On-device inference is available."
            else "This build includes model import and device checks, but not the llama.cpp inference engine, so an imported model is stored and not used yet. LIFE AI stays in basic mode.",
            style = MaterialTheme.typography.bodySmall, color = colors.warning,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text("To import: download a small quantized GGUF model (e.g. a 1–3B parameter Q4_K_M file from Hugging Face) to this device, then tap Import. Nothing is downloaded by LIFE OS itself.",
            style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            LifeSecondaryButton("Import GGUF", onClick = { picker.launch(arrayOf("*/*")) })
            if (model != null) LifeSecondaryButton("Remove", onClick = { vm.deleteModel() })
        }
    }
}

@Composable
private fun RegionCard() {
    GlassCard {
        CardHeader(Icons.Outlined.Info, "Currency & language")
        Text("Currency: Indian Rupee (₹) with lakh/crore grouping. Language: English. More options can be added in a future update.",
            style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textSecondary)
    }
}

@Composable
private fun AboutCard() {
    val colors = LifeTheme.colors
    GlassCard {
        CardHeader(Icons.Outlined.Info, "About LIFE OS")
        Text("One Life. One Intelligence. Total Control.", style = MaterialTheme.typography.titleSmall, color = colors.accentCyan)
        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        listOf(
            "Offline-first: no internet permission is requested.",
            "No account, registration or mandatory cloud sync.",
            "No analytics, tracking or advertising SDKs.",
            "Android cloud backup is disabled for your data; use the encrypted backup above.",
            "Finance is a manual tracker and never connects to banks.",
            "Health is for personal organisation, not medical advice.",
        ).forEach { Text("•  $it", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(vertical = 2.dp)) }
    }
}
