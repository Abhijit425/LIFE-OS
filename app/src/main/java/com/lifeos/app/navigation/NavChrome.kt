package com.lifeos.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.component.AiOrb
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Palette
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing

/** Height reserved at the bottom of phone layouts so content clears the floating bar. */
val FloatingBarClearance: Dp = 104.dp

private val navSurfaceDark = Palette.MidnightRaised.copy(alpha = 0.86f)
private val navSurfaceLight = Color.White.copy(alpha = 0.9f)

@Composable
fun FloatingBottomBar(
    selected: TopLevel?,
    onSelect: (TopLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LifeTheme.colors
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .shadow(24.dp, Radii.pill, ambientColor = colors.accentPurple, spotColor = colors.accentPurple)
            .clip(Radii.pill)
            .background(if (colors.isDark) navSurfaceDark else navSurfaceLight)
            .border(1.dp, colors.borderBrush, Radii.pill)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopLevel.entries.forEach { item ->
            NavItem(item = item, selected = item == selected, onClick = { onSelect(item) }, vertical = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FloatingNavRail(
    selected: TopLevel?,
    onSelect: (TopLevel) -> Unit,
    onOpenSettings: () -> Unit,
    settingsSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LifeTheme.colors
    Column(
        modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(Spacing.md)
            .width(96.dp)
            .shadow(24.dp, Radii.lg, ambientColor = colors.accentPurple, spotColor = colors.accentPurple)
            .clip(Radii.lg)
            .background(if (colors.isDark) navSurfaceDark else navSurfaceLight)
            .border(1.dp, colors.borderBrush, Radii.lg)
            .padding(vertical = Spacing.lg, horizontal = Spacing.xs)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        AiOrb(size = 44.dp)
        Spacer(Modifier.height(Spacing.md))
        TopLevel.entries.forEach { item ->
            NavItem(item = item, selected = item == selected && !settingsSelected, onClick = { onSelect(item) }, vertical = true)
        }
        Spacer(Modifier.weight(1f))
        RailIconButton(selected = settingsSelected, onClick = onOpenSettings)
    }
}

@Composable
private fun NavItem(
    item: TopLevel,
    selected: Boolean,
    onClick: () -> Unit,
    vertical: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LifeTheme.colors
    val haptics = LocalHapticFeedback.current
    val tint by animateColorAsState(
        if (selected) Color.White else colors.textSecondary,
        animationSpec = tween(if (LifeTheme.reducedMotion) 0 else 220),
        label = "navTint",
    )
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .clip(Radii.lg)
            .selectable(
                selected = selected,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                role = Role.Tab,
                interactionSource = interaction,
                indication = ripple(),
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .clip(Radii.pill)
                .background(if (selected) colors.accentBrush else SolidColor(Color.Transparent))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        if (vertical) {
            Spacer(Modifier.height(3.dp))
            Text(
                item.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RailIconButton(selected: Boolean, onClick: () -> Unit) {
    val colors = LifeTheme.colors
    Column(
        Modifier
            .clip(Radii.lg)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Settings, contentDescription = null, tint = if (selected) colors.accentCyan else colors.textSecondary)
        Text("Settings", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

/**
 * Padding every main screen applies: system bars, the floating bar on phones, and wider
 * gutters on tablets.
 */
@Composable
fun screenContentPadding(): PaddingValues {
    val layout = LifeTheme.layout
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(
        start = layout.contentPadding,
        end = layout.contentPadding,
        top = top + Spacing.md,
        bottom = if (layout.useNavRail) bottomInset + Spacing.xl else FloatingBarClearance + bottomInset,
    )
}
