package com.lifeos.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Raw palette. Screens should use [LifeColors] tokens, never these directly. */
object Palette {
    val Midnight = Color(0xFF070A1A)
    val MidnightRaised = Color(0xFF0E1330)
    val DeepIndigo = Color(0xFF1B1550)
    val Purple = Color(0xFF8B5CF6)
    val PurpleSoft = Color(0xFFC4B5FD)
    val ElectricBlue = Color(0xFF3B82F6)
    val Cyan = Color(0xFF22D3EE)
    val Mint = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val Coral = Color(0xFFF87171)
    val Snow = Color(0xFFF5F7FF)
    val Mist = Color(0xFFB4BCD9)
    val Slate = Color(0xFF6B7393)

    val Porcelain = Color(0xFFF3F5FC)
    val Ink = Color(0xFF0D1230)
    val InkSoft = Color(0xFF4A5275)
}

/** Semantic colour tokens for the LIFE OS glass design system. */
@Immutable
data class LifeColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundGlowA: Color,
    val backgroundGlowB: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentPurple: Color,
    val accentBlue: Color,
    val accentCyan: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
) {
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accentPurple, accentBlue, accentCyan))
    val borderBrush: Brush
        get() = Brush.linearGradient(listOf(glassHighlight, glassBorder, accentCyan.copy(alpha = 0.18f)))
}

val DarkLifeColors = LifeColors(
    isDark = true,
    background = Palette.Midnight,
    backgroundGlowA = Palette.Purple.copy(alpha = 0.28f),
    backgroundGlowB = Palette.Cyan.copy(alpha = 0.16f),
    glassFill = Color.White.copy(alpha = 0.06f),
    glassFillStrong = Color.White.copy(alpha = 0.10f),
    glassBorder = Color.White.copy(alpha = 0.10f),
    glassHighlight = Color.White.copy(alpha = 0.28f),
    textPrimary = Palette.Snow,
    textSecondary = Palette.Mist,
    textTertiary = Palette.Slate,
    accentPurple = Palette.Purple,
    accentBlue = Palette.ElectricBlue,
    accentCyan = Palette.Cyan,
    success = Palette.Mint,
    warning = Palette.Amber,
    danger = Palette.Coral,
)

val LightLifeColors = LifeColors(
    isDark = false,
    background = Palette.Porcelain,
    backgroundGlowA = Palette.Purple.copy(alpha = 0.16f),
    backgroundGlowB = Palette.Cyan.copy(alpha = 0.14f),
    glassFill = Color.White.copy(alpha = 0.62f),
    glassFillStrong = Color.White.copy(alpha = 0.80f),
    glassBorder = Palette.Ink.copy(alpha = 0.08f),
    glassHighlight = Color.White.copy(alpha = 0.95f),
    textPrimary = Palette.Ink,
    textSecondary = Palette.InkSoft,
    textTertiary = Palette.Slate,
    accentPurple = Color(0xFF7C3AED),
    accentBlue = Color(0xFF2563EB),
    accentCyan = Color(0xFF0891B2),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    danger = Color(0xFFDC2626),
)

val LocalLifeColors = staticCompositionLocalOf { DarkLifeColors }
