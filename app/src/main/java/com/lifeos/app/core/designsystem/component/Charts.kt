package com.lifeos.app.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing

/** Categorical palette used for slices/series. Colour is never the only carrier: legends show labels + values. */
@Composable
fun seriesColors(): List<Color> {
    val c = LifeTheme.colors
    return listOf(c.accentPurple, c.accentCyan, c.accentBlue, c.success, c.warning, c.danger,
        Color(0xFFF472B6), Color(0xFFA3E635), Color(0xFF94A3B8), Color(0xFFFB923C), Color(0xFF2DD4BF))
}

data class Slice(val label: String, val value: Float, val valueText: String)

/** Animated donut with a legend. */
@Composable
fun DonutChart(slices: List<Slice>, centerLabel: String, centerValue: String, modifier: Modifier = Modifier, size: Dp = 160.dp) {
    val colors = LifeTheme.colors
    val palette = seriesColors()
    val reduced = LifeTheme.reducedMotion
    val sweep = remember(slices) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(slices) { if (!reduced) sweep.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Row(
        modifier.semantics { contentDescription = "$centerLabel $centerValue. " + slices.joinToString { "${it.label} ${it.valueText}" } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val stroke = this.size.minDimension * 0.14f
                val inset = stroke / 2
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                drawArc(colors.glassFillStrong, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                var start = -90f
                slices.forEachIndexed { i, s ->
                    val angle = 360f * (s.value / total) * sweep.value
                    drawArc(palette[i % palette.size], start, (angle - 1.5f).coerceAtLeast(0.5f), false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    start += angle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerValue, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(centerLabel, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.width(Spacing.lg))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            slices.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(palette[i % palette.size]))
                    Spacer(Modifier.width(8.dp))
                    Text(s.label, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.valueText, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                }
            }
        }
    }
}

/** Two series per label (e.g. income vs expenses), animated. */
@Composable
fun GroupedBarChart(
    labels: List<String>,
    a: List<Float>,
    b: List<Float>,
    aName: String,
    bName: String,
    accessibilitySummary: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
) {
    val colors = LifeTheme.colors
    val reduced = LifeTheme.reducedMotion
    val growth = remember(a, b) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(a, b) { if (!reduced) growth.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val max = ((a + b).maxOrNull() ?: 0f).coerceAtLeast(1f)
    Column(modifier.semantics { contentDescription = accessibilitySummary }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Legend(colors.success, aName)
            Legend(colors.accentPurple, bName)
        }
        Spacer(Modifier.height(Spacing.xs))
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (labels.isEmpty()) return@Canvas
            val slot = size.width / labels.size
            val barW = slot * 0.28f
            labels.indices.forEach { i ->
                val left = i * slot + slot * 0.18f
                val ha = (a.getOrElse(i) { 0f } / max) * size.height * growth.value
                val hb = (b.getOrElse(i) { 0f } / max) * size.height * growth.value
                drawRoundRect(colors.success, Offset(left, size.height - ha), Size(barW, ha), CornerRadius(barW / 3, barW / 3))
                drawRoundRect(colors.accentPurple, Offset(left + barW + slot * 0.06f, size.height - hb), Size(barW, hb), CornerRadius(barW / 3, barW / 3))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
            labels.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textTertiary) }
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = LifeTheme.colors.textSecondary)
    }
}
