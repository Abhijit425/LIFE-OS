package com.lifeos.app.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Radii

/**
 * The LIFE AI orb: layered gradient core, rotating halo and a breathing glow.
 * Pure Canvas (no Lottie file needed); static when reduced motion is on.
 */
@Composable
fun AiOrb(size: Dp, modifier: Modifier = Modifier, active: Boolean = false) {
    val colors = LifeTheme.colors
    val reduced = LifeTheme.reducedMotion
    // States are read inside the draw lambda only: the orb animates without recomposition.
    val rotation: State<Float>
    val breath: State<Float>
    if (reduced) {
        rotation = remember { mutableFloatStateOf(0f) }
        breath = remember { mutableFloatStateOf(0.5f) }
    } else {
        val t = rememberInfiniteTransition(label = "orb")
        rotation = t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (active) 2400 else 9000, easing = LinearEasing)), label = "rot")
        breath = t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    }
    Canvas(modifier.size(size).semantics { contentDescription = "LIFE AI" }) {
        val c = center
        val radius = this.size.minDimension / 2f
        val b = breath.value
        val rot = rotation.value
        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                listOf(colors.accentPurple.copy(alpha = 0.35f + 0.2f * b), Color.Transparent),
                center = c, radius = radius,
            ),
            radius = radius, center = c,
        )
        // Rotating halo
        rotate(rot, c) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(colors.accentCyan, colors.accentPurple, colors.accentBlue, colors.accentCyan),
                    center = c,
                ),
                radius = radius * 0.68f, center = c,
                style = Stroke(width = radius * 0.06f, cap = StrokeCap.Round),
            )
        }
        // Core
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, colors.accentCyan, colors.accentBlue, colors.accentPurple),
                center = Offset(c.x - radius * 0.12f, c.y - radius * 0.14f),
                radius = radius * 0.62f,
            ),
            radius = radius * (0.48f + 0.03f * b), center = c,
        )
        // Specular highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = radius * 0.1f,
            center = Offset(c.x - radius * 0.18f, c.y - radius * 0.2f),
        )
    }
}

/** Animated circular progress with gradient stroke. [progress] in 0..1. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 10.dp,
    label: String? = null,
    subLabel: String? = null,
) {
    val colors = LifeTheme.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (LifeTheme.reducedMotion) tween(0) else tween(900, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = colors.glassFillStrong, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(colors.accentCyan, colors.accentBlue, colors.accentPurple, colors.accentCyan)),
                startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (label != null) Text(label, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            if (subLabel != null) Text(subLabel, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
}

/** Thin animated linear progress bar. */
@Composable
fun GlowProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color? = null) {
    val colors = LifeTheme.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (LifeTheme.reducedMotion) tween(0) else tween(800, easing = FastOutSlowInEasing),
        label = "bar",
    )
    Box(modifier.fillMaxWidth().height(8.dp).clip(Radii.pill).background(colors.glassFillStrong)) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .clip(Radii.pill)
                .background(if (color != null) Brush.horizontalGradient(listOf(color, color)) else colors.accentBrush),
        )
    }
}

data class BarDatum(val label: String, val value: Float)

/**
 * Compose-native animated bar chart. Bars grow in on first show; the highlighted (last) bar uses
 * the accent gradient. Accessible: the whole chart has a spoken summary.
 */
@Composable
fun AnimatedBarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    accessibilitySummary: String,
) {
    val colors = LifeTheme.colors
    val reduced = LifeTheme.reducedMotion
    val growth = remember(data) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(data) {
        if (!reduced) growth.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val max = (data.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(1f)
    Column(modifier.semantics { contentDescription = accessibilitySummary }) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (data.isEmpty()) return@Canvas
            val slot = size.width / data.size
            val barW = slot * 0.46f
            data.forEachIndexed { i, d ->
                val h = (d.value / max) * size.height * growth.value
                val left = i * slot + (slot - barW) / 2
                // Track
                drawRoundRect(
                    color = colors.glassFillStrong,
                    topLeft = Offset(left, 0f), size = Size(barW, size.height),
                    cornerRadius = CornerRadius(barW / 2, barW / 2),
                )
                if (h > 0f) {
                    val isLast = i == data.lastIndex
                    drawRoundRect(
                        brush = if (isLast) {
                            Brush.verticalGradient(listOf(colors.accentCyan, colors.accentPurple), startY = size.height - h, endY = size.height)
                        } else {
                            Brush.verticalGradient(listOf(colors.accentBlue.copy(alpha = 0.7f), colors.accentPurple.copy(alpha = 0.5f)))
                        },
                        topLeft = Offset(left, size.height - h), size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2, barW / 2),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            data.forEach {
                Text(
                    it.label, style = MaterialTheme.typography.labelSmall, color = colors.textTertiary,
                    maxLines = 1, overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
