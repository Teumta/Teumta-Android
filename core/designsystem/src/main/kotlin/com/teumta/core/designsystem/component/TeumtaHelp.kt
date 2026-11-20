package com.teumta.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme
import kotlinx.coroutines.launch

// 화면에 줄글 설명을 늘어놓지 않고 물음표 버튼 뒤에 둔다. 누르면 아래에서 안내 시트가 올라오고,
// 안내는 한 줄씩 끊은 점 목록으로 보여 준다
@Composable
fun TeumtaHelpButton(title: String, points: List<String>, modifier: Modifier = Modifier) {
    // 시트를 열었는지는 이 버튼만 알면 된다. 화면을 돌려도 열린 채로 남게 rememberSaveable
    var open by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(TeumtaIcons.Help, contentDescription = "$title 안내", tint = TeumtaTheme.colors.textTertiary)
    }
    if (open) TeumtaHelpSheet(title = title, points = points, onDismiss = { open = false })
}

// 안내 시트: 제목, 점 목록, 확인 버튼
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeumtaHelpSheet(title: String, points: List<String>, onDismiss: () -> Unit) {
    val colors = TeumtaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.bgSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.borderDefault, width = 32.dp, height = 4.dp) },
    ) {
        Column(
            modifier = Modifier.padding(start = TeumtaDimens.Space20, end = TeumtaDimens.Space20, bottom = TeumtaDimens.Space24),
            verticalArrangement = Arrangement.spacedBy(TeumtaDimens.Space16),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = TeumtaTheme.typography.title,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            TeumtaBulletList(points)
            // 내려가는 움직임이 끝난 뒤에 닫는다. 바로 닫으면 시트가 뚝 사라진다
            TeumtaButton(
                text = "확인",
                onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// 점 목록. 한 줄에 한 가지만 쓰고, 길면 점 오른쪽에서 줄을 바꾼다(점 아래로 파고들지 않는다)
@Composable
fun TeumtaBulletList(points: List<String>, modifier: Modifier = Modifier, style: TextStyle = TeumtaTheme.typography.body) {
    val colors = TeumtaTheme.colors
    // 점은 첫 줄 가운데에 맞춘다. 점 크기와 줄 높이가 글꼴 크기를 따라 커지므로 sp에서 계산한다
    val density = LocalDensity.current
    val dotSize = with(density) { 5.sp.toDp() }
    val dotTop = with(density) { (style.lineHeight.toDp() - dotSize) / 2 }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        points.forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(top = dotTop).size(dotSize).background(colors.textTertiary, CircleShape))
                Text(point, modifier = Modifier.weight(1f), style = style, color = colors.textSecondary)
            }
        }
    }
}
