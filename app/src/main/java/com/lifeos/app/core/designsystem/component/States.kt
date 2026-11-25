package com.lifeos.app.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing

/** Pulsing skeleton block used while real data loads. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    val alpha = if (LifeTheme.reducedMotion) {
        0.5f
    } else {
        val t = rememberInfiniteTransition(label = "skeleton")
        val a by t.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
        a
    }
    Box(
        modifier
            .clip(Radii.sm)
            .alpha(alpha)
            .background(LifeTheme.colors.glassFillStrong),
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().semantics { contentDescription = "Loading" },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SkeletonBlock(Modifier.fillMaxWidth(0.6f).height(18.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(14.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.8f).height(14.dp))
    }
}

/** Empty state: explains what's missing and offers the one real action that fills it. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LifeTheme.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(Radii.pill).background(colors.accentPurple.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accentPurple)
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xxs))
        Text(message, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.sm))
            LifeSecondaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val colors = LifeTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = colors.danger)
        Spacer(Modifier.size(Spacing.sm))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
        if (onRetry != null) {
            LifeSecondaryButton(text = "Retry", onClick = onRetry)
        }
    }
}
