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
import com.teumta.core.designsystem.component.TeumtaHelpButton
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

@Composable
fun FavoriteStationsRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: FavoriteStationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    FavoriteStationsScreen(
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
fun FavoriteStationsScreen(
    state: FavoriteStationsUiState,
    onBack: () -> Unit,
    onPick: (FavoriteSlot) -> Unit,
    onCancelPick: () -> Unit,
    onStationPicked: (Station) -> Unit,
    onClear: (FavoriteSlot) -> Unit,
    onSave: () -> Unit,
) {
    val picking = state.picking
    // 역 목록을 펼친 동안 뒤로 가기는 목록만 닫는다(화면을 떠나면 고른 값이 사라진다)
    BackHandler(enabled = picking != null, onBack = onCancelPick)
    val title = when (picking) {
        FavoriteSlot.ORIGIN -> "출발역 고르기"
        FavoriteSlot.DESTINATION -> "도착역 고르기"
        null -> "자주 쓰는 두 역"
    }

    Scaffold(
        containerColor = TeumtaTheme.colors.bgSurface,
        topBar = {
            TeumtaAppBar(
                title = title,
                onBack = if (picking != null) onCancelPick else onBack,
                modifier = Modifier.statusBarsPadding(),
                actions = {
                    // 역 목록을 펼친 동안에는 안내 버튼을 숨긴다
                    if (picking == null) TeumtaHelpButton("자주 쓰는 두 역", favoriteHelp(state.origin?.name, state.destination?.name))
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                picking != null -> StationPicker(state.sections, onStationPicked)
                else -> FavoriteStationsForm(state, onPick, onClear, onSave)
            }
        }
    }
}

@Composable
private fun FavoriteStationsForm(
    state: FavoriteStationsUiState,
    onPick: (FavoriteSlot) -> Unit,
    onClear: (FavoriteSlot) -> Unit,
    onSave: () -> Unit,
) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    Column(Modifier.fillMaxSize()) {
        SlotRow("출발역", state.origin, enabled = !state.isSaving, onPick = { onPick(FavoriteSlot.ORIGIN) }, onClear = { onClear(FavoriteSlot.ORIGIN) })
        SlotRow("도착역", state.destination, enabled = !state.isSaving, onPick = { onPick(FavoriteSlot.DESTINATION) }, onClear = { onClear(FavoriteSlot.DESTINATION) })
        Column(
            modifier = Modifier.padding(TeumtaDimens.Space20),
            verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
        ) {
            ServerError(state.error)
            TeumtaButton(
                text = if (state.isSaving) "저장하는 중…" else "저장",
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SlotRow(label: String, station: Station?, enabled: Boolean, onPick: () -> Unit, onClear: () -> Unit) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    // 설정 화면 목록과 같은 모양(카드 없이 줄과 구분선)
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = TeumtaDimens.Space20, end = TeumtaDimens.Space8).heightIn(min = TeumtaDimens.SizeRow),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = TeumtaDimens.Space8)) {
                Text(label, style = type.label, color = colors.textTertiary)
                Text(station?.name ?: "정하지 않았어요", style = type.bodyStrong, color = if (station != null) colors.textPrimary else colors.textTertiary)
            }
            if (station != null) TextButton(onClick = onClear, enabled = enabled) { Text("지우기", color = colors.textSecondary) }
            TextButton(onClick = onPick, enabled = enabled) { Text(if (station != null) "바꾸기" else "고르기", color = colors.textAccent) }
        }
        HorizontalDivider(Modifier.padding(horizontal = TeumtaDimens.Space20), TeumtaDimens.StrokeHairline, colors.borderDefault)
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
private fun FavoriteStationsScreenPreview() {
    val stations = listOf("서울역", "남영", "용산").mapIndexed { i, name ->
        Station("S$i", name, i + 1, parentId = if (i == 0) null else "S${i - 1}")
    }
    TeumtaTheme {
        FavoriteStationsScreen(
            state = FavoriteStationsUiState(isLoading = false, sections = stationSections(StationTree(stations)), origin = stations[2]),
            onBack = {}, onPick = {}, onCancelPick = {}, onStationPicked = {}, onClear = {}, onSave = {},
        )
    }
}
