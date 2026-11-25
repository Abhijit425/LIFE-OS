package com.lifeos.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing

/** Primary action: gradient pill. */
@Composable
fun LifePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = LifeTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Radii.pill,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(Radii.pill)
            .background(if (enabled) colors.accentBrush else androidx.compose.ui.graphics.SolidColor(colors.glassFillStrong)),
    ) {
        ButtonContent(text, icon, Color.White)
    }
}

/** Secondary action: glass pill. */
@Composable
fun LifeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = LifeTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Radii.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.glassFillStrong,
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.glassFill,
        ),
        modifier = modifier.defaultMinSize(minHeight = 48.dp).border(1.dp, colors.borderBrush, Radii.pill),
    ) {
        ButtonContent(text, icon, colors.textPrimary)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

@Composable
fun LifeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = LifeTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = Radii.md,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accentCyan,
            unfocusedBorderColor = colors.glassBorder,
            focusedContainerColor = colors.glassFill,
            unfocusedContainerColor = colors.glassFill,
            focusedLabelColor = colors.accentCyan,
            unfocusedLabelColor = colors.textSecondary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accentCyan,
        ),
        modifier = modifier,
    )
}

/**
 * Confirmation dialog. Every destructive operation in LIFE OS goes through this
 * (rule: never silently delete user information).
 */
@Composable
fun LifeConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = LifeTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (colors.isDark) com.lifeos.app.core.designsystem.theme.Palette.MidnightRaised else Color.White,
        shape = Radii.lg,
        title = { Text(title, color = colors.textPrimary) },
        text = { Text(message, color = colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) colors.danger else colors.accentCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
        },
    )
}

/** Small rounded status chip. */
@Composable
fun LifeChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .clip(Radii.pill)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
