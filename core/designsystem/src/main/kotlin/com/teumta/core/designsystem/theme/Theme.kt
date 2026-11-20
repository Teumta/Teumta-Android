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
        // 고른 칩(FilterChip)의 바탕. 회색(bgField)이면 바탕과 구분이 안 돼서 강조색 옅은 바탕을 쓴다
        secondaryContainer = bgAccentSubtle,
        onSecondaryContainer = textAccent,
        tertiary = routeLine1,
        onTertiary = textOnAccent,
        background = bgCanvas,
        onBackground = textPrimary,
        surface = bgSurface,
        onSurface = textPrimary,
        surfaceVariant = bgField,
        onSurfaceVariant = textSecondary,
        // 카드(Card)·시트가 쓰는 바탕. 비워 두면 Material 기본값(보랏빛 회색)이 나와서 모두 bgSurface로 맞춘다
        surfaceContainerLowest = bgSurface,
        surfaceContainerLow = bgSurface,
        surfaceContainer = bgSurface,
        surfaceContainerHigh = bgSurface,
        surfaceContainerHighest = bgSurface,
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
