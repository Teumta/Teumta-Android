package com.teumta.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalTeumtaColors = staticCompositionLocalOf { LightTeumtaColors }
private val LocalTeumtaTypography = staticCompositionLocalOf { TeumtaTypography() }

@Composable
fun TeumtaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkTeumtaColors else LightTeumtaColors
    val typography = TeumtaTypography()
    CompositionLocalProvider(
        LocalTeumtaColors provides colors,
        LocalTeumtaTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = typography.toMaterialTypography(),
            shapes = TeumtaShapes.toMaterialShapes(),
            content = content,
        )
    }
}

object TeumtaTheme {
    val colors: TeumtaColors
        @Composable @ReadOnlyComposable get() = LocalTeumtaColors.current
    val typography: TeumtaTypography
        @Composable @ReadOnlyComposable get() = LocalTeumtaTypography.current
}

// 동적 색(Material You)은 쓰지 않아요. 노선색이 바뀌면 안 돼서요.
fun TeumtaColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = actionPrimary,
        onPrimary = textOnAccent,
        primaryContainer = bgAccentSubtle,
        onPrimaryContainer = textAccent,
        secondary = accentDefault,
        onSecondary = textOnAccent,
        secondaryContainer = bgField,
        onSecondaryContainer = textPrimary,
        tertiary = routeLine1,
        onTertiary = textOnAccent,
        background = bgCanvas,
        onBackground = textPrimary,
        surface = bgSurface,
        onSurface = textPrimary,
        surfaceVariant = bgField,
        onSurfaceVariant = textSecondary,
        surfaceContainer = bgSurface,
        surfaceContainerHigh = bgSurface,
        inverseSurface = bgInverse,
        inverseOnSurface = textOnInverse,
        outline = borderDefault,
        outlineVariant = borderDefault,
        error = statusDanger,
        onError = textOnAccent,
        errorContainer = bgDangerSubtle,
        onErrorContainer = textDanger,
    )
}
