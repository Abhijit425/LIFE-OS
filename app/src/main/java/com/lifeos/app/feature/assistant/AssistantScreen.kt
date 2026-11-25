package com.lifeos.app.feature.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.component.AiOrb
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.navigation.screenContentPadding

/**
 * LIFE AI home. The command engine (Phase 5) isn't built yet, so this screen says exactly that
 * rather than offering a chat box that can't act. No input field = no false promises.
 */
@Composable
fun AssistantScreen() {
    val colors = LifeTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xl))
        AiOrb(size = 180.dp, active = true)
        Spacer(Modifier.height(Spacing.lg))
        Text("LIFE AI", style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
        Spacer(Modifier.height(Spacing.xs))
        LifeChip("Command engine arrives in Phase 5", colors.warning)
        Spacer(Modifier.height(Spacing.xl))
        GlassCard(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            Text("How LIFE AI will work", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Spacer(Modifier.height(Spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Bullet("Basic mode (always on, offline): a rule-based command engine that understands requests like “How much did I spend this month?” or “Remind me to pay the electricity bill tomorrow” and acts on your local data. It is automation, not a generative AI model.")
                Bullet("Optional generative mode: import a small quantized GGUF model and run it on-device with llama.cpp. Nothing is downloaded unless you choose to, and the app checks RAM and storage first.")
                Bullet("It will only say an action is done after it has actually been saved, and it will always ask before deleting anything.")
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text("•  $text", style = MaterialTheme.typography.bodyMedium, color = LifeTheme.colors.textSecondary, textAlign = TextAlign.Start)
}
