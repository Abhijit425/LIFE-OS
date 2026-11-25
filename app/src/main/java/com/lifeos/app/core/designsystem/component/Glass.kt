package com.lifeos.app.core.designsystem.component

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing

/**
 * App-wide backdrop: midnight base with two slowly drifting light orbs.
 *
 * Platform note: Compose cannot blur what is *behind* a view (true backdrop blur). We get the
 * glass look by blurring the light orbs themselves (API 31+, RenderEffect) and drawing cards as
 * translucent layers with a light-catching border. Below API 31 the orbs are radial gradients,
 * which are already soft, so the look degrades gracefully.
 */
@Composable
fun LifeBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = LifeTheme.colors
    val reduced = LifeTheme.reducedMotion
    // Read only inside the draw lambda so the animation re-draws without recomposing the screen.
    val driftState: State<Float> = if (reduced) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "bg").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "drift",
        )
    }
    Box(modifier.fillMaxSize().background(colors.background)) {
        val orbModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(60.dp) else Modifier
        Canvas(Modifier.fillMaxSize().then(orbModifier)) {
            val drift = driftState.value
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.backgroundGlowA, Color.Transparent),
                    center = Offset(w * (0.15f + 0.2f * drift), h * 0.12f),
                    radius = w * 0.75f,
                ),
                radius = w * 0.75f,
                center = Offset(w * (0.15f + 0.2f * drift), h * 0.12f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.backgroundGlowB, Color.Transparent),
                    center = Offset(w * (0.9f - 0.15f * drift), h * 0.7f),
                    radius = w * 0.7f,
                ),
                radius = w * 0.7f,
                center = Offset(w * (0.9f - 0.15f * drift), h * 0.7f),
            )
        }
        content()
    }
}

/** Frosted glass card with a soft light-catching border. Pressable when [onClick] is set. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = Radii.lg,
    strong: Boolean = false,
    contentPadding: Dp = Spacing.lg,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LifeTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !LifeTheme.reducedMotion) 0.98f else 1f,
        animationSpec = tween(140),
        label = "cardPress",
    )
    val haptics = LocalHapticFeedback.current
    var m = modifier
        .scale(scale)
        .clip(shape)
        .background(if (strong) colors.glassFillStrong else colors.glassFill)
        .border(1.dp, colors.borderBrush, shape)
    if (onClick != null) {
        m = m.clickable(
            interactionSource = interaction,
            indication = androidx.compose.material3.ripple(),
            role = Role.Button,
            onClickLabel = onClickLabel,
        ) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    }
    Column(m.padding(contentPadding), content = content)
}
