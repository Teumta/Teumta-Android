package com.teumta.app.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teumta.app.domain.model.RealtimeArrival
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

// 동선의 "OO 승차" 줄 밑에 붙는 "지금 오는 열차" 상자. 그 역에서 그 열차가 갈 쪽으로 오는 열차를 두 편까지.
// 줄마다 행선지(+급행), 어디쯤인지, 몇 분 전 위치인지. 설명은 "동선" 옆 물음표 안내에 둔다
@Composable
internal fun RealtimeBox(trains: RealtimeTrains, modifier: Modifier = Modifier) {
    val colors = TeumtaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgCanvas, RoundedCornerShape(TeumtaDimens.Space8))
            .padding(horizontal = TeumtaDimens.Space12, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8),
    ) {
        Text("지금 오는 열차", style = TeumtaTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = colors.textTertiary)
        if (trains.arrivals.isEmpty()) {
            Text("지금은 보이는 열차가 없어요", style = TeumtaTheme.typography.caption, color = colors.textSecondary)
        }
        trains.arrivals.forEachIndexed { i, arrival ->
            if (i > 0) HorizontalDivider(thickness = TeumtaDimens.StrokeHairline, color = colors.borderDefault)
            RealtimeTrainRow(arrival, trains)
        }
    }
}

@Composable
private fun RealtimeTrainRow(arrival: RealtimeArrival, trains: RealtimeTrains) {
    val colors = TeumtaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${arrival.terminal}행", style = TeumtaTheme.typography.bodyS, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            if (arrival.express) ExpressBadge()
            Text(
                text = realtimeAgeText(arrival.receivedAt, trains.checkedAt),
                modifier = Modifier.weight(1f),
                style = TeumtaTheme.typography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.End,
            )
        }
        Text(realtimePositionText(arrival), style = TeumtaTheme.typography.caption, color = colors.textSecondary)
        if (arrival.turnsBack) {
            Text("도착한 뒤 돌아 나가요", style = TeumtaTheme.typography.caption, color = colors.textTertiary)
        }
    }
}

// 급행 표시. 글자색만 바꾸면 색을 못 보는 사람에게 구분이 안 돼서 "급행" 글자를 쓴다
@Composable
private fun ExpressBadge() {
    val colors = TeumtaTheme.colors
    Text(
        text = "급행",
        modifier = Modifier
            .background(colors.bgAccentSubtle, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        style = TeumtaTheme.typography.caption,
        fontWeight = FontWeight.SemiBold,
        color = colors.textAccent,
    )
}

// "동선" 옆 물음표 안내. 실시간을 보여 줄 역이 있을 때만 버튼이 보인다
// 한 줄에 한 가지만, 보통 글꼴에서 한 줄에 들어가게 짧게 쓴다
internal val REALTIME_HELP = listOf(
    "타는 역마다 지금 오는 열차를 보여 줘요.",
    "당역은 타는 역, 전역은 바로 전 역이에요.",
    "서울시 실시간 위치라 몇 분 늦을 수 있어요.",
    "1분마다 다시 받고, 새로고침을 누르면 바로 받아요.",
    "출발 시각 계산에는 실시간을 넣지 않아요.",
    "오늘, 1시간 안에 타는 역에만 보여 줘요.",
    "광명은 금천구청까지 온 셔틀만 보여요.",
    "서동탄은 오래 서 있는 열차가 안 보일 수 있어요.",
    "타는 역과 내리는 역을 서버에 보내요.",
    "서버는 그 역을 저장하지 않아요.",
)

// 동선 머리글의 새로고침. 받는 중이면 버튼 대신 도는 표시를 둔다(여러 번 눌러도 한 번만 받는다)
internal data class RealtimeControls(val refreshing: Boolean, val onRefresh: () -> Unit)

@Composable
internal fun RealtimeRefreshButton(controls: RealtimeControls) {
    val colors = TeumtaTheme.colors
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (controls.refreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).semantics { contentDescription = "실시간 열차를 받는 중" },
                color = colors.textTertiary,
                strokeWidth = 2.dp,
            )
        } else {
            IconButton(onClick = controls.onRefresh) {
                Icon(TeumtaIcons.Refresh, contentDescription = "실시간 열차 새로고침", tint = colors.textTertiary)
            }
        }
    }
}
