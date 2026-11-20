package com.teumta.app.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teumta.app.R
import com.teumta.app.domain.DeparturePlan
import com.teumta.app.domain.DepartureResult
import com.teumta.app.domain.FailReason
import com.teumta.app.ui.formatClock
import com.teumta.app.ui.formatDuration
import com.teumta.app.ui.formatServiceDay
import com.teumta.app.ui.formatTimetableNote
import com.teumta.app.ui.input.SectionGap
import com.teumta.app.ui.input.StayChoice
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaColors
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

// 결과 화면에서 입력 화면으로 돌아가며 할 일. 입력 화면의 값을 바꾸는 일이라 NavHost가 입력 화면 ViewModel에 넘긴다
data class ResultActions(
    val onBack: () -> Unit,
    // 머무를 시간을 이 값(분)으로 줄이고 입력 화면으로
    val onReduceStay: (Int) -> Unit,
    // 들를 역을 지우고 바로 가기로 다시 계산
    val onGoDirect: () -> Unit,
    // 입력 화면으로 돌아가 도착 시각 시트를 연다
    val onChangeArrive: () -> Unit,
    // 입력 화면으로 돌아가 들를 역 고르기를 연다
    val onAddVia: () -> Unit,
)

@Composable
fun ResultRoute(
    actions: ResultActions,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ResultScreen(state = state, actions = actions, onNextDay = viewModel::showNextDay)
}

// 원래 기획 S-04: 위에 경로, 큰 주황 답 시각과 사실 세 칸, 회색 띠 아래 색 레일 동선, 맨 아래 시간표 기준
@Composable
fun ResultScreen(state: ResultUiState, actions: ResultActions, onNextDay: () -> Unit) {
    val colors = TeumtaTheme.colors
    Scaffold(containerColor = colors.bgSurface) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                ResultUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.actionPrimary)
                is ResultUiState.Ready -> Column(Modifier.fillMaxSize()) {
                    ResultTopBar(state, actions.onBack)
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (val result = state.result) {
                            is DepartureResult.Success -> SuccessContent(state, result, actions.onAddVia)
                            is DepartureResult.Failure -> FailureContent(state, result.reason, actions, onNextDay)
                        }
                        CalculationBasis(state)
                    }
                }
            }
        }
    }
}

// 맨 아래: 무엇을 기준으로 계산했는지. 한 줄에 몰아 쓰지 않고 이름·값으로 나눠 적는다
@Composable
private fun CalculationBasis(state: ResultUiState.Ready) {
    val colors = TeumtaTheme.colors
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = TeumtaDimens.Space24, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HorizontalDivider(Modifier.padding(bottom = 10.dp), TeumtaDimens.StrokeHairline, colors.borderDefault)
        BasisRow("계산한 날", formatServiceDay(state.day.date, state.day.dayType))
        BasisRow("시간표", formatTimetableNote(state.timetableSource, state.baseDate))
        BasisRow("실시간 지연", "반영하지 않아요")
    }
}

@Composable
private fun BasisRow(label: String, value: String) {
    val colors = TeumtaTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12)) {
        Text(label, modifier = Modifier.width(BASIS_LABEL_WIDTH), style = TeumtaTheme.typography.caption, color = colors.textTertiary)
        Text(value, modifier = Modifier.weight(1f), style = TeumtaTheme.typography.caption, color = colors.textSecondary)
    }
}

// 이름 칸 폭. 큰 글꼴에서도 "실시간 지연"이 꺾이지 않게 글꼴 크기를 따라 넓힌다
private val BASIS_LABEL_WIDTH @Composable get() = with(LocalDensity.current) { 12.sp.toDp() * 6.5f }

