package com.lifeos.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.BuildConfig
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ScreenTitle
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.designsystem.theme.WidthClass
import com.lifeos.app.data.repository.ProfileRepositoryImpl
import com.lifeos.app.domain.model.MotionPreference
import com.lifeos.app.domain.model.ThemeMode
import com.lifeos.app.navigation.screenContentPadding
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val colors = LifeTheme.colors
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val storedName by viewModel.displayName.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    val twoColumn = LifeTheme.layout.widthClass == WidthClass.Expanded

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenContentPadding()),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            ScreenTitle("Settings", "LIFE OS keeps everything on this device. No account, no cloud, no ads.")
            if (twoColumn) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        ProfileCard(storedName, viewModel::saveName)
                        AppearanceCard(prefs.themeMode, viewModel::setTheme)
                        MotionCard(prefs.motion, viewModel::setMotion)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        UpcomingSettingsCard()
                        AboutCard()
                    }
                }
            } else {
                Column(Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    ProfileCard(storedName, viewModel::saveName)
                    AppearanceCard(prefs.themeMode, viewModel::setTheme)
                    MotionCard(prefs.motion, viewModel::setMotion)
                    UpcomingSettingsCard()
                    AboutCard()
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
    }
}

@Composable
private fun ProfileCard(storedName: String?, onSave: (String) -> Unit) {
    val colors = LifeTheme.colors
    var draft by rememberSaveable(storedName) { mutableStateOf(storedName.orEmpty()) }
    val tooLong = draft.trim().length > ProfileRepositoryImpl.MAX_NAME_LENGTH
    GlassCard {
        CardHeader(Icons.Outlined.Person, "Profile")
        Spacer(Modifier.height(Spacing.md))
        LifeTextField(
            value = draft,
            onValueChange = { draft = it.take(ProfileRepositoryImpl.MAX_NAME_LENGTH + 10) },
            label = "Your name (used in greetings)",
            isError = tooLong,
            supportingText = if (tooLong) "Maximum ${ProfileRepositoryImpl.MAX_NAME_LENGTH} characters" else null,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        LifePrimaryButton(
            text = "Save name",
            onClick = { onSave(draft) },
            enabled = draft.isNotBlank() && !tooLong && draft.trim() != storedName,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text("Stored only on this device.", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
    }
}

@Composable
private fun AppearanceCard(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    GlassCard {
        CardHeader(Icons.Outlined.Palette, "Theme")
        Spacer(Modifier.height(Spacing.sm))
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
        Spacer(Modifier.height(Spacing.sm))
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
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.accentCyan, unselectedColor = colors.textSecondary),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
    }
}

/** Settings delivered in later phases are listed as information only — they're not tappable. */
@Composable
private fun UpcomingSettingsCard() {
    val colors = LifeTheme.colors
    val items = listOf(
        "Notification preferences" to 3,
        "Currency & language (INR / English today)" to 6,
        "Biometric app lock" to 6,
        "Local AI model (GGUF) settings" to 5,
        "Encrypted backup & restore" to 6,
        "Export / import data (CSV, JSON)" to 6,
    )
    GlassCard {
        CardHeader(Icons.Outlined.Lock, "Privacy & data")
        Spacer(Modifier.height(Spacing.sm))
        items.forEach { (label, phase) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.weight(1f))
                LifeChip("Phase $phase", colors.textTertiary)
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val colors = LifeTheme.colors
    GlassCard {
        CardHeader(Icons.Outlined.Info, "About LIFE OS")
        Spacer(Modifier.height(Spacing.sm))
        Text("One Life. One Intelligence. Total Control.", style = MaterialTheme.typography.titleSmall, color = colors.accentCyan)
        Spacer(Modifier.height(Spacing.xs))
        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(Spacing.sm))
        listOf(
            "Offline-first: works with no internet permission at all.",
            "No account, registration or mandatory cloud sync.",
            "No analytics, tracking or advertising SDKs.",
            "Android cloud backup is disabled for your data; LIFE OS will offer its own encrypted backup.",
            "Security measures reduce risk; no app can promise absolute security.",
        ).forEach {
            Text("•  $it", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}
