package com.teumta.app.ui.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teumta.core.designsystem.theme.TeumtaColors
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

// 입력 화면과 역 고르기 화면이 함께 쓰는 조각: 노선 레일과 역 점, 둥근 칩, 구역 사이 회색 띠

// 역 점 모양 (원래 기획 S-01, S-02)
enum class StationDot {
    // 흰 속에 1호선 파란 테두리: 출발역, 고를 수 있는 역
    RING,

    // 1호선 파란 점: 도착역, 고를 수 없는 끝 역
    FILLED,

    // 주황 점에 흰 테와 주황 고리: 들를 역, 지금 고른 역
    ACCENT,

    // 회색 점선 고리: 들를 역을 비워 둔 칸
    EMPTY,
}

// 세로 레일과 점을 그린다. x는 레일 가운데, 행의 위쪽 절반·아래쪽 절반 레일은 이어질 때만 그린다.
// 행 높이를 정해 두지 않아도(큰 글꼴) 행 높이 전체를 따라 그려서 위아래 행과 끊기지 않는다
fun DrawScope.drawRailAndDot(
    colors: TeumtaColors,
    x: Dp,
    dot: StationDot,
    railAbove: Boolean,
    railBelow: Boolean,
) {
    val cx = x.toPx()
    val cy = size.height / 2
    val rail = colors.routeLine1Subtle
    val width = TeumtaDimens.StrokeThick.toPx()
    if (railAbove) drawLine(rail, Offset(cx, 0f), Offset(cx, cy), width)
    if (railBelow) drawLine(rail, Offset(cx, cy), Offset(cx, size.height), width)
    val center = Offset(cx, cy)
    when (dot) {
        StationDot.RING -> {
            drawCircle(colors.bgSurface, 6.dp.toPx(), center)
            drawCircle(colors.routeLine1, 5.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        }
        StationDot.FILLED -> drawCircle(colors.routeLine1, 6.dp.toPx(), center)
        StationDot.ACCENT -> {
            drawCircle(colors.accentDefault, 8.dp.toPx(), center)
            drawCircle(colors.bgSurface, 7.dp.toPx(), center)
            drawCircle(colors.accentDefault, 5.5.dp.toPx(), center)
        }
        StationDot.EMPTY -> {
            drawCircle(colors.bgSurface, 6.dp.toPx(), center)
            drawCircle(
                colors.textTertiary, 5.dp.toPx(), center,
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
            )
        }
    }
}

// 구역 사이 회색 띠 (원래 기획 sectionGap)
@Composable
fun SectionGap(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(TeumtaDimens.SizeSectionGap).background(TeumtaTheme.colors.bgCanvas))
}

// 둥근 칩. 고르면 남색 바탕(원래 기획 inverse)에 흰 글자. selected가 null이면 고르는 칩이 아니라 누르는 버튼이다
@Composable
fun TeumtaChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    height: Dp = TeumtaDimens.SizeChipLg,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val colors = TeumtaTheme.colors
    val on = selected == true
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.heightIn(min = height).padding(horizontal = if (height < TeumtaDimens.SizeChipLg) 14.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leadingIcon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textSecondary) }
            Text(
                text = label,
                style = TeumtaTheme.typography.bodyS,
                fontWeight = if (on || selected == null) FontWeight.SemiBold else FontWeight.Medium,
                color = if (on) colors.textOnInverse else colors.textPrimary,
                maxLines = 1,
            )
            trailingIcon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textOnInverse) }
        }
    }
    val border = if (on) null else BorderStroke(TeumtaDimens.StrokeHairline, colors.borderDefault)
    val bg = if (on) colors.bgInverse else colors.bgSurface
    if (selected == null) {
        Surface(onClick = onClick, modifier = modifier, shape = CircleShape, color = bg, border = border, content = content)
    } else {
        Surface(selected = selected, onClick = onClick, modifier = modifier, shape = CircleShape, color = bg, border = border, content = content)
    }
}

// 칩 줄 사이 간격
val ChipSpacing = 8.dp
