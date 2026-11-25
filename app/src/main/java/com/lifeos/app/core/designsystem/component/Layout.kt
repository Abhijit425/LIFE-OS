package com.lifeos.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing

/** Header inside a widget/card: icon + title + optional trailing content. */
@Composable
fun CardHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LifeTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.accentCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(Spacing.xs))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing?.invoke()
    }
}

/** Large screen title used at the top of every main screen. */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val colors = LifeTheme.colors
    Column(modifier.padding(bottom = Spacing.lg)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Spacing.xxs))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

/** Label/value pair used in stat rows. */
@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier, valueColor: androidx.compose.ui.graphics.Color? = null) {
    val colors = LifeTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor ?: colors.textPrimary, maxLines = 1)
    }
}
