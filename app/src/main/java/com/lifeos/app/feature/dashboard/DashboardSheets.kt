package com.lifeos.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.LifeConfirmDialog
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSecondaryButton
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Palette
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.domain.model.DashboardWidget
import com.lifeos.app.domain.model.ReminderItem
import java.time.LocalDate
import java.time.ZoneId

@Composable
private fun sheetColor() = if (LifeTheme.colors.isDark) Palette.MidnightRaised else Palette.Porcelain

/**
 * Show/hide ("add") widgets and reorder them. Arrow buttons are used instead of drag-and-drop
 * because they are fully accessible with TalkBack and keyboards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeDashboardSheet(
    widgets: List<DashboardWidget>,
    onDismiss: () -> Unit,
    onToggle: (index: Int, visible: Boolean) -> Unit,
    onMove: (index: Int, delta: Int) -> Unit,
    onReset: () -> Unit,
) {
    val colors = LifeTheme.colors
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetColor(),
    ) {
        Column(Modifier.padding(horizontal = Spacing.xl).navigationBarsPadding()) {
            Text("Customize dashboard", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Text(
                "Turn widgets on or off and change their order. Changes save automatically.",
                style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary,
            )
            Spacer(Modifier.height(Spacing.md))
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                itemsIndexed(widgets, key = { _, w -> w.type.name }) { index, w ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            w.type.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (w.isVisible) colors.textPrimary else colors.textTertiary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onMove(index, -1) }, enabled = index > 0) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move ${w.type.title} up")
                        }
                        IconButton(onClick = { onMove(index, 1) }, enabled = index < widgets.lastIndex) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move ${w.type.title} down")
                        }
                        Switch(
                            checked = w.isVisible,
                            onCheckedChange = { onToggle(index, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.accentPurple),
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LifeSecondaryButton("Reset layout", onClick = { confirmReset = true }, modifier = Modifier.weight(1f))
                LifePrimaryButton("Done", onClick = onDismiss, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
    if (confirmReset) {
        LifeConfirmDialog(
            title = "Reset dashboard layout?",
            message = "All widgets will be shown again in their default order. Your data is not affected.",
            confirmLabel = "Reset",
            onConfirm = {
                confirmReset = false
                onReset()
            },
            onDismiss = { confirmReset = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSheet(
    reminders: List<ReminderItem>,
    today: LocalDate,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val colors = LifeTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = sheetColor()) {
        Column(Modifier.padding(horizontal = Spacing.xl).navigationBarsPadding()) {
            Text("Upcoming reminders", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Spacer(Modifier.height(Spacing.md))
            if (reminders.isEmpty()) {
                EmptyState(
                    Icons.Outlined.NotificationsNone, "You're all clear",
                    "No reminders are scheduled. Reminders you create in Calendar, Tasks or Finance appear here.",
                )
            } else {
                reminders.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                            Text(
                                r.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                            )
                        }
                        Text(TimeFormat.relative(r.triggerAt, today, zone), style = MaterialTheme.typography.labelMedium, color = colors.accentCyan)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            LifeSecondaryButton("Open Calendar", onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
