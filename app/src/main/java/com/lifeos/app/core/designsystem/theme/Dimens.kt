package com.lifeos.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp-based spacing scale. */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

object Radii {
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(18.dp)
    val lg = RoundedCornerShape(26.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/** Window width buckets (Material guidance): tablets get rails and multi-column layouts. */
enum class WidthClass { Compact, Medium, Expanded }

fun widthClassFor(widthDp: Dp): WidthClass = when {
    widthDp < 600.dp -> WidthClass.Compact
    widthDp < 840.dp -> WidthClass.Medium
    else -> WidthClass.Expanded
}

@Immutable
data class LayoutInfo(
    val widthClass: WidthClass,
    /** Side rail on medium/expanded widths (tablets), floating bottom bar on compact. */
    val useNavRail: Boolean,
    val dashboardColumns: Int,
    val contentPadding: Dp,
)

fun layoutInfoFor(widthDp: Dp): LayoutInfo {
    val wc = widthClassFor(widthDp)
    return when (wc) {
        WidthClass.Compact -> LayoutInfo(wc, useNavRail = false, dashboardColumns = 1, contentPadding = Spacing.md)
        WidthClass.Medium -> LayoutInfo(wc, useNavRail = true, dashboardColumns = 2, contentPadding = Spacing.xl)
        WidthClass.Expanded -> LayoutInfo(wc, useNavRail = true, dashboardColumns = 3, contentPadding = Spacing.xxl)
    }
}

val LocalLayoutInfo = staticCompositionLocalOf { layoutInfoFor(400.dp) }
