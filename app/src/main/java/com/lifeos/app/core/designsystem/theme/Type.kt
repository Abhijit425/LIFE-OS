package com.lifeos.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens. Sizes are in sp so they follow the system font scale.
 * The family is the platform sans-serif; drop Inter / Space Grotesk .ttf files into
 * res/font and swap [LifeFont] to change the whole app at once.
 */
val LifeFont: FontFamily = FontFamily.SansSerif

val LifeTypography = Typography(
    displayLarge = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Light, fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = LifeFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
)
