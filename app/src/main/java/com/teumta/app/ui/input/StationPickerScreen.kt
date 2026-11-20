package com.teumta.app.ui.input

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teumta.app.R
import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaShapes
import com.teumta.core.designsystem.theme.TeumtaTheme
import java.time.LocalTime

// 원래 기획 S-02: 경로 상자의 한 칸에 넣을 역을 고르는 화면.
// 위에 검색창(이름이나 초성), 아래에 노선 레일 목록. 출발·도착은 노선 전체를, 들를 역은 두 역 사이만 보여 준다
@Composable
fun StationPickerScreen(
    role: StationRole,
    state: InputUiState,
    onPick: (Station) -> Unit,
    onSkipVia: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var query by rememberSaveable { mutableStateOf("") }
    val selection = state.selection

    Scaffold(containerColor = TeumtaTheme.colors.bgSurface) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(), // 키보드가 올라오면 목록이 그 위에서 끝나게
        ) {
            SearchBar(role, query, onQueryChange = { query = it }, onBack = onBack)
            if (query.isBlank()) {
                BrowseList(role, state, onPick, onSkipVia)
            } else {
                SearchResults(role, selection, state.sections, query, onPick)
            }
        }
    }
}

private fun slotName(role: StationRole): String = when (role) {
    StationRole.FROM -> "출발역"
    StationRole.VIA -> "들를 역"
    StationRole.TO -> "도착역"
}

@Composable
private fun SearchBar(role: StationRole, query: String, onQueryChange: (String) -> Unit, onBack: () -> Unit) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    val focusManager = LocalFocusManager.current
    val placeholder = "${slotName(role)} 이름이나 초성 (예: ㄱㄹ)"
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space4),
    ) {
        IconButton(onClick = onBack) { Icon(TeumtaIcons.Back, contentDescription = "뒤로", tint = colors.textPrimary) }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = TeumtaDimens.SizeSearch)
                .background(colors.bgField, TeumtaShapes.Md)
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8),
        ) {
            Icon(TeumtaIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.textTertiary)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).semantics { contentDescription = placeholder },
                singleLine = true,
                textStyle = type.body.copy(fontWeight = FontWeight.Medium, color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accentDefault),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(placeholder, style = type.body, color = colors.textTertiary, maxLines = 1)
                        field()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier.size(TeumtaDimens.SizeTouchMin - 8.dp).clickable(onClickLabel = "검색어 지우기") { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(24.dp).background(colors.textTertiary, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(TeumtaIcons.Close, contentDescription = "검색어 지우기", modifier = Modifier.size(12.dp), tint = colors.bgSurface)
                    }
                }
            }
        }
    }
}

// 목록 한 줄의 종류. LazyColumn이 key로 줄을 구분하고, 지금 고른 역까지 처음부터 내려가 있게 위치를 찾는다
private sealed interface PickerRow {
    val key: String

    data object SkipVia : PickerRow {
        override val key = "skip-via"
    }

    data object NoViaCandidate : PickerRow {
        override val key = "no-via"
    }

    data class Gap(override val key: String) : PickerRow

    data class Favorites(val stations: List<Station>) : PickerRow {
        override val key = "favorites"
    }

    data class Header(val title: String) : PickerRow {
        override val key = "header-$title"
    }

    data class StationLine(val station: Station, val isFirst: Boolean, val isLast: Boolean) : PickerRow {
        override val key = "station-${station.id}"
    }
}

