package com.impervir.agent.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = Color(0xFFFFE3D8),
    onPrimaryContainer = Color(0xFF3A1204),
    secondary = Lilac,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E4FF),
    onSecondaryContainer = Color(0xFF231A5C),
    tertiary = Positive,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Chip,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF9F6),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0ECE6),
    surfaceContainerHighest = Color(0xFFE9E4DD),
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    outline = Color(0xFFD9D4CC),
    outlineVariant = Line,
    error = Danger,
    scrim = Night,
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = Color(0xFF4A1A08),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = SoftLilac,
    onSecondary = Color(0xFF231A5C),
    secondaryContainer = Color(0xFF332A6E),
    onSecondaryContainer = Color(0xFFE9E4FF),
    tertiary = Color(0xFF5BD39A),
    background = Night,
    onBackground = NightText,
    surface = Night,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = Color(0xFF09080B),
    surfaceContainerLow = Color(0xFF121115),
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2B2830),
    inverseSurface = NightText,
    inverseOnSurface = Night,
    outline = Color(0xFF4A4650),
    outlineVariant = NightLine,
    error = Color(0xFFFF8A7A),
    scrim = Color.Black,
)

/** Brand colors that Material's scheme has no slot for. */
@Immutable
data class ImpervirExtras(
    val accent: Color,
    val lilac: Color,
    val softPeach: Color,
    val softLilac: Color,
    val positive: Color,
    val card: Color,
    val cardElevated: Color,
    val muted: Color,
    val isDark: Boolean,
)

val LocalImpervirExtras = staticCompositionLocalOf {
    ImpervirExtras(Accent, Lilac, SoftPeach, SoftLilac, Positive, Color.White, Color.White, Muted, false)
}

object ImpervirTheme {
    val extras: ImpervirExtras
        @Composable get() = LocalImpervirExtras.current
}

/**
 * @param dynamicColor use the wallpaper palette on Android 12+ (off by default: the brand orange is part of the product).
 */
@Composable
fun ImpervirTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val extras = ImpervirExtras(
        accent = scheme.primary,
        lilac = Lilac,
        softPeach = SoftPeach,
        softLilac = SoftLilac,
        positive = if (darkTheme) Color(0xFF5BD39A) else Positive,
        card = if (darkTheme) NightSurface else Color.White,
        cardElevated = if (darkTheme) NightSurfaceHigh else Color.White,
        muted = if (darkTheme) NightMuted else Muted,
        isDark = darkTheme,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalImpervirExtras provides extras) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ImpervirTypography,
            shapes = ImpervirShapes,
            content = content,
        )
    }
}

/** Forces the dark brand ground for immersive screens (Talk, Voice mode, Weekly report). */
@Composable
fun ImmersiveDark(content: @Composable () -> Unit) = ImpervirTheme(darkTheme = true, content = content)
