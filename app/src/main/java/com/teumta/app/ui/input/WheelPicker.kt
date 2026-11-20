package com.teumta.app.ui.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teumta.core.designsystem.theme.TeumtaTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.abs

// 바퀴에 보이는 줄 수. 가운데 줄이 고른 값이고 위아래로 두 줄씩 보인다
const val WHEEL_VISIBLE_ROWS = 5

// 바퀴 한 줄 높이. 원래 기획은 44dp지만 큰 글꼴에서 가운데 숫자(24sp)가 잘리지 않게 글꼴 크기를 따라 늘린다
@Composable
fun wheelRowHeight(): Dp {
    val textHeight = with(LocalDensity.current) { 24.sp.toDp() }
    return maxOf(44.dp, textHeight * 1.4f)
}

// 위아래로 굴려 고르는 숫자 바퀴(원래 기획 시간 시트). 칸 0..count-1 중 가운데에 멈춘 칸을 고른다.
// 굴리다 멈추면 가장 가까운 칸에 딱 맞춰 서고(snap), 칸을 눌러도 그 칸으로 굴러간다.
// 화면 읽기 프로그램은 칸마다 describe(i)를 읽고, 두 번 누르면 그 칸을 고른다
@Composable
fun NumberWheel(
    count: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    label: (Int) -> String,
    describe: (Int) -> String,
    modifier: Modifier = Modifier,
    width: Dp = 84.dp,
) {
    val colors = TeumtaTheme.colors
    val rowHeight = wheelRowHeight()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value.coerceIn(0, count - 1))
    val scope = rememberCoroutineScope()
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // 지금 가운데 줄에 가장 가까운 칸. 굴리는 동안에도 글자 크기·색을 바로 바꾸는 데 쓴다
    val centered by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: 0
        }
    }

    // 멈췄을 때만 고른 값을 알린다. 굴리는 중간 값까지 알리면 바깥에서 다시 굴리라고 해서 서로 밀고 당긴다
    LaunchedEffect(listState) {
        snapshotFlow { if (listState.isScrollInProgress) null else centered }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { currentOnValueChange(it) }
    }

    // 바깥에서 값이 바뀌면(빠른 칩) 그 칸으로 굴린다
    LaunchedEffect(value) {
        if (!listState.isScrollInProgress && centered != value) {
            listState.animateScrollToItem(value.coerceIn(0, count - 1))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.width(width).height(rowHeight * WHEEL_VISIBLE_ROWS),
        // 첫 칸과 끝 칸도 가운데 줄까지 올 수 있게 위아래로 두 줄씩 비운다
        contentPadding = PaddingValues(vertical = rowHeight * (WHEEL_VISIBLE_ROWS / 2)),
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center),
    ) {
        items(count) { i ->
            val distance = abs(i - centered)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clickable { scope.launch { listState.animateScrollToItem(i) } }
                    .semantics {
                        contentDescription = describe(i)
                        selected = i == centered
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(i),
                    fontSize = when (distance) {
                        0 -> 24.sp
                        1 -> 18.sp
                        else -> 16.sp
                    },
                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = TeumtaTheme.typography.body.fontFamily,
                    // 원래 기획은 먼 줄을 더 옅게 했지만 글자 대비(4.5:1)를 지키려고 옅은 회색 한 가지만 쓴다
                    color = if (distance == 0) colors.textPrimary else colors.textTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}