// 뒤로 가기와 "출발 → 도착", 그 밑에 들르는지 한 줄
@Composable
private fun ResultTopBar(state: ResultUiState.Ready, onBack: () -> Unit) {
    val colors = TeumtaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(TeumtaIcons.Back, contentDescription = "뒤로", tint = colors.textPrimary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "${state.fromName} → ${state.toName}",
                modifier = Modifier.semantics { heading() },
                style = TeumtaTheme.typography.title,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Text(
                text = state.viaName?.let { "${it}에서 ${formatDuration(state.stayMin * 60)} 머무르기" } ?: "들르지 않고 바로 가기",
                style = TeumtaTheme.typography.caption,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun SuccessContent(state: ResultUiState.Ready, result: DepartureResult.Success, onAddVia: () -> Unit) {
    val colors = TeumtaTheme.colors
    val plan = result.plan

    // 답 블록: 어디서 늦어도 몇 시, 어느 열차
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = TeumtaDimens.Space16)) {
        Text("${state.fromName}에서 늦어도", style = TeumtaTheme.typography.bodyS, fontWeight = FontWeight.Medium, color = colors.textSecondary)
        Text(
            text = formatClock(plan.departSec),
            modifier = Modifier.padding(top = TeumtaDimens.Space4),
            style = TeumtaTheme.typography.display,
            lineHeight = 62.sp,
            color = colors.textAccent,
        )
        Text(
            text = "${plan.first.rides.first().train.headsign}행 열차",
            modifier = Modifier.padding(top = 6.dp),
            style = TeumtaTheme.typography.bodyStrong,
            color = colors.textPrimary,
        )
        state.day.nowSec?.let { now ->
            Text("지금부터 ${formatDuration(plan.departSec - now)} 뒤", style = TeumtaTheme.typography.caption, color = colors.textTertiary)
        }
        FactsRow(buildFacts(plan, state.toName, state.arriveBySec))
        result.alternative?.let { alt ->
            Row(
                modifier = Modifier.padding(top = TeumtaDimens.Space16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(TeumtaIcons.Clock, contentDescription = null, modifier = Modifier.size(TeumtaDimens.SizeIcon16), tint = colors.textTertiary)
                Text(alternativeText(alt), style = TeumtaTheme.typography.label, fontWeight = FontWeight.Normal, color = colors.textSecondary)
            }
        }
    }
    SectionGap(Modifier.padding(top = TeumtaDimens.Space24))

    // 동선
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "동선",
            modifier = Modifier.weight(1f).semantics { heading() },
            style = TeumtaTheme.typography.titleS,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Text("${formatClock(plan.departSec)} – ${formatClock(plan.arriveSec)}", style = TeumtaTheme.typography.caption, color = colors.textTertiary)
    }
    Timeline(buildTimeline(plan, state::nameOf, state.arriveBySec), Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))

    // 바로 가는 답이면 들렀다 가는 길도 있다고 알려 준다
    if (plan.second == null) {
        SectionGap(Modifier.padding(top = TeumtaDimens.Space16))
        OptionRow(
            icon = TeumtaIcons.Coffee,
            title = "중간에 들렀다 가기",
            detail = "들를 역을 고르면 머무를 수 있는 시간을 알려 줘요",
            onClick = onAddVia,
        )
    }
}

// 사실 세 칸: 머무는(걸리는) 시간, 도착, 목표. 칸 사이에 세로 선
@Composable
private fun FactsRow(facts: List<Fact>) {
    val colors = TeumtaTheme.colors
    Column(Modifier.padding(top = 20.dp)) {
        HorizontalDivider(thickness = TeumtaDimens.StrokeHairline, color = colors.borderDefault)
        Row(Modifier.padding(top = 14.dp).height(IntrinsicSize.Min)) {
            facts.forEachIndexed { i, fact ->
                if (i > 0) {
                    Box(Modifier.width(TeumtaDimens.StrokeHairline).fillMaxHeight().drawBehind { drawRect(colors.borderDefault) })
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = if (i > 0) 12.dp else 0.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space4),
                ) {
                    Text(fact.label, style = TeumtaTheme.typography.caption, color = colors.textTertiary)
                    Text(fact.value, style = TeumtaTheme.typography.stat, color = colors.textPrimary)
                }
            }
        }
    }
}

// 동선: 왼쪽 시각, 가운데 레일과 점, 오른쪽 역과 설명. 레일 색은 그 구간의 색(열차 파랑, 머무름 주황, 갈아타기 회색)
@Composable
private fun Timeline(items: List<TimelineItem>, modifier: Modifier = Modifier) {
    // 시각 칸 폭과 점 높이는 글꼴 크기를 따른다. 큰 글꼴에서 "19:29"가 두 줄로 꺾이거나 점이 역 이름보다 위에 뜨지 않게
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val timeStyle = TeumtaTheme.typography.bodyS.copy(fontWeight = FontWeight.Bold)
    val bodyLineHeight = TeumtaTheme.typography.body.lineHeight
    val layout = remember(density, timeStyle, bodyLineHeight) {
        with(density) {
            TimelineLayout(
                timeWidth = maxOf(48.dp, measurer.measure("00:00", timeStyle).size.width.toDp() + 8.dp),
                dotCenterY = bodyLineHeight.toDp() / 2,
            )
        }
    }
    Column(modifier) {
        items.forEachIndexed { i, item ->
            when (item) {
                is TimelineItem.Stop -> StopRow(
                    item,
                    railAbove = (items.getOrNull(i - 1) as? TimelineItem.Segment)?.kind,
                    railBelow = (items.getOrNull(i + 1) as? TimelineItem.Segment)?.kind,
                    layout = layout,
                    timeStyle = timeStyle,
                )
                is TimelineItem.Segment -> SegmentRow(item, layout)
            }
        }
    }
}

