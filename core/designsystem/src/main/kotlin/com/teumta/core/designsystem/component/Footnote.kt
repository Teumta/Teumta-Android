package com.teumta.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teumta.core.designsystem.theme.TeumtaTheme

// Figma: Footnote. 계산 근거 한 줄, 아이콘 없이
@Composable
fun Footnote(
    modifier: Modifier = Modifier,
    text: String = "평일 시간표 기준이에요. 실시간 지연은 반영하지 않아요.",
) {
    Text(text, modifier = modifier, style = TeumtaTheme.typography.caption, color = TeumtaTheme.colors.textTertiary)
}