private fun browseRows(role: StationRole, state: InputUiState): List<PickerRow> {
    val selection = state.selection
    val rows = mutableListOf<PickerRow>()
    if (role == StationRole.VIA) {
        rows += PickerRow.SkipVia
        rows += PickerRow.Gap("gap-skip")
        val path = selection.path.orEmpty()
        if (selection.viaCandidates.isEmpty()) {
            rows += PickerRow.NoViaCandidate
        } else {
            rows += PickerRow.Header("${selection.from?.name} → ${selection.to?.name} 사이")
            rows += path.mapIndexed { i, s -> PickerRow.StationLine(s, i == 0, i == path.lastIndex) }
        }
        return rows
    }
    // 출발·도착: 설정에 저장한 자주 쓰는 역을 먼저, 그다음 노선 전체
    val favorites = listOfNotNull(state.favoriteOrigin, state.favoriteDestination)
        .filter { selection.canPick(role, it) }
        .distinct()
    if (favorites.isNotEmpty()) {
        rows += PickerRow.Favorites(favorites)
        rows += PickerRow.Gap("gap-favorites")
    }
    state.sections.forEach { section ->
        rows += PickerRow.Header(section.title)
        rows += section.stations.mapIndexed { i, s -> PickerRow.StationLine(s, i == 0, i == section.stations.lastIndex) }
    }
    return rows
}

@Composable
private fun BrowseList(role: StationRole, state: InputUiState, onPick: (Station) -> Unit, onSkipVia: () -> Unit) {
    val selection = state.selection
    val rows = remember(role, state.sections, selection, state.favoriteOrigin, state.favoriteDestination) { browseRows(role, state) }
    // 출발·도착은 지금 칸에 들어 있는 역 근처로 내려가 있는다(인천처럼 아래쪽 역도 바로 보인다).
    // 들를 역(맨 위 "바로 가기")과 자주 쓰는 역이 있을 때는 맨 위 줄이 가려지지 않게 내려가지 않는다
    val mine = selection.stationOf(role)
    val keepTop = role == StationRole.VIA || rows.any { it is PickerRow.Favorites }
    val start = if (keepTop) 0 else rows.indexOfFirst { it is PickerRow.StationLine && it.station == mine }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (start - 3).coerceAtLeast(0))

    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                PickerRow.SkipVia -> SkipViaRow(selection, onSkipVia)
                PickerRow.NoViaCandidate -> Message("두 역이 바로 이웃해서 사이에 들를 역이 없어요")
                is PickerRow.Gap -> SectionGap()
                is PickerRow.Favorites -> FavoriteChips(row.stations, onPick)
                is PickerRow.Header -> GroupHeader(row.title)
                is PickerRow.StationLine -> StationLineRow(row, role, selection, onPick)
            }
        }
        item(key = "bottom") { Spacer(Modifier.height(TeumtaDimens.Space24)) }
    }
}

