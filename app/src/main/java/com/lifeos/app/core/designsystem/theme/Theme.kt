package com.lifeos.app.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.lifeos.app.domain.model.MotionPreference
import com.lifeos.app.domain.model.ThemeMode

/** True when animations should be minimised (system "Remove animations" or user setting). */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun systemAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
fun LifeOsTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    motionPreference: MotionPreference = MotionPreference.FOLLOW_SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val lifeColors = if (dark) DarkLifeColors else LightLifeColors
    val reduced = when (motionPreference) {
        MotionPreference.REDUCED -> true
        MotionPreference.FULL -> false
        MotionPreference.FOLLOW_SYSTEM -> systemAnimationsDisabled()
    }
    val scheme = if (dark) {
        darkColorScheme(
            primary = lifeColors.accentPurple,
            secondary = lifeColors.accentCyan,
            tertiary = lifeColors.accentBlue,
            background = lifeColors.background,
            surface = Palette.MidnightRaised,
            onPrimary = Palette.Snow,
            onBackground = lifeColors.textPrimary,
            onSurface = lifeColors.textPrimary,
            onSurfaceVariant = lifeColors.textSecondary,
            error = lifeColors.danger,
        )
    } else {
        lightColorScheme(
            primary = lifeColors.accentPurple,
            secondary = lifeColors.accentCyan,
            tertiary = lifeColors.accentBlue,
            background = lifeColors.background,
            surface = Palette.Porcelain,
            onPrimary = Palette.Snow,
            onBackground = lifeColors.textPrimary,
            onSurface = lifeColors.textPrimary,
            onSurfaceVariant = lifeColors.textSecondary,
            error = lifeColors.danger,
        )
    }
    CompositionLocalProvider(
        LocalLifeColors provides lifeColors,
        LocalReducedMotion provides reduced,
    ) {
        MaterialTheme(colorScheme = scheme, typography = LifeTypography, content = content)
    }
}

/** Shorthand accessors: `LifeTheme.colors.textPrimary`. */
object LifeTheme {
    val colors: LifeColors
        @Composable get() = LocalLifeColors.current
    val reducedMotion: Boolean
        @Composable get() = LocalReducedMotion.current
    val layout: LayoutInfo
        @Composable get() = LocalLayoutInfo.current
}
