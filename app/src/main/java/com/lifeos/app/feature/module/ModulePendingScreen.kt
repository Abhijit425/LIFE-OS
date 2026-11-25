package com.lifeos.app.feature.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.lifeos.app.navigation.LifeModule
import com.lifeos.app.navigation.screenContentPadding

/**
 * Honest placeholder for modules scheduled in later build phases: it states what the module will
 * do and which phase delivers it. It contains no inert buttons and no sample data.
 */
@Composable
fun ModulePendingScreen(module: LifeModule, onBack: (() -> Unit)?) {
    val colors = LifeTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding()),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(bottom = Spacing.xs)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
        }
        ScreenTitle(module.title, module.summary)
        GlassCard(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(), strong = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(module.icon, contentDescription = null, tint = colors.accentCyan, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("Arriving in build Phase ${module.phase}", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                LifeChip("In development", colors.warning)
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                "The database tables for this module already exist, and the dashboard will show its records as soon as the module ships. It will include:",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(Spacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                module.features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, tint = colors.accentPurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(feature, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}
