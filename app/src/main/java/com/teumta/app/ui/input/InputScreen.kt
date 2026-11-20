package com.teumta.app.ui.input

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teumta.app.R
import com.teumta.app.domain.ServiceTime
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.chooseCalculationDay
import com.teumta.app.domain.model.Station
import com.teumta.app.ui.formatServiceDay
import com.teumta.app.ui.info.AppInfo
import com.teumta.core.designsystem.component.TeumtaButton
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaShapes
import com.teumta.core.designsystem.theme.TeumtaTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ViewModel과 화면을 잇는다. 아래 InputScreen은 상태만 받아 그린다(state hoisting).
// 경로 상자의 칸을 누르면 그 칸의 역 고르기 화면을 이 자리에 띄운다. 고르면 입력 화면으로 돌아온다
@Composable
fun InputRoute(
    onCalculate: (RouteInput) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: InputViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // 어느 칸을 고르는 중인지는 화면만 알면 되는 값이라 ViewModel에 두지 않는다. 화면을 돌려도 남게 rememberSaveable
    var picking by rememberSaveable { mutableStateOf<StationRole?>(null) }

    val role = picking
    if (role != null) {
        StationPickerScreen(
            role = role,
            state = state,
            onPick = { station ->
                viewModel.onStationPick(role, station)
                picking = null
            },
            onSkipVia = {
                viewModel.onClearVia()
                picking = null
            },
            onBack = { picking = null },
        )
        return
    }
    InputScreen(
        state = state,
        onPickStation = { picking = it },
        onClearVia = viewModel::onClearVia,
        onSwap = viewModel::onSwap,
        onArriveByChange = viewModel::onArriveByChange,
        onStayChange = viewModel::onStayChange,
        onCalculate = { viewModel.onCalculate()?.let(onCalculate) },
        onSettingsClick = onSettingsClick,
        onCommute = viewModel::onCommute,
        onPolicyNoticeSeen = viewModel::onPolicyNoticeSeen,
    )
}

// 원래 기획 S-01: 흰 바탕에 경로 상자, 회색 띠 아래 도착 시각과 머무를 시간, 맨 아래 계산 버튼
@Composable
fun InputScreen(
    state: InputUiState,
    onPickStation: (StationRole) -> Unit,
    onClearVia: () -> Unit,
    onSwap: () -> Unit,
    onArriveByChange: (LocalTime) -> Unit,
    onStayChange: (Int) -> Unit,
    onCalculate: () -> Unit,
    onSettingsClick: () -> Unit,
    onCommute: (CommuteDirection) -> Unit,
    onPolicyNoticeSeen: () -> Unit,
) {
    // 대화상자를 열었는지는 화면만 알면 되는 값이라 ViewModel에 두지 않는다
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val selection = state.selection

    Scaffold(
        containerColor = TeumtaTheme.colors.bgSurface,
        bottomBar = { CalculateBar(state, onCalculate) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            HomeTopBar(onSettingsClick)
            CommuteChips(state.home, state.work, onCommute)
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().height(182.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TeumtaTheme.colors.actionPrimary)
                }
            } else {
                RouteBox(selection, onPickStation, onClearVia, onSwap)
            }
            Spacer(Modifier.height(20.dp))
            SectionGap()
            ArriveBySection(state.arriveBy, selection.to, onTimeClick = { showTimePicker = true })
            selection.via?.let { via -> StaySection(via, state.stayMin, onStayChange) }
            Spacer(Modifier.height(TeumtaDimens.Space24))
        }
    }

    if (state.showPolicyNotice) {
        PolicyNoticeDialog(onSeen = onPolicyNoticeSeen)
    }

    if (showTimePicker) {
        ArriveByDialog(
            initial = state.arriveBy,
            onConfirm = {
                onArriveByChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

// 왼쪽에 로고와 틈타, 오른쪽에 설정
@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    val colors = TeumtaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_logo_mark), contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(TeumtaDimens.Space8))
        Text(
            text = "틈타",
            modifier = Modifier.weight(1f).semantics { heading() },
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            color = colors.textPrimary,
        )
        IconButton(onClick = onSettingsClick) {
            Icon(TeumtaIcons.Sliders, contentDescription = "설정", tint = colors.textPrimary)
        }
    }
}

// 설정에서 자주 쓰는 두 역을 저장해 두었으면 한 번에 출발·도착을 채우는 버튼 두 개(가는 길, 오는 길).
// 어떤 길인지 짐작하지 않고 역 이름으로만 보여 준다(예: 동암 → 가산디지털단지). 둘 중 하나라도 없으면 칸만 띄운다
@Composable
private fun CommuteChips(home: Station?, work: Station?, onCommute: (CommuteDirection) -> Unit) {
    if (home == null || work == null) {
        Spacer(Modifier.height(TeumtaDimens.Space12))
        return
    }
    FlowRow(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
        verticalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        RouteChip(home, work) { onCommute(CommuteDirection.TO_WORK) }
        RouteChip(work, home) { onCommute(CommuteDirection.TO_HOME) }
    }
}

