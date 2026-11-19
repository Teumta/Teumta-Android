package com.teumta.app.ui.input

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station
import com.teumta.app.ui.info.AppInfo
import com.teumta.core.designsystem.theme.TeumtaTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ViewModel과 화면을 잇는다. 아래 InputScreen은 상태만 받아 그린다(state hoisting)
@Composable
fun InputRoute(
    onCalculate: (RouteInput) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: InputViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InputScreen(
        state = state,
        onStationTap = viewModel::onStationTap,
        onArriveByChange = viewModel::onArriveByChange,
        onStayChange = viewModel::onStayChange,
        onCalculate = { viewModel.onCalculate()?.let(onCalculate) },
        onSettingsClick = onSettingsClick,
        onCommute = viewModel::onCommute,
        onPolicyNoticeSeen = viewModel::onPolicyNoticeSeen,
    )
}

@Composable
fun InputScreen(
    state: InputUiState,
    onStationTap: (Station) -> Unit,
    onArriveByChange: (LocalTime) -> Unit,
    onStayChange: (Int) -> Unit,
    onCalculate: () -> Unit,
    onSettingsClick: () -> Unit,
    onCommute: (CommuteDirection) -> Unit,
    onPolicyNoticeSeen: () -> Unit,
) {
    // 대화상자를 열었는지는 화면만 알면 되는 값이라 ViewModel에 두지 않는다
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            InputBottomBar(
                state = state,
                onTimeClick = { showTimePicker = true },
                onStayChange = onStayChange,
                onCalculate = onCalculate,
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("1호선 102개 역 · 급행 포함", style = MaterialTheme.typography.labelMedium)
                    Text(guideText(state.selection), style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = onSettingsClick) { Text("설정") }
            }
            CommuteButtons(state.home, state.work, onCommute)
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                StationList(state, onStationTap)
            }
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

private fun guideText(selection: RouteSelection): String = when (selection.step) {
    SelectStep.FROM -> "출발역을 눌러 주세요"
    SelectStep.TO -> "도착역을 눌러 주세요"
    SelectStep.VIA -> if (selection.hasViaCandidate) "들를 역을 눌러 주세요" else "사이에 들를 역이 없어요. 도착역을 다시 골라 주세요"
    SelectStep.DONE -> "도착 시각과 머무를 시간을 정해 주세요"
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

// 집역·회사역을 저장해 두었으면 한 번에 출발·도착을 채우는 버튼. 둘 중 하나라도 없으면 그리지 않는다
@Composable
private fun CommuteButtons(home: Station?, work: Station?, onCommute: (CommuteDirection) -> Unit) {
    if (home == null || work == null) return
    FlowRow(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(onClick = { onCommute(CommuteDirection.TO_WORK) }, label = { Text("출근길 ${home.name} → ${work.name}") })
        AssistChip(onClick = { onCommute(CommuteDirection.TO_HOME) }, label = { Text("퇴근길 ${work.name} → ${home.name}") })
    }
}

// 출발·도착을 고를 때는 노선도 전체(갈림길에서 끊은 토막들)를, 둘 다 고른 뒤에는 두 역을 잇는 길만 보여 준다.
// 길만 남기면 1호선이 갈라져 있어도 경유역 후보가 한 줄로 이어져 보인다
@Composable
private fun StationList(state: InputUiState, onStationTap: (Station) -> Unit) {
    val path = state.selection.path
    val sections = if (path != null) listOf(StationSection(branchFrom = null, stations = path)) else state.sections
    val listState = rememberLazyListState()
    // 노선도 전체와 길을 오갈 때 목록 맨 위부터 보여 준다
    LaunchedEffect(path != null) { listState.scrollToItem(0) }

    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        sections.forEach { section ->
            item(key = "title-${section.stations.first().id}") { SectionTitle(section.title) }
            itemsIndexed(section.stations, key = { _, s -> s.id }) { index, station ->
                StationRow(
                    station = station,
                    selection = state.selection,
                    isFirst = index == 0,
                    isLast = index == section.stations.lastIndex,
                    onPath = path != null,
                    onClick = { onStationTap(station) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        // 화면 읽기(TalkBack)에서 제목으로 건너뛸 수 있게
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

// 노선도 한 줄: 왼쪽에 선과 점, 가운데 역 이름, 오른쪽에 출발·경유·도착 표시
@Composable
private fun StationRow(
    station: Station,
    selection: RouteSelection,
    isFirst: Boolean,
    isLast: Boolean,
    // 출발역~도착역 길을 보여 주는 중이면 선을 진하게
    onPath: Boolean,
    onClick: () -> Unit,
) {
    val role = selection.roleOf(station)
    val enabled = selection.isEnabled(station)
    val colors = MaterialTheme.colorScheme
    val lineColor = if (onPath) colors.primary else colors.outlineVariant
    val dotColor = when {
        role != null -> colors.primary
        enabled -> colors.outline
        else -> colors.outlineVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.width(24.dp).fillMaxHeight()) {
            val x = size.width / 2
            val y = size.height / 2
            val stroke = 4.dp.toPx()
            if (!isFirst) drawLine(lineColor, Offset(x, 0f), Offset(x, y), stroke)
            if (!isLast) drawLine(lineColor, Offset(x, y), Offset(x, size.height), stroke)
            drawCircle(dotColor, radius = (if (role != null) 9.dp else 5.dp).toPx(), center = Offset(x, y))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = station.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (role != null) FontWeight.Bold else null,
            color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
        )
        if (role != null) RoleBadge(role)
    }
}

@Composable
private fun RoleBadge(role: StationRole) {
    val label = when (role) {
        StationRole.FROM -> "출발"
        StationRole.VIA -> "경유"
        StationRole.TO -> "도착"
    }
    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun InputBottomBar(
    state: InputUiState,
    onTimeClick: () -> Unit,
    onStayChange: (Int) -> Unit,
    onCalculate: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onTimeClick) {
                    Text("${state.arriveBy.format(TIME)}까지 도착", maxLines = 1)
                }
                InputViewModel.STAY_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = state.stayMin == minutes,
                        onClick = { onStayChange(minutes) },
                        // 글자가 세로로 쪼개지지 않게 한 줄로만
                        label = { Text(stayLabel(minutes), maxLines = 1) },
                    )
                }
            }
            Button(onClick = onCalculate, enabled = state.canCalculate, modifier = Modifier.fillMaxWidth()) {
                Text("늦어도 몇 시 열차?")
            }
        }
    }
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
            ),
            onStationTap = {},
            onArriveByChange = {},
            onStayChange = {},
            onCalculate = {},
            onSettingsClick = {},
            onCommute = {},
            onPolicyNoticeSeen = {},
        )
    }
}
