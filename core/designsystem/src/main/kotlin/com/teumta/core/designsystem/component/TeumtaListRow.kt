package com.teumta.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teumta.core.designsystem.icon.TeumtaIcons
import com.teumta.core.designsystem.theme.TeumtaDimens
import com.teumta.core.designsystem.theme.TeumtaTheme

// Figma: ListRow (설정 S-05). 왼쪽 이름(+한 줄 설명), 오른쪽 값과 화살표, 아래 구분선.
// onClick이 있으면 줄 전체를 누를 수 있고 화살표가 보인다. 마지막 줄은 divider = false로 선을 뺀다
@Composable
fun TeumtaListRow(
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
) {
    val colors = TeumtaTheme.colors
    val type = TeumtaTheme.typography
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .heightIn(min = TeumtaDimens.SizeRow)
                .padding(start = TeumtaDimens.Space20, end = if (onClick != null) TeumtaDimens.Space16 else TeumtaDimens.Space20, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TeumtaDimens.Space12),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = type.body, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                sub?.let { Text(it, style = type.caption, color = colors.textTertiary) }
            }
            // 값은 오른쪽 끝에 붙인다. 이름과 폭을 반씩 나눠, 길면(큰 글꼴의 긴 역 이름) 줄을 바꾸되 오른쪽 정렬을 지킨다
            value?.let {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text(text = it, style = type.body, color = colors.textSecondary, textAlign = TextAlign.End)
                }
            }
            if (onClick != null) {
                Icon(TeumtaIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(TeumtaDimens.SizeIcon20), tint = colors.textTertiary)
            }
        }
        if (divider) {
            HorizontalDivider(Modifier.padding(horizontal = TeumtaDimens.Space20), TeumtaDimens.StrokeHairline, colors.borderDefault)
        }
    }
}

// 목록 묶음 위 작은 이름(예: "앱 정보"). 화면 읽기 프로그램이 제목으로 건너뛸 수 있게 heading.
// help를 주면 오른쪽 끝에 물음표 버튼을 둔다(TeumtaHelpButton)
@Composable
fun TeumtaListLabel(text: String, modifier: Modifier = Modifier, help: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = TeumtaDimens.Space20, end = if (help != null) TeumtaDimens.Space4 else TeumtaDimens.Space20, top = if (help != null) TeumtaDimens.Space12 else TeumtaDimens.Space24, bottom = TeumtaDimens.Space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = TeumtaTheme.typography.label,
            color = TeumtaTheme.colors.textTertiary,
        )
        help?.invoke()
    }
}