@Composable
private fun RouteChip(from: Station, to: Station, onClick: () -> Unit) {
    TeumtaChip(
        label = "${from.name} → ${to.name}",
        onClick = onClick,
        height = 36.dp,
        // 화면 읽기에서 화살표를 "오른쪽 화살표"로 읽지 않게
        modifier = Modifier.semantics { contentDescription = "${from.name}에서 ${to.name}까지" },
    )
}

// 경로 상자: 출발 · 들를 역(없어도 됨) · 도착 세 칸과 바꾸기 버튼. 칸을 누르면 그 칸의 역을 고른다
@Composable
private fun RouteBox(
    selection: RouteSelection,
    onPickStation: (StationRole) -> Unit,
    onClearVia: () -> Unit,
    onSwap: () -> Unit,
) {
    val colors = TeumtaTheme.colors
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .border(TeumtaDimens.StrokeHairline, colors.borderDefault, TeumtaShapes.Md),
    ) {
        Column {
            RouteField(
                label = "출발",
                value = selection.from?.name,
                dot = StationDot.RING,
                railAbove = false,
                onClick = { onPickStation(StationRole.FROM) },
                clickLabel = "출발역 고르기",
            )
            RouteDivider()
            val via = selection.via
            RouteField(
                label = "들를 역 · 선택",
                value = via?.name,
                placeholder = "없으면 바로 가요",
                dot = if (via != null) StationDot.ACCENT else StationDot.EMPTY,
                valueIsAccent = true,
                onClick = { onPickStation(StationRole.VIA) },
                clickLabel = "들를 역 고르기",
                trailing = if (via != null) {
                    { ClearViaButton(onClearVia) }
                } else {
                    null
                },
            )
            RouteDivider()
            RouteField(
                label = "도착",
                value = selection.to?.name,
                dot = StationDot.FILLED,
                railBelow = false,
                onClick = { onPickStation(StationRole.TO) },
                clickLabel = "도착역 고르기",
            )
        }
        // 가운데(들를 역 칸) 오른쪽에 둔다. 들를 역이 있으면 그 칸의 지우기 버튼 오른쪽이다
        IconButton(onClick = onSwap, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) {
            Icon(TeumtaIcons.Swap, contentDescription = "출발역과 도착역 바꾸기", tint = colors.textSecondary)
        }
    }
}

// 경로 상자의 한 칸. 왼쪽 48dp에 레일과 점, 오른쪽 56dp는 바꾸기 버튼 자리로 비워 둔다
@Composable
private fun RouteField(
    label: String,
    value: String?,
    dot: StationDot,
    onClick: () -> Unit,
    clickLabel: String,
    placeholder: String = "역 고르기",
    railAbove: Boolean = true,
    railBelow: Boolean = true,
    valueIsAccent: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = TeumtaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .drawBehind { drawRailAndDot(colors, x = 24.dp, dot = dot, railAbove = railAbove, railBelow = railBelow) }
            .padding(end = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 60.dp)
                .clickable(onClickLabel = clickLabel, onClick = onClick)
                .padding(start = 48.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Text(label, style = TeumtaTheme.typography.caption, color = colors.textTertiary)
            if (value != null) {
                Text(
                    text = value,
                    style = TeumtaTheme.typography.titleS,
                    color = if (valueIsAccent) colors.textAccent else colors.textPrimary,
                )
            } else {
                Text(placeholder, style = TeumtaTheme.typography.titleS, fontWeight = FontWeight.Medium, color = colors.textTertiary)
            }
        }
        trailing?.invoke()
    }
}

// 칸 사이 선. 레일이 끊기지 않게 레일만 이어 그리고, 선은 점 오른쪽부터 그린다
@Composable
private fun RouteDivider() {
    val colors = TeumtaTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(TeumtaDimens.StrokeHairline)
            .drawBehind {
                val x = 24.dp.toPx()
                drawLine(colors.routeLine1Subtle, Offset(x, 0f), Offset(x, size.height), TeumtaDimens.StrokeThick.toPx())
            }
            .padding(start = 48.dp, end = 56.dp)
            .background(colors.borderDefault),
    )
}

@Composable
private fun ClearViaButton(onClick: () -> Unit) {
    val colors = TeumtaTheme.colors
    // 보이는 원은 36dp, 누르는 곳은 48dp
    Box(
        modifier = Modifier.size(TeumtaDimens.SizeTouchMin).clickable(onClickLabel = "들를 역 지우기", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(36.dp).background(colors.bgField, CircleShape), contentAlignment = Alignment.Center) {
            Icon(TeumtaIcons.Close, contentDescription = "들를 역 지우기", modifier = Modifier.size(16.dp), tint = colors.textSecondary)
        }
    }
}

