package com.lifeos.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Palette
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Screen header: optional back, big title, subtitle and trailing actions (e.g. "Add"). */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LifeTheme.colors
    Column(Modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** Glass pill segmented control for tabs/views. */
@Composable
fun <T> SegmentedTabs(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = LifeTheme.colors
    Row(
        modifier
            .clip(Radii.pill)
            .background(colors.glassFill)
            .border(1.dp, colors.borderBrush, Radii.pill)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSel = option == selected
            Box(
                Modifier
                    .clip(Radii.pill)
                    .background(if (isSel) colors.accentBrush else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                    .clickable(role = Role.Tab) { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(label(option), style = MaterialTheme.typography.labelLarge, color = if (isSel) Color.White else colors.textSecondary)
            }
        }
    }
}

/** Horizontally scrolling single-choice chips. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = LifeTheme.colors
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        options.forEach { o ->
            FilterChip(
                selected = o == selected,
                onClick = { onSelect(o) },
                label = { Text(label(o)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.accentPurple.copy(alpha = 0.3f),
                    selectedLabelColor = colors.textPrimary,
                    labelColor = colors.textSecondary,
                ),
            )
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = LifeTheme.colors.textSecondary, modifier = Modifier.padding(top = Spacing.sm, bottom = 4.dp))
}

/** Bottom-sheet form container with save / cancel / optional delete (delete always confirms). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeSheet(
    title: String,
    onDismiss: () -> Unit,
    onSave: (() -> Unit)?,
    saveEnabled: Boolean = true,
    saveLabel: String = "Save",
    onDelete: (() -> Unit)? = null,
    deleteMessage: String = "This will be permanently removed.",
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LifeTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (colors.isDark) Palette.MidnightRaised else Palette.Porcelain,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Spacer(Modifier.height(Spacing.sm))
            content()
            Spacer(Modifier.height(Spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = colors.danger) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(if (onSave == null) "Close" else "Cancel", color = colors.textSecondary) }
                if (onSave != null) LifePrimaryButton(text = saveLabel, onClick = onSave, enabled = saveEnabled)
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
    if (confirmDelete && onDelete != null) {
        LifeConfirmDialog(
            title = "Delete?",
            message = deleteMessage,
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Tappable field that opens a Material date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, date: LocalDate?, onDate: (LocalDate?) -> Unit, modifier: Modifier = Modifier, allowClear: Boolean = true) {
    var open by rememberSaveable { mutableStateOf(false) }
    PickerField(label, date?.let { TimeFormat.shortDate(it) + " " + it.year } ?: "Not set", Icons.Outlined.CalendarToday, { open = true }, modifier,
        onClear = if (allowClear && date != null) ({ onDate(null) }) else null)
    if (open) {
        // DatePicker works in UTC-midnight millis.
        val state = rememberDatePickerState(initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, time: LocalTime?, onTime: (LocalTime?) -> Unit, modifier: Modifier = Modifier, allowClear: Boolean = true) {
    var open by rememberSaveable { mutableStateOf(false) }
    PickerField(label, time?.let { "%d:%02d %s".format(if (it.hour % 12 == 0) 12 else it.hour % 12, it.minute, if (it.hour < 12) "AM" else "PM") } ?: "Not set",
        Icons.Outlined.Schedule, { open = true }, modifier, onClear = if (allowClear && time != null) ({ onTime(null) }) else null)
    if (open) {
        val initial = time ?: LocalTime.of(9, 0)
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onTime(LocalTime.of(state.hour, state.minute))
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    onClear: (() -> Unit)?,
) {
    val colors = LifeTheme.colors
    Column(modifier) {
        FieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(Radii.md)
                .background(colors.glassFill)
                .border(1.dp, colors.glassBorder, Radii.md)
                .clickable(role = Role.Button, onClickLabel = "Choose $label", onClick = onClick)
                .padding(horizontal = Spacing.md, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accentCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
            if (onClear != null) {
                Icon(
                    Icons.Outlined.Close, contentDescription = "Clear $label", tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp).clickable(role = Role.Button, onClick = onClear),
                )
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, description: String? = null, enabled: Boolean = true) {
    val colors = LifeTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) colors.textPrimary else colors.textTertiary)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = colors.accentPurple))
    }
}

@Composable
fun SearchField(query: String, onQuery: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = LifeTheme.colors
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text(placeholder, color = colors.textTertiary) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.textSecondary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = colors.textSecondary) }
            }
        },
        singleLine = true,
        shape = Radii.pill,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accentCyan,
            unfocusedBorderColor = colors.glassBorder,
            focusedContainerColor = colors.glassFill,
            unfocusedContainerColor = colors.glassFill,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Standard list row inside a glass card. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    strikeThrough: Boolean = false,
) {
    val colors = LifeTheme.colors
    var m = modifier.fillMaxWidth().clip(Radii.md)
    if (onClick != null) m = m.clickable(role = Role.Button, onClick = onClick)
    Row(m.padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Spacing.sm))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title, style = MaterialTheme.typography.bodyLarge,
                color = if (strikeThrough) colors.textTertiary else colors.textPrimary,
                textDecoration = if (strikeThrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Spacing.sm))
            trailing()
        }
    }
}

/** Content width cap so forms and lists stay readable on large tablets. */
fun Modifier.readableWidth(): Modifier = this.widthIn(max = 900.dp)

/** Disclaimer banner used by Health. */
@Composable
fun NoticeCard(text: String, modifier: Modifier = Modifier) {
    val colors = LifeTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.textPrimary,
        modifier = modifier
            .fillMaxWidth()
            .clip(Radii.md)
            .background(colors.warning.copy(alpha = 0.14f))
            .padding(Spacing.md),
    )
}
