package com.teumta.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Figma 변수: 틈타 Semantic (Light / Dark)

@Immutable
data class TeumtaColors(
    // 배경
    val bgCanvas: Color,
    val bgSurface: Color,
    val bgField: Color,
    val bgInverse: Color,
    val bgAccentSubtle: Color,
    val bgDangerSubtle: Color,
    val bgMap: Color,
    // 글자·아이콘
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnInverse: Color,
    val textOnAccent: Color,
    val textAccent: Color,
    val textDanger: Color,
    // 테두리
    val borderDefault: Color,
    val borderFocus: Color,
    val borderDanger: Color,
    // 동작
    val actionPrimary: Color,
    val actionDisabled: Color,
    // 강조
    val accentDefault: Color,
    val statusDanger: Color,
    val routeLine1: Color,
    val routeLine1Subtle: Color,
    // 겹침
    val shadowBase: Color,
)

val LightTeumtaColors = TeumtaColors(
    // 배경
    bgCanvas = Gray50,
    bgSurface = Gray0,
    bgField = Gray100,
    bgInverse = Gray900,
    bgAccentSubtle = Orange50,
    bgDangerSubtle = Red50,
    bgMap = Gray150,
    // 글자·아이콘
    textPrimary = Gray900,
    textSecondary = Gray700,
    textTertiary = Gray600,
    textOnInverse = Gray0,
    textOnAccent = Gray0,
    textAccent = Orange700,
    textDanger = Red700,
    // 테두리
    borderDefault = Gray200,
    borderFocus = Gray900,
    borderDanger = Red600,
    // 동작
    actionPrimary = Orange600,
    actionDisabled = Gray100,
    // 강조
    accentDefault = Orange600,
    statusDanger = Red600,
    routeLine1 = Blue600,
    routeLine1Subtle = Blue300,
    // 겹침
    shadowBase = Gray950,
)

val DarkTeumtaColors = TeumtaColors(
    // 배경
    bgCanvas = Gray950,
    bgSurface = Gray900,
    bgField = Gray800,
    bgInverse = Gray600,
    bgAccentSubtle = Orange900,
    bgDangerSubtle = Red900,
    bgMap = Gray850,
    // 글자·아이콘
    textPrimary = Gray50,
    textSecondary = Gray400,
    textTertiary = Gray500,
    textOnInverse = Gray0,
    textOnAccent = Gray0,
    textAccent = Orange300,
    textDanger = Red200,
    // 테두리
    borderDefault = Gray750,
    borderFocus = Gray100,
    borderDanger = Red300,
    // 동작
    actionPrimary = Orange600,
    actionDisabled = Gray800,
    // 강조
    accentDefault = Orange400,
    statusDanger = Red300,
    routeLine1 = Blue400,
    routeLine1Subtle = Blue700,
    // 겹침
    shadowBase = Gray1000,
)