// 구역 머리: 왼쪽 제목, 오른쪽 지금 고른 값 요약
@Composable
private fun SectionHeader(title: String, summary: String) {
    val colors = TeumtaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = TeumtaTheme.typography.titleS,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Text(summary, style = TeumtaTheme.typography.label, fontWeight = FontWeight.Normal, color = colors.textSecondary)
    }
}

// 도착 시각. 칩을 누르면 시각을 고른다(시·분 바퀴는 다음 작업에서 바꾼다)
@Composable
private fun ArriveBySection(arriveBy: LocalTime, to: Station?, onTimeClick: () -> Unit) {
    val time = arriveBy.format(TIME)
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
    ) {
        SectionHeader("도착 시각", if (to != null) "${time}까지 ${to.name} 도착" else "${time}까지 도착")
        TeumtaChip(
            label = time,
            onClick = onTimeClick,
            selected = true,
            trailingIcon = TeumtaIcons.ChevronDown,
            modifier = Modifier.semantics { contentDescription = "도착 시각 $time, 바꾸기" },
        )
    }
}

// 머무를 시간. 들를 역이 있을 때만 보인다
@Composable
private fun StaySection(via: Station, stayMin: Int, onStayChange: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
    ) {
        SectionHeader("머무를 시간", "${via.name}에서 ${stayLabel(stayMin)}")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
            verticalArrangement = Arrangement.spacedBy(ChipSpacing),
        ) {
            InputViewModel.STAY_OPTIONS.forEach { minutes ->
                TeumtaChip(label = stayLabel(minutes), onClick = { onStayChange(minutes) }, selected = stayMin == minutes)
            }
        }
    }
}

// 맨 아래: 어느 날 시간표로 계산하는지 한 줄과 계산 버튼
@Composable
private fun CalculateBar(state: InputUiState, onCalculate: () -> Unit) {
    val colors = TeumtaTheme.colors
    // 도착 시각이 이미 지났으면 다음 운행일로 계산한다(결과 화면과 같은 규칙)
    val dayNote = remember(state.arriveBy) {
        val day = chooseCalculationDay(ServiceTime.now(), ServiceTime.toServiceSeconds(state.arriveBy))
        "${formatServiceDay(day.date, day.dayType)} · 실시간 지연 미반영"
    }
    Column(Modifier.background(colors.bgSurface).navigationBarsPadding()) {
        HorizontalDivider(thickness = TeumtaDimens.StrokeHairline, color = colors.borderDefault)
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = dayNote,
                modifier = Modifier.fillMaxWidth(),
                style = TeumtaTheme.typography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
            TeumtaButton(
                text = "출발 시각 보기",
                onClick = onCalculate,
                enabled = state.canCalculate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// 개인정보처리방침이 바뀌었다는 안내. 0.2.0 방침에 "앱에서도 알려요"라고 약속한 것을 지킨다
@Composable
private fun PolicyNoticeDialog(onSeen: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onSeen,
        title = { Text("개인정보처리방침이 바뀌어요") },
        text = {
            Text(
                "${AppInfo.PRIVACY_EFFECTIVE_DATE}부터 시행해요. 회원가입·로그인이 생겨서, 가입하면 이메일과 집역·회사역을 " +
                    "틈타 서버(호주)에 저장해요. 시간표도 서버에서 받아요. 로그인하지 않으면 계정 정보는 보내지 않아요.",
            )
        },
        confirmButton = { TextButton(onClick = onSeen) { Text("확인") } },
        dismissButton = {
            TextButton(onClick = {
                onSeen()
                // 브라우저가 없으면 openUri가 예외를 던져 앱이 꺼진다(앱 정보 화면과 같은 처리)
                runCatching { uriHandler.openUri(AppInfo.PRIVACY_URL) }
                    .onFailure { Toast.makeText(context, "열 수 있는 앱이 없어요: ${AppInfo.PRIVACY_URL}", Toast.LENGTH_LONG).show() }
            }) { Text("방침 보기") }
        },
    )
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

private fun stayLabel(minutes: Int): String = if (minutes % 60 == 0) "${minutes / 60}시간" else "${minutes}분"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArriveByDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("언제까지 도착해야 하나요?") },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun InputScreenPreview() {
    // 한 줄로 이어진 다섯 역 (앞 역을 parent로)
    val stations = listOf("서울역", "남영", "용산", "노량진", "대방").mapIndexed { i, name ->
        Station("S$i", name, i + 1, parentId = if (i == 0) null else "S${i - 1}")
    }
    val tree = StationTree(stations)
    TeumtaTheme {
        InputScreen(
            state = InputUiState(
                isLoading = false,
                sections = stationSections(tree),
                selection = RouteSelection(tree, from = stations[0], to = stations[4], via = stations[2]),
                arriveBy = LocalTime.of(21, 0),
                home = stations[4],
                work = stations[0],
            ),
            onPickStation = {},
            onClearVia = {},
            onSwap = {},
            onArriveByChange = {},
            onStayChange = {},
            onCalculate = {},
            onSettingsClick = {},
            onCommute = {},
            onPolicyNoticeSeen = {},
        )
    }
}
