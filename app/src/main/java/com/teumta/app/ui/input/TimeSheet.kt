package com.teumta.app.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teumta.app.domain.ServiceTime
import com.teumta.app.ui.formatDuration
import com.teumta.core.designsystem.component.TeumtaButton
import com.teumta.core.designsystem.component.TeumtaButtonStyle
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaShapes
import com.teumta.core.designsystem.theme.TeumtaTheme
import kotlinx.coroutines.launch
import java.time.LocalTime

// 원래 기획 시간 시트: 아래에서 올라와 제목·설명, 빠른 칩, 바퀴 두 개, 취소·확인 버튼.
// 도착 시각은 시 00~23·분 00~59(1분 단위), 머무를 시간은 시간 0~5·분 00~55(5분 단위)

// 도착 시각 고르기. 확인하면 고른 시각을 넘긴다
@Composable
fun ArriveBySheet(
    initial: LocalTime,
    toName: String?,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    // 화면을 돌려도 굴리던 값이 남게 rememberSaveable
    var hour by rememberSaveable { mutableIntStateOf(initial.hour) }
    var minute by rememberSaveable { mutableIntStateOf(initial.minute) }
    val picks = remember { arriveQuickPicks(ServiceTime.now().withZoneSameInstant(ServiceTime.SEOUL).toLocalTime()) }
    val chosen = LocalTime.of(hour, minute)
    val time = chosen.format(TIME)

    TimeSheetFrame(
        title = "도착 시각",
        subtitle = "이 시각까지 ${toName ?: "도착역"}에 도착하는 열차로 계산해요",
        confirmText = "${time}까지 도착",
        onConfirm = { onConfirm(chosen) },
        onDismiss = onDismiss,
        chips = {
            picks.forEach { pick ->
                TeumtaChip(
                    label = pick.format(TIME),
                    onClick = {
                        hour = pick.hour
                        minute = pick.minute
                    },
                    selected = pick == chosen,
                    height = SHEET_CHIP_HEIGHT,
                )
            }
        },
    ) {
        NumberWheel(
            count = 24,
            value = hour,
            onValueChange = { hour = it },
            label = { "%02d".format(it) },
            describe = { "${it}시" },
        )
        WheelUnit("시")
        NumberWheel(
            count = 60,
            value = minute,
            onValueChange = { minute = it },
            label = { "%02d".format(it) },
            describe = { "${it}분" },
        )
        WheelUnit("분")
    }
}

// 머무를 시간 고르기. 확인하면 고른 시간을 분으로 넘긴다
@Composable
fun StaySheet(
    initialMin: Int,
    viaName: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = initialMin.coerceIn(0, StayChoice.MAX_MIN)
    var hours by rememberSaveable { mutableIntStateOf(StayChoice.hoursOf(start)) }
    var minuteIndex by rememberSaveable { mutableIntStateOf(StayChoice.minuteIndexOf(start)) }
    val chosen = StayChoice.of(hours, minuteIndex)

    TimeSheetFrame(
        title = "머무를 시간",
        subtitle = "${viaName}에서 보낼 시간이에요",
        confirmText = "${formatDuration(chosen * 60)} 머무르기",
        onConfirm = { onConfirm(chosen) },
        onDismiss = onDismiss,
        chips = {
            StayChoice.SHEET_CHIPS.forEach { minutes ->
                TeumtaChip(
                    label = formatDuration(minutes * 60),
                    onClick = {
                        hours = StayChoice.hoursOf(minutes)
                        minuteIndex = StayChoice.minuteIndexOf(minutes)
                    },
                    selected = minutes == chosen,
                    height = SHEET_CHIP_HEIGHT,
                )
            }
        },
    ) {
        NumberWheel(
            count = StayChoice.HOUR_COUNT,
            value = hours,
            onValueChange = { hours = it },
            label = { "$it" },
            describe = { "${it}시간" },
        )
        WheelUnit("시간")
        NumberWheel(
            count = StayChoice.MINUTE_COUNT,
            value = minuteIndex,
            onValueChange = { minuteIndex = it },
            label = { "%02d".format(it * StayChoice.STEP_MIN) },
            describe = { "${it * StayChoice.STEP_MIN}분" },
        )
        WheelUnit("분")
    }
}

// 두 시트가 함께 쓰는 틀. wheels에는 바퀴와 단위 글자를 옆으로 늘어놓는다
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSheetFrame(
    title: String,
    subtitle: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    chips: @Composable () -> Unit,
    wheels: @Composable () -> Unit,
) {
    val colors = TeumtaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // 내려가는 움직임이 끝난 뒤에 닫는다. 바로 닫으면 시트가 뚝 사라진다
    val close: (() -> Unit) -> Unit = { after ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // 바퀴를 굴릴 때 시트가 같이 끌려 내려가지 않게 시트 끌기를 끈다. 바깥을 누르거나 뒤로 가기, 취소로 닫는다
        sheetGesturesEnabled = false,
        // 원래 기획: 위 모서리만 20dp로 둥글게
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.bgSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.borderDefault, width = 32.dp, height = 4.dp) },
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = TeumtaDimens.Space24),
            verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space16),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space4)) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = TeumtaTheme.typography.title,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(subtitle, style = TeumtaTheme.typography.label, fontWeight = FontWeight.Normal, color = colors.textSecondary)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
                verticalArrangement = Arrangement.spacedBy(ChipSpacing),
            ) { chips() }
            WheelBox(wheels)
            Row(horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space8)) {
                TeumtaButton(
                    text = "취소",
                    onClick = { close(onDismiss) },
                    style = TeumtaButtonStyle.Outlined,
                    modifier = Modifier.weight(1f),
                )
                TeumtaButton(
                    text = confirmText,
                    onClick = { close(onConfirm) },
                    modifier = Modifier.weight(2f),
                )
            }
        }
    }
}

// 가운데 줄 뒤에 회색 띠를 깔고 그 위에 바퀴들을 가운데 정렬로 놓는다
@Composable
private fun WheelBox(wheels: @Composable () -> Unit) {
    val rowHeight = wheelRowHeight()
    Box(Modifier.fillMaxWidth().height(rowHeight * WHEEL_VISIBLE_ROWS), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .background(TeumtaTheme.colors.bgField, TeumtaShapes.Md),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space4),
            verticalAlignment = Alignment.CenterVertically,
        ) { wheels() }
    }
}

// 바퀴 옆 단위 글자("시", "분", "시간"). 큰 글꼴에서 "시간"이 두 줄로 꺾이지 않게 폭은 최소만 정하고 한 줄로 둔다
@Composable
private fun WheelUnit(text: String) {
    Text(
        text = text,
        modifier = Modifier.widthIn(min = 36.dp),
        maxLines = 1,
        style = TeumtaTheme.typography.titleS,
        color = TeumtaTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
}

// 원래 기획: 시트 안 칩은 입력 화면 칩(40dp)보다 조금 작은 36dp
private val SHEET_CHIP_HEIGHT = 36.dp