// 시각 칸 폭과, 줄 위에서 점 한가운데까지의 높이(역 이름 첫 줄의 가운데)
private data class TimelineLayout(val timeWidth: Dp, val dotCenterY: Dp)

private val RAIL_COLUMN = 28.dp

@Composable
private fun StopRow(stop: TimelineItem.Stop, railAbove: SegmentKind?, railBelow: SegmentKind?, layout: TimelineLayout, timeStyle: TextStyle) {
    val colors = TeumtaTheme.colors
    // 큰 글꼴로 역 이름이 길어져도 레일이 끊기지 않게 행 높이를 내용에 맞추고 레일 칸이 그 높이를 채운다
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 48.dp)) {
        Text(
            text = formatClock(stop.timeSec),
            modifier = Modifier.width(layout.timeWidth).padding(top = 2.dp),
            style = timeStyle,
            color = colors.textPrimary,
            maxLines = 1,
        )
        Box(
            Modifier
                .width(RAIL_COLUMN)
                .fillMaxHeight()
                .drawBehind {
                    val cx = size.width / 2
                    val cy = layout.dotCenterY.toPx()
                    railAbove?.let { drawRail(colors, it, Offset(cx, 0f), Offset(cx, cy)) }
                    railBelow?.let { drawRail(colors, it, Offset(cx, cy), Offset(cx, size.height)) }
                    val center = Offset(cx, cy)
                    when (stop.dot) {
                        StopDot.RIDE, StopDot.VIA -> {
                            val ring = if (stop.dot == StopDot.VIA) colors.accentDefault else colors.routeLine1
                            drawCircle(ring, 7.dp.toPx(), center)
                            drawCircle(colors.bgSurface, 4.dp.toPx(), center)
                        }
                        StopDot.END -> drawCircle(colors.textPrimary, 6.dp.toPx(), center)
                    }
                },
        )
        Column(Modifier.weight(1f).padding(start = 8.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stop.title, style = TeumtaTheme.typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            stop.detail?.let { Text(it, style = TeumtaTheme.typography.caption, color = colors.textTertiary) }
        }
    }
}

@Composable
private fun SegmentRow(segment: TimelineItem.Segment, layout: TimelineLayout) {
    val colors = TeumtaTheme.colors
    val stay = segment.kind == SegmentKind.STAY
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(layout.timeWidth))
        Box(
            Modifier
                .width(RAIL_COLUMN)
                .fillMaxHeight()
                .drawBehind {
                    val cx = size.width / 2
                    drawRail(colors, segment.kind, Offset(cx, 0f), Offset(cx, size.height))
                },
        )
        Text(
            text = segment.label,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
            style = TeumtaTheme.typography.caption,
            fontWeight = if (stay) FontWeight.SemiBold else FontWeight.Medium,
            color = if (stay) colors.textAccent else colors.textTertiary,
        )
    }
}

// 레일 한 토막. 갈아타기는 열차를 타고 있지 않은 시간이라 점선으로 그린다
private fun DrawScope.drawRail(colors: TeumtaColors, kind: SegmentKind, start: Offset, end: Offset) {
    val dash = if (kind == SegmentKind.TRANSFER) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null
    drawLine(railColor(colors, kind), start, end, TeumtaDimens.StrokeRoute.toPx(), pathEffect = dash)
}

private fun railColor(colors: TeumtaColors, kind: SegmentKind): Color = when (kind) {
    SegmentKind.TRAIN -> colors.routeLine1
    SegmentKind.STAY -> colors.accentDefault
    SegmentKind.TRANSFER -> colors.textTertiary
}

