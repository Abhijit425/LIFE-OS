package com.lifeos.app.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.ScreenTitle
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.designsystem.theme.WidthClass
import com.lifeos.app.navigation.LifeModule
import com.lifeos.app.navigation.screenContentPadding

@Composable
fun MoreScreen(onOpenModule: (String) -> Unit, onOpenSettings: () -> Unit) {
    val layout = LifeTheme.layout
    val columns = when (layout.widthClass) {
        WidthClass.Compact -> 2
        WidthClass.Medium -> 3
        WidthClass.Expanded -> 4
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = screenContentPadding(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenTitle("More", "Every part of your life, organised.")
        }
        items(LifeModule.moreSection, key = { it.key }) { module ->
            ModuleTile(
                title = module.title,
                subtitle = module.summary,
                icon = { Icon(module.icon, contentDescription = null, tint = LifeTheme.colors.accentCyan, modifier = Modifier.size(28.dp)) },
                badge = null,
                onClick = { onOpenModule(module.key) },
            )
        }
        item(key = "settings") {
            ModuleTile(
                title = "Settings",
                subtitle = "Profile, theme, motion and privacy.",
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = LifeTheme.colors.accentPurple, modifier = Modifier.size(28.dp)) },
                badge = null,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun ModuleTile(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    badge: String?,
    onClick: () -> Unit,
) {
    val colors = LifeTheme.colors
    GlassCard(onClick = onClick, onClickLabel = "Open $title", modifier = Modifier.height(168.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.weight(1f))
            if (badge != null) LifeChip(badge, colors.textSecondary)
        }
        Spacer(Modifier.height(Spacing.md))
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Spacer(Modifier.height(Spacing.xxs))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 3)
        Spacer(Modifier.width(0.dp))
    }
}
