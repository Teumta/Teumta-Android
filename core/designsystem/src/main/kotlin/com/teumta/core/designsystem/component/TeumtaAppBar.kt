package com.teumta.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

// Figma: AppBar (Type=Title / Subtitle / Home). 뒤로 아이콘은 바탕 없이, 제목은 왼쪽 정렬
@Composable
fun TeumtaAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    overline: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = TeumtaDimens.SizeAppBar).padding(horizontal = TeumtaDimens.Space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(TeumtaIcons.Back, contentDescription = "뒤로", tint = colors.textPrimary) }
        } else {
            Spacer(Modifier.width(TeumtaDimens.Space16))
        }
        Column(Modifier.weight(1f)) {
            // Home 타입: 작은 머리글 + 굵은 제목(경로)
            overline?.let { Text(it, style = type.caption, color = colors.textTertiary) }
            Text(title, style = if (overline != null) type.titleL else type.title, color = colors.textPrimary, maxLines = 1)
            subtitle?.let { Text(it, style = type.caption, color = colors.textTertiary) }
        }
        actions()
    }
}

// 홈 앱바 예: TeumtaAppBar(
//     overline = "오늘", title = "가산디지털단지 → 동암",
//     actions = { IconButton(onClick = onSettings) { Icon(TeumtaIcons.Sliders, contentDescription = "설정") } },
// )
