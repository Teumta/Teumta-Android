package com.teumta.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class TeumtaShadow(val offsetY: Dp, val blur: Dp, val alpha: Float, val elevation: Dp)

object TeumtaElevation {
    val Level1 = TeumtaShadow(2.dp, 8.dp, 0.1f, elevation = 3.dp)  // 지도 검색창, 지도 말풍선
    val Level2 = TeumtaShadow(-2.dp, 16.dp, 0.1f, elevation = 6.dp)  // 결과 시트, 대화상자
}

// Compose UI 1.9+는 Modifier.dropShadow로 offsetY, blur를 그대로 쓸 수 있어요.
fun Modifier.teumtaShadow(level: TeumtaShadow, shape: Shape, color: Color): Modifier =
    shadow(
        elevation = level.elevation,
        shape = shape,
        ambientColor = color,
        spotColor = color,
    )
