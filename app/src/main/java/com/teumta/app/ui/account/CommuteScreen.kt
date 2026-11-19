package com.teumta.app.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station
import com.teumta.app.ui.input.StationSection
import com.teumta.app.ui.input.stationSections
import com.teumta.core.designsystem.component.TeumtaAppBar
import com.teumta.core.designsystem.component.TeumtaButton
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

@Composable
fun CommuteRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: CommuteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    CommuteScreen(
        state = state,
        onBack = onBack,
        onPick = viewModel::onPick,
        onCancelPick = viewModel::onCancelPick,
        onStationPicked = viewModel::onStationPicked,
        onClear = viewModel::onClear,
        onSave = viewModel::onSave,
    )
}

@Composable
fun CommuteScreen(
    state: CommuteUiState,
    onBack: () -> Unit,
    onPick: (CommuteSlot) -> Unit,
    onCancelPick: () -> Unit,
    onStationPicked: (Station) -> Unit,
    onClear: (CommuteSlot) -> Unit,
    onSave: () -> Unit,
) {
    val picking = state.picking
    // 역 목록을 펼친 동안 뒤로 가기는 목록만 닫는다(화면을 떠나면 고른 값이 사라진다)
    BackHandler(enabled = picking != null, onBack = onCancelPick)
    val title = when (picking) {
        CommuteSlot.HOME -> "집역 고르기"
        CommuteSlot.WORK -> "회사역 고르기"
        null -> "집역·회사역"
    }

    Scaffold(
        topBar = {
            TeumtaAppBar(
                title = title,
                onBack = if (picking != null) onCancelPick else onBack,
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                picking != null -> StationPicker(state.sections, onStationPicked)
                else -> CommuteForm(state, onPick, onClear, onSave)
            }
        }
    }
}

@Composable
private fun CommuteForm(
    state: CommuteUiState,
    onPick: (CommuteSlot) -> Unit,
    onClear: (CommuteSlot) -> Unit,
    onSave: () -> Unit,
) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    Column(
        modifier = Modifier.fillMaxSize().padding(TeumtaDimens.Space20),
        verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
    ) {
        Text(
            "정해 두면 입력 화면에 출근길·퇴근길 버튼이 생겨요. 틈타 서버에 저장해서 다른 휴대폰에서도 로그인하면 그대로 쓸 수 있어요.",
            style = type.body,
            color = colors.textSecondary,
        )
        SlotRow("집역", state.home, enabled = !state.isSaving, onPick = { onPick(CommuteSlot.HOME) }, onClear = { onClear(CommuteSlot.HOME) })
        SlotRow("회사역", state.work, enabled = !state.isSaving, onPick = { onPick(CommuteSlot.WORK) }, onClear = { onClear(CommuteSlot.WORK) })
        ServerError(state.error)
        TeumtaButton(
            text = if (state.isSaving) "저장하는 중…" else "저장",
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SlotRow(label: String, station: Station?, enabled: Boolean, onPick: () -> Unit, onClear: () -> Unit) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = TeumtaDimens.Space16, end = TeumtaDimens.Space4).heightIn(min = TeumtaDimens.SizeRow),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = type.label, color = colors.textSecondary)
                Text(station?.name ?: "정하지 않았어요", style = type.bodyStrong, color = if (station != null) colors.textPrimary else colors.textTertiary)
            }
            if (station != null) TextButton(onClick = onClear, enabled = enabled) { Text("지우기") }
            TextButton(onClick = onPick, enabled = enabled) { Text(if (station != null) "바꾸기" else "고르기") }
        }
    }
}

// 102개 역을 노선도 토막 순서대로 보여 준다. 누르면 그 역을 고른다
@Composable
private fun StationPicker(sections: List<StationSection>, onStationPicked: (Station) -> Unit) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    LazyColumn(Modifier.fillMaxSize()) {
        sections.forEach { section ->
            item(key = "title-${section.stations.first().id}") {
                Text(
                    section.title,
                    style = type.label,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .padding(start = TeumtaDimens.Space20, top = TeumtaDimens.Space16, bottom = TeumtaDimens.Space4)
                        .semantics { heading() },
                )
            }
            items(section.stations, key = { it.id }) { station ->
                Text(
                    station.name,
                    style = type.body,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onStationPicked(station) }
                        .heightIn(min = TeumtaDimens.SizeTouchMin)
                        .padding(horizontal = TeumtaDimens.Space20, vertical = TeumtaDimens.Space12),
                )
                HorizontalDivider(Modifier.padding(horizontal = TeumtaDimens.Space20))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CommuteScreenPreview() {
    val stations = listOf("서울역", "남영", "용산").mapIndexed { i, name ->
        Station("S$i", name, i + 1, parentId = if (i == 0) null else "S${i - 1}")
    }
    TeumtaTheme {
        CommuteScreen(
            state = CommuteUiState(isLoading = false, sections = stationSections(StationTree(stations)), home = stations[2]),
            onBack = {}, onPick = {}, onCancelPick = {}, onStationPicked = {}, onClear = {}, onSave = {},
        )
    }
}