@Composable
private fun SkipViaRow(selection: RouteSelection, onSkipVia: () -> Unit) {
    val colors = TeumtaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onSkipVia)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
    ) {
        Box(Modifier.size(36.dp).background(colors.bgAccentSubtle, CircleShape), contentAlignment = Alignment.Center) {
            Icon(ImageVector.vectorResource(R.drawable.ic_arrow_right), contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.textAccent)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("들르지 않고 바로 가기", style = TeumtaTheme.typography.titleS, color = colors.textPrimary)
            Text(
                "${selection.from?.name} → ${selection.to?.name} 가장 늦은 열차만 알려 줘요",
                style = TeumtaTheme.typography.caption,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun FavoriteChips(stations: List<Station>, onPick: (Station) -> Unit) {
    Column(Modifier.padding(top = 16.dp, bottom = 16.dp)) {
        GroupHeader("자주 쓰는 역", top = 0.dp)
        FlowRow(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
            verticalArrangement = Arrangement.spacedBy(ChipSpacing),
        ) {
            stations.forEach { station ->
                TeumtaChip(label = station.name, height = 36.dp, onClick = { onPick(station) })
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String, top: Dp = 20.dp) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = top, bottom = 6.dp)
            .semantics { heading() },
        style = TeumtaTheme.typography.label,
        fontWeight = FontWeight.SemiBold,
        color = TeumtaTheme.colors.textTertiary,
    )
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp),
        style = TeumtaTheme.typography.bodyS,
        color = TeumtaTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
}

// 노선 한 줄: 왼쪽 레일과 점, 역 이름, 오른쪽에 이미 고른 칸(출발·도착·들를 역).
// 고를 수 없는 역(다른 칸의 끝 역)은 파란 점에 회색 글자로 두고 누를 수 없다
@Composable
private fun StationLineRow(row: PickerRow.StationLine, role: StationRole, selection: RouteSelection, onPick: (Station) -> Unit) {
    val colors = TeumtaTheme.colors
    val station = row.station
    val enabled = selection.canPick(role, station)
    val mine = selection.stationOf(role) == station
    val roleLabel = selection.roleOf(station)?.let(::roleLabel)
    val dot = when {
        mine -> StationDot.ACCENT
        enabled -> StationDot.RING
        else -> StationDot.FILLED
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TeumtaDimens.SizeRow)
            .drawBehind { drawRailAndDot(colors, x = 30.dp, dot = dot, railAbove = !row.isFirst, railBelow = !row.isLast) }
            .clickable(enabled = enabled, onClickLabel = "${slotName(role)}으로 고르기") { onPick(station) }
            .padding(start = 60.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = station.name,
            modifier = Modifier.weight(1f),
            style = TeumtaTheme.typography.titleS,
            color = if (enabled) colors.textPrimary else colors.textTertiary,
        )
        if (roleLabel != null) {
            Text(
                text = roleLabel,
                style = TeumtaTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colors.textAccent else colors.textTertiary,
            )
        }
    }
}

private fun roleLabel(role: StationRole): String = when (role) {
    StationRole.FROM -> "출발"
    StationRole.VIA -> "들를 역"
    StationRole.TO -> "도착"
}

// 검색 결과: 맞는 글자만 주황으로, 아래에 어느 구간 역인지
@Composable
private fun SearchResults(
    role: StationRole,
    selection: RouteSelection,
    sections: List<StationSection>,
    query: String,
    onPick: (Station) -> Unit,
) {
    val colors = TeumtaTheme.colors
    val candidates = if (role == StationRole.VIA) selection.viaCandidates else selection.tree.stations
    val results = remember(role, selection, query) {
        searchStations(candidates, query).filter { selection.canPick(role, it.station) }
    }
    val sectionOf = remember(sections) {
        sections.flatMap { section -> section.stations.map { it.id to section.title } }.toMap()
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "count") {
            Text(
                text = "검색 결과 ${results.size}개",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                style = TeumtaTheme.typography.label,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
            )
        }
        if (results.isEmpty()) {
            item(key = "empty") {
                Message(if (role == StationRole.VIA) "두 역 사이에 그런 역이 없어요" else "1호선에 그런 역이 없어요")
            }
        }
        items(results, key = { it.station.id }) { match ->
            val name = match.station.name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable(onClickLabel = "${slotName(role)}으로 고르기") { onPick(match.station) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(name.substring(0, match.start))
                        withStyle(SpanStyle(color = colors.textAccent)) { append(name.substring(match.start, match.start + match.length)) }
                        append(name.substring(match.start + match.length))
                    },
                    style = TeumtaTheme.typography.titleS,
                    color = colors.textPrimary,
                )
                sectionOf[match.station.id]?.let {
                    Text("$it 구간", style = TeumtaTheme.typography.caption, color = colors.textTertiary)
                }
            }
            HorizontalDivider(thickness = TeumtaDimens.StrokeHairline, color = colors.borderDefault)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StationPickerPreview() {
    val stations = listOf("서울역", "남영", "용산", "노량진", "대방").mapIndexed { i, name ->
        Station("S$i", name, i + 1, parentId = if (i == 0) null else "S${i - 1}")
    }
    val tree = StationTree(stations)
    TeumtaTheme {
        StationPickerScreen(
            role = StationRole.VIA,
            state = InputUiState(
                isLoading = false,
                sections = stationSections(tree),
                selection = RouteSelection(tree, from = stations[0], to = stations[4], via = stations[2]),
                arriveBy = LocalTime.of(21, 0),
            ),
            onPick = {},
            onSkipVia = {},
            onBack = {},
        )
    }
}
