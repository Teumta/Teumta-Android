package com.teumta.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

// 시각은 서비스일 기준 분. 자정을 넘으면 1440 이상
@Immutable
data class TodaySummaryState(
    val stationName: String,
    val tags: List<String>,
    val startMin: Int,
    val goalMin: Int,
    val arriveMin: Int,
    val lastTrainMin: Int,
    val lastTrainIsExpress: Boolean,
) {
    val slackMin: Int get() = lastTrainMin - arriveMin
}

fun Int.toClock(): String = "%02d:%02d".format((this / 60) % 24, this % 60)
fun Int.toSpokenClock(): String = "${(this / 60) % 24}시 ${this % 60}분"
fun Int.toDurationText(): String {
    val h = this / 60
    val m = this % 60
    return when {
        h == 0 -> "${m}분"
        m == 0 -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}

// Figma: TodaySummary. 카드 없이 화면 바탕 위에 글자와 얇은 막대만
@Composable
fun TodaySummary(state: TodaySummaryState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    val train = if (state.lastTrainIsExpress) "급행" else "일반"
    val spoken = "${state.stationName}, 여유 ${state.slackMin.toDurationText()}. " +
        "${state.lastTrainMin.toSpokenClock()} ${train} 열차"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = TeumtaDimens.Space20, vertical = TeumtaDimens.Space16)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                this.onClick(label = "자세히 보기") { onClick(); true }
            },
    ) {
        Text("오늘 가장 오래 머물 수 있는 역", style = type.label, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8)) {
            Text(state.stationName, style = type.titleL, color = colors.textPrimary)
            Text(state.tags.joinToString(" · "), style = type.caption, color = colors.textTertiary)
        }
        // 포인트 색: 이 화면의 답인 여유 시간
        Text(state.slackMin.toDurationText(), style = type.headlineL, color = colors.textAccent)
        Spacer(Modifier.height(TeumtaDimens.Space12))
        TimeRangeBar(
            startMin = state.startMin,
            endMin = state.goalMin,
            fromMin = state.arriveMin,
            toMin = state.lastTrainMin,
            track = colors.bgField,
            fill = colors.accentDefault,
        )
        Spacer(Modifier.height(TeumtaDimens.Space8))
        Row(Modifier.fillMaxWidth()) {
            Text("${state.arriveMin.toClock()} 도착", style = type.caption, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Text("${state.lastTrainMin.toClock()} $train 타기 · 목표 ${state.goalMin.toClock()}", style = type.caption, color = colors.textSecondary)
        }
    }
}

@Composable
fun TimeRangeBar(
    startMin: Int,
    endMin: Int,
    fromMin: Int,
    toMin: Int,
    track: Color,
    fill: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxWidth().height(TeumtaDimens.SizeProgress)) {
        val total = (endMin - startMin).coerceAtLeast(1).toFloat()
        val x0 = size.width * ((fromMin - startMin) / total).coerceIn(0f, 1f)
        val x1 = size.width * ((toMin - startMin) / total).coerceIn(0f, 1f)
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = track, cornerRadius = radius)
        drawRoundRect(
            color = fill,
            topLeft = Offset(x0, 0f),
            size = Size((x1 - x0).coerceAtLeast(0f), size.height),
            cornerRadius = radius,
        )
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun TodaySummaryPreview() {
    TeumtaTheme {
        TodaySummary(
            state = TodaySummaryState("구로", listOf("급행", "환승"), 18 * 60 + 30, 21 * 60 + 30, 18 * 60 + 34, 21 * 60 + 6, true),
            onClick = {},
        )
    }
}
