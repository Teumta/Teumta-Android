package com.teumta.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Figma 변수: radius/…
object TeumtaShapes {
    val Xs   = RoundedCornerShape(4.dp)   // 체크박스, 지도 역 이름
    val Sm   = RoundedCornerShape(8.dp)   // 칩, 스테퍼 버튼, 말풍선
    val Md   = RoundedCornerShape(12.dp)  // 버튼, 입력, 검색창, 경로 입력
    val Lg   = RoundedCornerShape(16.dp)  // 대화상자
    val Xl   = RoundedCornerShape(20.dp)  // 결과 시트 위 모서리
    val Full = CircleShape                // 역 점, 지도 핀, 라디오
}

fun TeumtaShapes.toMaterialShapes() = Shapes(
    extraSmall = Xs,
    small = Sm,
    medium = Md,
    large = Lg,
    extraLarge = Xl,
)