// 계산할 수 없을 때: 왜 안 되는지 한두 줄, 회색 띠 아래 "대신 이렇게 해 보세요"
@Composable
private fun FailureContent(state: ResultUiState.Ready, reason: FailReason, actions: ResultActions, onNextDay: () -> Unit) {
    val colors = TeumtaTheme.colors
    val arrive = formatClock(state.arriveBySec)
    val stay = formatDuration(state.stayMin * 60)
    val (title, body) = when (reason) {
        FailReason.STAY_TOO_LONG -> "오늘은 들르기 어려워요" to
            "${state.viaName}에서 $stay 머무르면 ${arrive}까지 ${state.toName}에 도착할 수 없어요."
        FailReason.ALREADY_DEPARTED -> "탈 수 있는 열차가 이미 떠났어요" to
            "${arrive}까지 ${state.toName}에 도착하는 열차는 모두 떠났어요."
        FailReason.NO_TRAIN_BEFORE_ARRIVAL -> "그 시각까지 도착하는 열차가 없어요" to
            "${arrive} 전에 ${state.toName}에 닿는 열차가 없어요. 첫차보다 이른 시각이에요."
    }
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8)) {
        Text(title, modifier = Modifier.semantics { heading() }, style = TeumtaTheme.typography.headline, color = colors.textPrimary)
        Text(body, style = TeumtaTheme.typography.bodyS, color = colors.textSecondary)
    }

    // 머무를 시간이 모자라면 얼마까지 되는지와 고른 시간을 나란히
    val maxStaySec = state.maxStaySec
    if (reason == FailReason.STAY_TOO_LONG && maxStaySec != null) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            CompareRow("머무를 수 있는 시간", "최대 ${formatDuration(maxStaySec)}", colors.textPrimary)
            HorizontalDivider(thickness = TeumtaDimens.StrokeHairline, color = colors.borderDefault)
            CompareRow("고른 시간", stay, colors.textDanger)
        }
    }

    SectionGap(Modifier.padding(top = 20.dp))
    Text(
        text = "대신 이렇게 해 보세요",
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp).semantics { heading() },
        style = TeumtaTheme.typography.titleS,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
    )
    val options = buildList<@Composable () -> Unit> {
        if (reason == FailReason.STAY_TOO_LONG && maxStaySec != null) {
            // 고를 수 있는 값(5분 단위)으로 내림
            val reduced = maxStaySec / 60 / StayChoice.STEP_MIN * StayChoice.STEP_MIN
            add {
                OptionRow(
                    TeumtaIcons.Clock,
                    "머무를 시간 줄이기",
                    "${formatDuration(reduced * 60)}으로 바꿔요 (최대 ${formatDuration(maxStaySec)})",
                ) { actions.onReduceStay(reduced) }
            }
        }
        state.directPlan?.let { direct ->
            add {
                OptionRow(
                    ImageVector.vectorResource(R.drawable.ic_arrow_right),
                    "들르지 않고 바로 가기",
                    "${formatClock(direct.departSec)} ${state.fromName} 출발 → ${formatClock(direct.arriveSec)} ${state.toName} 도착",
                    actions.onGoDirect,
                )
            }
        }
        if (reason == FailReason.ALREADY_DEPARTED) {
            add { OptionRow(TeumtaIcons.Train, "다음 운행일로 보기", "다음 운행일 같은 시각으로 계산해요", onNextDay) }
        }
        add { OptionRow(TeumtaIcons.Flag, "도착 시각 늦추기", "지금은 ${arrive}까지예요", actions.onChangeArrive) }
    }
    options.forEachIndexed { i, option ->
        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 20.dp), TeumtaDimens.StrokeHairline, colors.borderDefault)
        option()
    }
}

@Composable
private fun CompareRow(label: String, value: String, valueColor: Color) {
    val colors = TeumtaTheme.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = TeumtaTheme.typography.bodyS, color = colors.textSecondary)
        Text(value, style = TeumtaTheme.typography.numberL, color = valueColor)
    }
}

// 누르면 무언가 하는 줄: 왼쪽 아이콘, 제목과 설명, 오른쪽 화살표
@Composable
private fun OptionRow(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    val colors = TeumtaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(TeumtaDimens.SizeIcon20), tint = colors.textSecondary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = TeumtaTheme.typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(detail, style = TeumtaTheme.typography.caption, color = colors.textTertiary)
        }
        Icon(TeumtaIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(TeumtaDimens.SizeIcon20), tint = colors.textTertiary)
    }
}
